package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.TenantContext;
import com.garment.dto.AddEmployeeDTO;
import com.garment.dto.BatchAddEmployeeDTO;
import com.garment.entity.Company;
import com.garment.entity.User;
import com.garment.entity.UserCompany;
import com.garment.mapper.CompanyMapper;
import com.garment.mapper.UserCompanyMapper;
import com.garment.mapper.UserMapper;
import com.garment.util.PasswordGenerator;
import com.garment.vo.BatchAddResultVO;
import com.garment.vo.CheckPhoneVO;
import com.garment.vo.EmployeeVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EmployeeService {

    private final UserMapper userMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final CompanyMapper companyMapper;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(UserMapper userMapper, UserCompanyMapper userCompanyMapper,
                           CompanyMapper companyMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.companyMapper = companyMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 添加员工
     *
     * @return 新账号的明文临时密码；跨厂添加（existingUserId）时返回 null
     */
    @Transactional
    public String addEmployee(AddEmployeeDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 禁止添加老板角色
        if ("boss".equals(dto.getRole())) {
            throw new RuntimeException("不能通过员工管理添加老板");
        }

        LocalDate hireDate = dto.getHireDate() != null ? dto.getHireDate() : LocalDate.now();

        if (dto.getExistingUserId() != null) {
            // 流程2：手机号已注册，只创建关联
            User existUser = userMapper.selectById(dto.getExistingUserId());
            if (existUser == null) {
                throw new RuntimeException("用户不存在");
            }
            // 检查是否已关联本公司
            Long count = userCompanyMapper.selectCount(
                    new LambdaQueryWrapper<UserCompany>()
                            .eq(UserCompany::getUserId, dto.getExistingUserId())
                            .eq(UserCompany::getCompanyId, companyId)
                            .eq(UserCompany::getDeleted, 0)
            );
            if (count > 0) {
                throw new RuntimeException("该员工已在公司中");
            }
            UserCompany uc = new UserCompany();
            uc.setUserId(dto.getExistingUserId());
            uc.setCompanyId(companyId);
            uc.setRole(dto.getRole());
            uc.setHireDate(hireDate);
            uc.setStatus(1);
            // 主管自动分配扎号前缀
            if ("supervisor".equals(dto.getRole())) {
                uc.setBundlePrefix(allocateBundlePrefix(companyId));
            }
            userCompanyMapper.insert(uc);
            return null; // 跨厂添加，不创建新账号，无临时密码
        } else {
            // 流程1：手机号未注册，创建用户+关联
            String phone = dto.getPhone();
            if (phone == null || phone.isEmpty()) {
                throw new RuntimeException("手机号不能为空");
            }
            // 检查用户名（手机号）是否已存在
            Long exist = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, phone)
            );
            if (exist > 0) {
                throw new RuntimeException("该手机号已被注册");
            }

            User user = new User();
            user.setUsername(phone);
            // 支持自定义密码，为空则自动生成随机密码
            String tempPassword = (dto.getPassword() != null && !dto.getPassword().isEmpty())
                    ? dto.getPassword()
                    : PasswordGenerator.generate();
            user.setPassword(passwordEncoder.encode(tempPassword));
            user.setRealName(dto.getRealName());
            user.setPhone(phone);
            user.setGender(dto.getGender());
            user.setIdCard(dto.getIdCard());
            user.setBankCard(dto.getBankCard());
            user.setType(1);
            user.setStatus(1);
            user.setNeedChangePassword(1);
            userMapper.insert(user);

            UserCompany uc = new UserCompany();
            uc.setUserId(user.getId());
            uc.setCompanyId(companyId);
            uc.setRole(dto.getRole());
            uc.setHireDate(hireDate);
            uc.setStatus(1);
            // 主管自动分配扎号前缀
            if ("supervisor".equals(dto.getRole())) {
                uc.setBundlePrefix(allocateBundlePrefix(companyId));
            }
            userCompanyMapper.insert(uc);
            return tempPassword;
        }
    }

    /**
     * 批量添加员工（超级管理员专用）
     * 使用统一密码，支持已注册用户的跨厂添加
     */
    @Transactional
    public BatchAddResultVO batchAddEmployees(BatchAddEmployeeDTO dto) {
        Long companyId = dto.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请选择目标公司");
        }

        // 验证公司是否存在
        Company company = companyMapper.selectById(companyId);
        if (company == null || company.getDeleted() == 1) {
            throw new RuntimeException("目标公司不存在");
        }

        String password = dto.getPassword();
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("请设置初始密码");
        }

        String defaultRole = dto.getRole() != null ? dto.getRole() : "worker";
        if ("boss".equals(defaultRole)) {
            throw new RuntimeException("不能通过批量添加创建老板角色");
        }

        String encodedPassword = passwordEncoder.encode(password);
        List<BatchAddResultVO.FailItem> failures = new ArrayList<>();
        int successCount = 0;

        for (BatchAddEmployeeDTO.EmployeeItem item : dto.getEmployees()) {
            try {
                String phone = item.getPhone();
                String realName = item.getRealName();

                if (phone == null || phone.isEmpty()) {
                    failures.add(new BatchAddResultVO.FailItem(phone, realName, "手机号不能为空"));
                    continue;
                }
                if (realName == null || realName.isEmpty()) {
                    failures.add(new BatchAddResultVO.FailItem(phone, realName, "姓名不能为空"));
                    continue;
                }

                LocalDate hireDate = item.getHireDate() != null ? item.getHireDate() : LocalDate.now();
                Integer gender = item.getGender();

                // 检查用户是否已注册
                User existUser = userMapper.selectOne(
                        new LambdaQueryWrapper<User>().eq(User::getUsername, phone)
                );

                if (existUser != null) {
                    // 已注册：检查是否已关联本公司
                    Long count = userCompanyMapper.selectCount(
                            new LambdaQueryWrapper<UserCompany>()
                                    .eq(UserCompany::getUserId, existUser.getId())
                                    .eq(UserCompany::getCompanyId, companyId)
                                    .eq(UserCompany::getDeleted, 0)
                    );
                    if (count > 0) {
                        failures.add(new BatchAddResultVO.FailItem(phone, realName, "该员工已在公司中"));
                        continue;
                    }
                    // 创建关联
                    UserCompany uc = new UserCompany();
                    uc.setUserId(existUser.getId());
                    uc.setCompanyId(companyId);
                    uc.setRole(defaultRole);
                    uc.setHireDate(hireDate);
                    uc.setStatus(1);
                    if ("supervisor".equals(defaultRole)) {
                        uc.setBundlePrefix(allocateBundlePrefix(companyId));
                    }
                    userCompanyMapper.insert(uc);
                } else {
                    // 未注册：创建用户+关联
                    User user = new User();
                    user.setUsername(phone);
                    user.setPassword(encodedPassword);
                    user.setRealName(realName);
                    user.setPhone(phone);
                    user.setGender(gender);
                    user.setType(1);
                    user.setStatus(1);
                    user.setNeedChangePassword(1);
                    userMapper.insert(user);

                    UserCompany uc = new UserCompany();
                    uc.setUserId(user.getId());
                    uc.setCompanyId(companyId);
                    uc.setRole(defaultRole);
                    uc.setHireDate(hireDate);
                    uc.setStatus(1);
                    if ("supervisor".equals(defaultRole)) {
                        uc.setBundlePrefix(allocateBundlePrefix(companyId));
                    }
                    userCompanyMapper.insert(uc);
                }

                successCount++;
            } catch (Exception e) {
                failures.add(new BatchAddResultVO.FailItem(
                        item.getPhone(), item.getRealName(), e.getMessage()
                ));
            }
        }

        BatchAddResultVO result = new BatchAddResultVO();
        result.setSuccessCount(successCount);
        result.setFailures(failures);
        return result;
    }

    /**
     * 员工列表（分页+搜索，支持按 status 筛选）
     */
    public Page<EmployeeVO> listEmployees(int pageNum, int pageSize, String keyword, Integer status) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        LambdaQueryWrapper<UserCompany> ucWrapper = new LambdaQueryWrapper<>();
        ucWrapper.eq(UserCompany::getCompanyId, companyId)
                .eq(UserCompany::getDeleted, 0)
                .ne(UserCompany::getRole, "boss");

        if (status != null) {
            ucWrapper.eq(UserCompany::getStatus, status);
        }

        List<Long> matchedUserIds = null;
        if (keyword != null && !keyword.isEmpty()) {
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.like(User::getRealName, keyword)
                    .or()
                    .like(User::getPhone, keyword);
            List<User> matchedUsers = userMapper.selectList(userWrapper);
            if (matchedUsers.isEmpty()) {
                Page<EmployeeVO> emptyPage = new Page<>();
                emptyPage.setCurrent(pageNum);
                emptyPage.setSize(pageSize);
                emptyPage.setTotal(0);
                emptyPage.setPages(0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            matchedUserIds = matchedUsers.stream().map(User::getId).toList();
            ucWrapper.in(UserCompany::getUserId, matchedUserIds);
        }

        ucWrapper.orderByDesc(UserCompany::getCreateTime);
        Page<UserCompany> ucPage = userCompanyMapper.selectPage(new Page<>(pageNum, pageSize), ucWrapper);

        // 批量查询用户信息
        List<Long> userIds = ucPage.getRecords().stream().map(UserCompany::getUserId).toList();
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getId, userIds)
            );
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        Page<EmployeeVO> result = new Page<>();
        result.setCurrent(ucPage.getCurrent());
        result.setSize(ucPage.getSize());
        result.setTotal(ucPage.getTotal());
        result.setPages(ucPage.getPages());

        List<EmployeeVO> records = new ArrayList<>();
        for (UserCompany uc : ucPage.getRecords()) {
            EmployeeVO vo = new EmployeeVO();
            vo.setId(uc.getId());
            vo.setUserId(uc.getUserId());
            User user = userMap.get(uc.getUserId());
            if (user != null) {
                vo.setRealName(user.getRealName());
                vo.setPhone(user.getPhone());
                vo.setGender(user.getGender());
                vo.setIdCard(user.getIdCard());
                vo.setBankCard(user.getBankCard());
            }
            vo.setRole(uc.getRole());
            vo.setBundlePrefix(uc.getBundlePrefix());
            vo.setHireDate(uc.getHireDate());
            vo.setStatus(uc.getStatus());
            vo.setCreateTime(uc.getCreateTime());
            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    /**
     * 编辑员工信息
     */
    @Transactional
    public void updateEmployee(Long userCompanyId, AddEmployeeDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        UserCompany uc = userCompanyMapper.selectById(userCompanyId);
        if (uc == null || !uc.getCompanyId().equals(companyId) || uc.getDeleted() == 1) {
            throw new RuntimeException("员工不存在");
        }

        // 禁止修改老板角色
        if ("boss".equals(dto.getRole())) {
            throw new RuntimeException("不能将员工设为老板");
        }

        // 更新 user 表
        User user = userMapper.selectById(uc.getUserId());
        if (user != null) {
            if (dto.getRealName() != null) user.setRealName(dto.getRealName());
            if (dto.getGender() != null) user.setGender(dto.getGender());
            if (dto.getIdCard() != null) user.setIdCard(dto.getIdCard());
            if (dto.getBankCard() != null) user.setBankCard(dto.getBankCard());
            userMapper.updateById(user);
        }

        // 更新 user_company 表
        if (dto.getRole() != null) uc.setRole(dto.getRole());
        if (dto.getHireDate() != null) uc.setHireDate(dto.getHireDate());
        userCompanyMapper.updateById(uc);
    }

    /**
     * 员工离职（status 设为 0，不改 deleted）
     */
    public void deleteEmployee(Long userCompanyId) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        UserCompany uc = userCompanyMapper.selectById(userCompanyId);
        if (uc == null || !uc.getCompanyId().equals(companyId) || uc.getDeleted() == 1) {
            throw new RuntimeException("员工不存在");
        }

        uc.setStatus(0);
        userCompanyMapper.updateById(uc);
    }

    /**
     * 恢复离职员工（status 设为 1）
     */
    public void restoreEmployee(Long userCompanyId) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        UserCompany uc = userCompanyMapper.selectById(userCompanyId);
        if (uc == null || !uc.getCompanyId().equals(companyId) || uc.getDeleted() == 1) {
            throw new RuntimeException("员工不存在");
        }

        uc.setStatus(1);
        userCompanyMapper.updateById(uc);
    }

    /**
     * 重置员工密码为随机密码
     *
     * @return 新的明文临时密码
     */
    @Transactional
    public String resetEmployeePassword(Long userCompanyId) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        UserCompany uc = userCompanyMapper.selectById(userCompanyId);
        if (uc == null || !uc.getCompanyId().equals(companyId) || uc.getDeleted() == 1) {
            throw new RuntimeException("员工不存在");
        }

        User user = userMapper.selectById(uc.getUserId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String tempPassword = PasswordGenerator.generate();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setNeedChangePassword(1);
        userMapper.updateById(user);
        return tempPassword;
    }

    /**
     * 为该公司分配下一个扎号字母前缀（A/B/C...Z/AA/AB...）
     */
    private String allocateBundlePrefix(Long companyId) {
        // 查询该公司所有已分配的前缀
        List<UserCompany> existing = userCompanyMapper.selectList(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getDeleted, 0)
                        .isNotNull(UserCompany::getBundlePrefix)
                        .select(UserCompany::getBundlePrefix)
        );
        Set<String> usedPrefixes = new HashSet<>();
        for (UserCompany uc : existing) {
            usedPrefixes.add(uc.getBundlePrefix());
        }

        // A 保留给 boss，主管从 B 开始
        usedPrefixes.add("A");
        int n = 1;
        while (true) {
            String prefix = numberToLetters(n);
            if (!usedPrefixes.contains(prefix)) {
                return prefix;
            }
            n++;
            if (n > 702) { // 最多支持 A-Z(26) + AA-AZ(26) + BA-BZ(26)... 足够了
                throw new RuntimeException("主管数量超出限制，无法分配扎号前缀");
            }
        }
    }

    /**
     * 数字转字母序列（1=A, 2=B, 26=Z, 27=AA, 28=AB...）
     */
    private String numberToLetters(int n) {
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + n % 26));
            n /= 26;
        }
        return sb.toString();
    }

    /**
     * 检查手机号是否已注册
     */
    public CheckPhoneVO checkPhone(String phone) {
        CheckPhoneVO vo = new CheckPhoneVO();
        if (phone == null || phone.isEmpty()) {
            vo.setExists(false);
            return vo;
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, phone)
        );
        if (user == null) {
            vo.setExists(false);
            return vo;
        }

        vo.setExists(true);
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());

        // 查询该用户已关联的公司
        List<UserCompany> relations = userCompanyMapper.selectList(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getUserId, user.getId())
                        .eq(UserCompany::getDeleted, 0)
        );
        if (!relations.isEmpty()) {
            List<Long> companyIds = relations.stream().map(UserCompany::getCompanyId).toList();
            List<Company> companies = companyMapper.selectList(
                    new LambdaQueryWrapper<Company>().in(Company::getId, companyIds)
            );
            List<String> companyNames = companies.stream().map(Company::getName).toList();
            vo.setExistingCompanies(companyNames);
        } else {
            vo.setExistingCompanies(List.of());
        }

        return vo;
    }
}

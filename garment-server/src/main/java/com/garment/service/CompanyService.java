package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.garment.common.TenantContext;
import com.garment.entity.*;
import com.garment.mapper.*;
import com.garment.util.PasswordGenerator;
import com.garment.vo.CompanyInfoVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class CompanyService extends ServiceImpl<CompanyMapper, Company> {

    private final UserMapper userMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final CompanyRenewLogMapper renewLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final StyleMapper styleMapper;
    private final BedMapper bedMapper;
    private final WorkRecordMapper workRecordMapper;

    public CompanyService(UserMapper userMapper, UserCompanyMapper userCompanyMapper,
                          CompanyRenewLogMapper renewLogMapper, PasswordEncoder passwordEncoder,
                          StyleMapper styleMapper, BedMapper bedMapper,
                          WorkRecordMapper workRecordMapper) {
        this.userMapper = userMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.renewLogMapper = renewLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.styleMapper = styleMapper;
        this.bedMapper = bedMapper;
        this.workRecordMapper = workRecordMapper;
    }

    /**
     * 分页查询公司列表（含老板信息，支持多字段搜索）
     */
    public Page<Map<String, Object>> listByPage(int pageNum, int pageSize,
                                                  String nameKeyword, String phoneKeyword,
                                                  String bossNameKeyword, Integer status) {
        // 如果按老板名字搜索，先获取匹配的公司ID列表
        List<Long> bossCompanyIds = null;
        if (bossNameKeyword != null && !bossNameKeyword.isEmpty()) {
            List<User> matchedUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>().like(User::getRealName, bossNameKeyword)
            );
            if (matchedUsers.isEmpty()) {
                Page<Map<String, Object>> emptyPage = new Page<>();
                emptyPage.setCurrent(pageNum);
                emptyPage.setSize(pageSize);
                emptyPage.setTotal(0);
                emptyPage.setPages(0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            List<Long> userIds = matchedUsers.stream().map(User::getId).toList();
            List<UserCompany> relations = userCompanyMapper.selectList(
                    new LambdaQueryWrapper<UserCompany>()
                            .in(UserCompany::getUserId, userIds)
                            .eq(UserCompany::getRole, "boss")
                            .eq(UserCompany::getStatus, 1)
            );
            if (relations.isEmpty()) {
                Page<Map<String, Object>> emptyPage = new Page<>();
                emptyPage.setCurrent(pageNum);
                emptyPage.setSize(pageSize);
                emptyPage.setTotal(0);
                emptyPage.setPages(0);
                emptyPage.setRecords(List.of());
                return emptyPage;
            }
            bossCompanyIds = relations.stream().map(UserCompany::getCompanyId).toList();
        }

        LambdaQueryWrapper<Company> wrapper = new LambdaQueryWrapper<>();
        if (nameKeyword != null && !nameKeyword.isEmpty()) {
            wrapper.like(Company::getName, nameKeyword);
        }
        if (phoneKeyword != null && !phoneKeyword.isEmpty()) {
            wrapper.like(Company::getContactPhone, phoneKeyword);
        }
        if (bossCompanyIds != null) {
            wrapper.in(Company::getId, bossCompanyIds);
        }
        if (status != null) {
            wrapper.eq(Company::getStatus, status);
        }
        wrapper.orderByDesc(Company::getCreateTime);
        Page<Company> companyPage = page(new Page<>(pageNum, pageSize), wrapper);

        // 收集所有公司ID，批量查询老板信息
        List<Long> companyIds = companyPage.getRecords().stream().map(Company::getId).toList();
        Map<Long, User> bossMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            List<UserCompany> relations = userCompanyMapper.selectList(
                    new LambdaQueryWrapper<UserCompany>()
                            .in(UserCompany::getCompanyId, companyIds)
                            .eq(UserCompany::getRole, "boss")
                            .eq(UserCompany::getStatus, 1)
            );
            List<Long> userIds = relations.stream().map(UserCompany::getUserId).toList();
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.selectList(
                        new LambdaQueryWrapper<User>()
                                .in(User::getId, userIds)
                );
                Map<Long, User> userMap = new HashMap<>();
                for (User u : users) {
                    userMap.put(u.getId(), u);
                }
                for (UserCompany uc : relations) {
                    bossMap.put(uc.getCompanyId(), userMap.get(uc.getUserId()));
                }
            }
        }

        // 组装结果
        Page<Map<String, Object>> result = new Page<>();
        result.setCurrent(companyPage.getCurrent());
        result.setSize(companyPage.getSize());
        result.setTotal(companyPage.getTotal());
        result.setPages(companyPage.getPages());

        List<Map<String, Object>> records = new ArrayList<>();
        for (Company c : companyPage.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("contactPerson", c.getContactPerson());
            map.put("contactPhone", c.getContactPhone());
            map.put("address", c.getAddress());
            map.put("status", c.getStatus());
            map.put("createTime", c.getCreateTime());
            map.put("expireTime", c.getExpireTime());
            map.put("renewTime", c.getRenewTime());
            User boss = bossMap.get(c.getId());
            if (boss != null) {
                map.put("bossUsername", boss.getUsername());
                map.put("bossRealName", boss.getRealName());
            }
            records.add(map);
        }
        result.setRecords(records);
        return result;
    }

    /**
     * 新增公司，同时自动生成老板账号
     *
     * @return 老板账号的明文临时密码（需展示给创建人，通知老板首次登录后修改）
     */
    @Transactional
    public String createCompany(Company company, int months) {
        company.setStatus(1);
        // 默认租期：根据传入的月数计算
        if (company.getExpireTime() == null) {
            company.setExpireTime(LocalDateTime.now().plusMonths(months));
        }
        save(company);

        // 记录初始租期
        CompanyRenewLog log = new CompanyRenewLog();
        log.setCompanyId(company.getId());
        log.setNewExpireTime(company.getExpireTime());
        log.setMonths(months);
        log.setRemark("初始租期");
        log.setOperatorId(TenantContext.getUserId());
        renewLogMapper.insert(log);

        // 用联系电话作为用户名
        String username = company.getContactPhone();
        if (username == null || username.isEmpty()) {
            throw new RuntimeException("联系电话不能为空，将作为老板登录账号");
        }

        // 检查用户名是否已存在
        Long exist = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (exist > 0) {
            throw new RuntimeException("该手机号已被其他账号使用: " + username);
        }

        // 创建老板账号
        String tempPassword = PasswordGenerator.generate();
        User boss = new User();
        boss.setUsername(username);
        boss.setPassword(passwordEncoder.encode(tempPassword));
        boss.setRealName(company.getContactPerson());
        boss.setPhone(company.getContactPhone());
        boss.setType(1); // 普通用户
        boss.setStatus(1);
        boss.setNeedChangePassword(1);
        userMapper.insert(boss);

        // 关联老板到公司
        UserCompany uc = new UserCompany();
        uc.setUserId(boss.getId());
        uc.setCompanyId(company.getId());
        uc.setRole("boss");
        uc.setHireDate(LocalDate.now());
        uc.setStatus(1);
        userCompanyMapper.insert(uc);

        return tempPassword;
    }

    /**
     * 更新公司信息
     */
    public void updateCompany(Company company) {
        updateById(company);
    }

    /**
     * 启用/禁用公司
     */
    public void toggleStatus(Long id, int status) {
        Company company = getById(id);
        if (company == null) {
            throw new RuntimeException("公司不存在");
        }
        company.setStatus(status);
        updateById(company);
    }

    /**
     * 续费：延长到期时间
     * @param months 续费月数
     * @param operatorId 操作人ID
     */
    @Transactional
    public void renewCompany(Long id, int months, Long operatorId) {
        Company company = getById(id);
        if (company == null) {
            throw new RuntimeException("公司不存在");
        }
        // 基于现有到期时间续费，若已过期则从当前时间开始
        LocalDateTime prevExpire = company.getExpireTime();
        LocalDateTime base = prevExpire;
        if (base == null || base.isBefore(LocalDateTime.now())) {
            base = LocalDateTime.now();
        }
        LocalDateTime newExpire = base.plusMonths(months);
        company.setExpireTime(newExpire);
        company.setRenewTime(LocalDateTime.now());
        updateById(company);

        // 记录续费历史
        CompanyRenewLog log = new CompanyRenewLog();
        log.setCompanyId(id);
        log.setPrevExpireTime(prevExpire);
        log.setNewExpireTime(newExpire);
        log.setMonths(months);
        log.setRemark("续费");
        log.setOperatorId(operatorId);
        renewLogMapper.insert(log);
    }

    /**
     * 查询公司续费历史
     */
    public List<CompanyRenewLog> getRenewLogs(Long companyId) {
        LambdaQueryWrapper<CompanyRenewLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompanyRenewLog::getCompanyId, companyId);
        wrapper.orderByDesc(CompanyRenewLog::getCreateTime);
        return renewLogMapper.selectList(wrapper);
    }

    /**
     * 分页查询所有续费记录（含公司名称）
     */
    public Page<Map<String, Object>> listAllRenewLogs(int pageNum, int pageSize, String keyword) {
        Page<CompanyRenewLog> pageParam = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CompanyRenewLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(CompanyRenewLog::getCreateTime);
        Page<CompanyRenewLog> logPage = renewLogMapper.selectPage(pageParam, wrapper);

        // 批量查询公司名称
        List<Long> companyIds = logPage.getRecords().stream()
                .map(CompanyRenewLog::getCompanyId)
                .distinct()
                .toList();
        Map<Long, String> companyNameMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            List<Company> companies = baseMapper.selectList(
                    new LambdaQueryWrapper<Company>()
                            .in(Company::getId, companyIds)
            );
            for (Company c : companies) {
                companyNameMap.put(c.getId(), c.getName());
            }
        }

        Page<Map<String, Object>> result = new Page<>();
        result.setCurrent(logPage.getCurrent());
        result.setSize(logPage.getSize());
        result.setTotal(logPage.getTotal());
        result.setPages(logPage.getPages());

        List<Map<String, Object>> records = new ArrayList<>();
        for (CompanyRenewLog log : logPage.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("companyId", log.getCompanyId());
            map.put("companyName", companyNameMap.getOrDefault(log.getCompanyId(), "未知公司"));
            map.put("prevExpireTime", log.getPrevExpireTime());
            map.put("newExpireTime", log.getNewExpireTime());
            map.put("months", log.getMonths());
            map.put("remark", log.getRemark());
            map.put("createTime", log.getCreateTime());
            records.add(map);
        }
        result.setRecords(records);
        return result;
    }

    /**
     * 重置老板密码为随机密码
     *
     * @return 新的明文临时密码（需展示给操作人，通知老板首次登录后修改）
     */
    @Transactional
    public String resetBossPassword(Long companyId) {
        UserCompany uc = userCompanyMapper.selectOne(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getRole, "boss")
                        .eq(UserCompany::getStatus, 1)
        );
        if (uc == null) {
            throw new RuntimeException("该公司没有关联的老板账号");
        }
        User boss = userMapper.selectById(uc.getUserId());
        if (boss == null) {
            throw new RuntimeException("老板账号不存在");
        }
        String tempPassword = PasswordGenerator.generate();
        boss.setPassword(passwordEncoder.encode(tempPassword));
        boss.setNeedChangePassword(1);
        userMapper.updateById(boss);
        return tempPassword;
    }

    /**
     * 获取公司信息（基本资料 + 统计数据）
     */
    public CompanyInfoVO getCompanyInfo(Long companyId) {
        // 1. 获取公司基本资料
        Company company = getById(companyId);
        if (company == null) {
            throw new RuntimeException("公司不存在");
        }

        CompanyInfoVO vo = new CompanyInfoVO();
        vo.setCompanyId(company.getId());
        vo.setCompanyName(company.getName());
        vo.setContactPerson(company.getContactPerson());
        vo.setContactPhone(company.getContactPhone());
        vo.setAddress(company.getAddress());
        vo.setStatus(company.getStatus());
        vo.setExpireTime(company.getExpireTime());
        vo.setCreateTime(company.getCreateTime());

        // 2. 统计员工数量
        Long workerCount = userCompanyMapper.selectCount(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getRole, "worker")
                        .eq(UserCompany::getStatus, 1)
        );
        vo.setWorkerCount(workerCount.intValue());

        // 3. 统计主管数量
        Long supervisorCount = userCompanyMapper.selectCount(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getRole, "supervisor")
                        .eq(UserCompany::getStatus, 1)
        );
        vo.setSupervisorCount(supervisorCount.intValue());

        // 4. 统计款式数量
        Long styleCount = styleMapper.selectCount(
                new LambdaQueryWrapper<Style>()
                        .eq(Style::getCompanyId, companyId)
                        .eq(Style::getArchived, 0)
        );
        vo.setStyleCount(styleCount.intValue());

        // 5. 统计床次总数
        Long bedCount = bedMapper.selectCount(
                new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getCompanyId, companyId)
        );
        vo.setBedCount(bedCount.intValue());

        // 6. 统计本月产量和工资
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // 本月产量总和
        List<WorkRecord> monthRecords = workRecordMapper.selectList(
                new LambdaQueryWrapper<WorkRecord>()
                        .eq(WorkRecord::getCompanyId, companyId)
                        .ge(WorkRecord::getCreateTime, monthStart)
                        .le(WorkRecord::getCreateTime, monthEnd)
        );

        int monthQuantity = 0;
        BigDecimal monthAmount = BigDecimal.ZERO;
        for (WorkRecord record : monthRecords) {
            if (record.getQuantity() != null) {
                monthQuantity += record.getQuantity();
            }
            if (record.getAmount() != null) {
                monthAmount = monthAmount.add(record.getAmount());
            }
        }
        vo.setMonthQuantity(monthQuantity);
        vo.setMonthAmount(monthAmount);

        return vo;
    }
}

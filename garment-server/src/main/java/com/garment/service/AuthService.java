package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.config.JwtUtil;
import com.garment.entity.Company;
import com.garment.entity.User;
import com.garment.entity.UserCompany;
import com.garment.mapper.CompanyMapper;
import com.garment.mapper.UserCompanyMapper;
import com.garment.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final CompanyMapper companyMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserMapper userMapper, UserCompanyMapper userCompanyMapper,
                       CompanyMapper companyMapper, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.companyMapper = companyMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(String username, String rawPassword) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 普通用户登录时，检查是否有关联的正常公司（未被禁用的）
        if (user.getType() != null && user.getType() != 0) {
            List<UserCompany> relations = userCompanyMapper.selectList(
                    new LambdaQueryWrapper<UserCompany>()
                            .eq(UserCompany::getUserId, user.getId())
                            .eq(UserCompany::getStatus, 1)
            );
            if (relations.isEmpty()) {
                throw new RuntimeException("您未关联任何公司，请联系管理员");
            }
            List<Long> companyIds = relations.stream().map(UserCompany::getCompanyId).toList();
            List<Company> companies = companyMapper.selectList(
                    new LambdaQueryWrapper<Company>()
                            .in(Company::getId, companyIds)
                            .eq(Company::getStatus, 1)
            );
            if (companies.isEmpty()) {
                throw new RuntimeException("您关联的公司均已被禁用，请联系管理员");
            }
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getType());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("userType", user.getType());
        result.put("needChangePassword", user.getNeedChangePassword() != null && user.getNeedChangePassword() == 1);
        return result;
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new RuntimeException("请输入当前密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码至少6位");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setNeedChangePassword(0);
        userMapper.updateById(user);
    }

    /**
     * 查询用户关联的公司列表（过滤掉被禁用的公司）
     */
    public List<Map<String, Object>> getUserCompanies(Long userId) {
        List<UserCompany> relations = userCompanyMapper.selectList(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getUserId, userId)
                        .eq(UserCompany::getStatus, 1)
        );

        if (relations.isEmpty()) {
            return List.of();
        }

        List<Long> companyIds = relations.stream().map(UserCompany::getCompanyId).toList();
        List<Company> companies = companyMapper.selectList(
                new LambdaQueryWrapper<Company>()
                        .in(Company::getId, companyIds)
                        .eq(Company::getStatus, 1)
        );
        Map<Long, Company> companyMap = new HashMap<>();
        for (Company c : companies) {
            companyMap.put(c.getId(), c);
        }

        return relations.stream()
                .filter(uc -> companyMap.containsKey(uc.getCompanyId()))
                .map(uc -> {
                    Map<String, Object> map = new HashMap<>();
                    Company company = companyMap.get(uc.getCompanyId());
                    map.put("companyId", uc.getCompanyId());
                    map.put("role", uc.getRole());
                    map.put("companyName", company.getName());
                    return map;
                }).toList();
    }
}

package com.garment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.entity.Company;
import com.garment.entity.UserCompany;
import com.garment.mapper.CompanyMapper;
import com.garment.mapper.UserCompanyMapper;
import com.garment.security.RequestAuthorizationPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final UserCompanyMapper userCompanyMapper;

    public SecurityConfig(JwtUtil jwtUtil, ObjectMapper objectMapper, CompanyMapper companyMapper, UserCompanyMapper userCompanyMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.companyMapper = companyMapper;
        this.userCompanyMapper = userCompanyMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(companyMapper, userCompanyMapper), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWT 认证过滤器
     */
    class JwtAuthFilter extends OncePerRequestFilter {

        private final CompanyMapper companyMapper;
        private final UserCompanyMapper userCompanyMapper;

        JwtAuthFilter(CompanyMapper companyMapper, UserCompanyMapper userCompanyMapper) {
            this.companyMapper = companyMapper;
            this.userCompanyMapper = userCompanyMapper;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserId(token);
                    String username = jwtUtil.getUsername(token);
                    Integer userType = jwtUtil.getUserType(token);

                    TenantContext.setUserId(userId);
                    TenantContext.setIsSuperAdmin(userType != null && userType == 0);
                    TenantContext.setUserType(userType);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    username, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            // 从请求头获取当前公司ID
            String companyIdStr = request.getHeader("X-Company-Id");
            Long companyId = null;
            if (companyIdStr != null && !companyIdStr.isEmpty()) {
                try {
                    companyId = Long.parseLong(companyIdStr);
                    TenantContext.setCompanyId(companyId);
                } catch (NumberFormatException ignored) {}
            }

            // 登录/认证相关接口跳过公司检查，避免公司被禁用时无法登录
            String requestPath = request.getRequestURI();
            if (requestPath.startsWith("/api/auth/")) {
                filterChain.doFilter(request, response);
                return;
            }

            // 非超级管理员携带 companyId 时，检查公司状态和用户归属
            if (companyId != null && !TenantContext.isSuperAdmin()) {
                // 验证当前用户是否属于该公司
                Long currentUserId = TenantContext.getUserId();
                if (currentUserId != null) {
                    UserCompany membership = userCompanyMapper.selectOne(
                            new LambdaQueryWrapper<UserCompany>()
                                    .eq(UserCompany::getUserId, currentUserId)
                                    .eq(UserCompany::getCompanyId, companyId)
                                    .eq(UserCompany::getStatus, 1)
                                    .last("LIMIT 1")
                    );
                    if (membership == null) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        Result<String> result = Result.error(403, "无权访问该公司数据");
                        response.getWriter().write(objectMapper.writeValueAsString(result));
                        TenantContext.clear();
                        return;
                    }
                    TenantContext.setCompanyRole(membership.getRole());
                }

                Company company = companyMapper.selectById(companyId);
                if (company != null) {
                    // 检查公司是否被禁用
                    if (company.getStatus() != null && company.getStatus() == 0) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        Result<String> result = Result.error(403, "公司已被禁用，请联系管理员");
                        response.getWriter().write(objectMapper.writeValueAsString(result));
                        TenantContext.clear();
                        return;
                    }
                    // 检查公司是否已过期
                    if (company.getExpireTime() != null
                            && company.getExpireTime().isBefore(LocalDateTime.now())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        Result<String> result = Result.error(403, "公司租期已到期，请联系管理员续费");
                        response.getWriter().write(objectMapper.writeValueAsString(result));
                        TenantContext.clear();
                        return;
                    }
                }
            }

            if (companyId != null && TenantContext.isSuperAdmin()
                    && companyMapper.selectById(companyId) == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        Result.error(403, "公司不存在")));
                TenantContext.clear();
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() != null
                    && !RequestAuthorizationPolicy.isAllowed(
                    TenantContext.getCompanyRole(),
                    TenantContext.isSuperAdmin(),
                    TenantContext.isPenetrating(),
                    request.getMethod(),
                    request.getRequestURI())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        Result.error(403, "无权执行此操作")));
                TenantContext.clear();
                return;
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        }
    }
}

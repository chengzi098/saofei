package com.garment.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器 - 从请求头中获取当前公司ID，设置到 ThreadLocal
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String HEADER_COMPANY_ID = "X-Company-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String companyIdStr = request.getHeader(HEADER_COMPANY_ID);
        if (companyIdStr != null && !companyIdStr.isEmpty()) {
            try {
                TenantContext.setCompanyId(Long.parseLong(companyIdStr));
            } catch (NumberFormatException e) {
                // 忽略无效的 company_id
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}

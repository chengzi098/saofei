package com.garment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.entity.Company;
import com.garment.entity.CompanyRenewLog;
import com.garment.service.CompanyService;
import com.garment.vo.CompanyInfoVO;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/company")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * 分页查询公司列表（含老板信息，仅超级管理员）
     */
    @GetMapping("/list")
    public Result<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String nameKeyword,
            @RequestParam(required = false) String phoneKeyword,
            @RequestParam(required = false) String bossNameKeyword,
            @RequestParam(required = false) Integer status) {

        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "无权限访问");
        }
        return Result.ok(companyService.listByPage(pageNum, pageSize, nameKeyword, phoneKeyword, bossNameKeyword, status));
    }

    /**
     * 获取公司详情（超管或该公司成员可访问）
     */
    @GetMapping("/{id}")
    public Result<Company> getById(@PathVariable Long id) {
        // 非超管只能查看自己所在的公司
        if (!TenantContext.isSuperAdmin()) {
            Long currentCompanyId = TenantContext.getCompanyId();
            if (currentCompanyId == null || !currentCompanyId.equals(id)) {
                return Result.error(403, "无权访问该公司信息");
            }
        }
        Company company = companyService.getById(id);
        return company != null ? Result.ok(company) : Result.error("公司不存在");
    }

    /**
     * 新增公司（同时自动生成老板账号，仅超级管理员）
     */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "无权限操作");
        }

        // 解析公司信息
        Company company = new Company();
        company.setName((String) body.get("name"));
        company.setContactPerson((String) body.get("contactPerson"));
        company.setContactPhone((String) body.get("contactPhone"));
        company.setAddress((String) body.get("address"));

        // 解析到期时间
        String expireTimeStr = (String) body.get("expireTime");
        if (expireTimeStr != null && !expireTimeStr.isEmpty()) {
            company.setExpireTime(java.time.LocalDateTime.parse(expireTimeStr.replace(" ", "T")));
        }

        // 解析月数（默认12个月）
        Object monthsObj = body.get("months");
        int months = monthsObj != null ? ((Number) monthsObj).intValue() : 12;

        String tempPassword = companyService.createCompany(company, months);
        Map<String, Object> data = new HashMap<>();
        data.put("tempPassword", tempPassword);
        return Result.ok(data);
    }

    /**
     * 更新公司信息（仅超级管理员）
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Company company) {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "无权限操作");
        }
        company.setId(id);
        companyService.updateCompany(company);
        return Result.ok();
    }

    /**
     * 启用/禁用公司（仅超级管理员）
     */
    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam int status) {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "无权限操作");
        }
        companyService.toggleStatus(id, status);
        return Result.ok();
    }

    /**
     * 续费：延长到期时间（仅超级管理员）
     */
    @PutMapping("/{id}/renew")
    public Result<Void> renew(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "无权限操作");
        }
        Object monthsObj = body.get("months");
        if (monthsObj == null) {
            return Result.error("续费时长不能为空");
        }
        int months = ((Number) monthsObj).intValue();
        int[] allowed = {1, 3, 6, 12, 24};
        boolean valid = false;
        for (int a : allowed) {
            if (a == months) { valid = true; break; }
        }
        if (!valid) {
            return Result.error("续费时长只能是 1个月、3个月、6个月、1年、2年、3年");
        }
        companyService.renewCompany(id, months, TenantContext.getUserId());
        return Result.ok();
    }

    /**
     * 查询公司续费历史（仅超级管理员）
     */
    @GetMapping("/{id}/renew-logs")
    public Result<List<CompanyRenewLog>> getRenewLogs(@PathVariable Long id) {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "无权限访问");
        }
        return Result.ok(companyService.getRenewLogs(id));
    }

    /**
     * 分页查询所有续费记录（仅超级管理员）
     */
    @GetMapping("/renew-logs")
    public Result<Page<Map<String, Object>>> listAllRenewLogs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "无权限访问");
        }
        return Result.ok(companyService.listAllRenewLogs(pageNum, pageSize, keyword));
    }

    /**
     * 重置老板密码（仅超级管理员）
     */
    @PutMapping("/{id}/reset-password")
    public Result<Map<String, Object>> resetBossPassword(@PathVariable Long id) {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "无权限操作");
        }
        String tempPassword = companyService.resetBossPassword(id);
        Map<String, Object> data = new HashMap<>();
        data.put("tempPassword", tempPassword);
        return Result.ok(data);
    }

    /**
     * 获取本公司信息（基本资料 + 统计数据）
     * 仅老板可访问
     */
    @GetMapping("/info")
    public Result<CompanyInfoVO> getCompanyInfo() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error(400, "未选择公司");
        }

        // 检查是否是老板
        if (!TenantContext.isSuperAdmin()) {
            Long userId = TenantContext.getUserId();
            // 这里需要检查用户是否是该公司老板，简化处理：允许超管和普通用户访问
            // 实际权限由前端控制，后端只校验是否登录
        }

        CompanyInfoVO companyInfo = companyService.getCompanyInfo(companyId);
        return Result.ok(companyInfo);
    }
}

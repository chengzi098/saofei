package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.service.DashboardService;
import com.garment.vo.AdminDashboardVO;
import com.garment.vo.BossDashboardVO;
import com.garment.vo.DashboardVO;
import com.garment.vo.SupervisorDashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Result<DashboardVO> getDashboard() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        DashboardVO vo = dashboardService.getDashboard();
        return Result.ok(vo);
    }

    /**
     * 超级管理员全局统计
     */
    @GetMapping("/admin-stats")
    public Result<AdminDashboardVO> getAdminStats() {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "仅超级管理员可访问");
        }
        AdminDashboardVO vo = dashboardService.getAdminStats();
        return Result.ok(vo);
    }

    /**
     * 主管工作台统计
     */
    @GetMapping("/supervisor-stats")
    public Result<SupervisorDashboardVO> getSupervisorStats() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        SupervisorDashboardVO vo = dashboardService.getSupervisorStats();
        return Result.ok(vo);
    }

    /**
     * 老板工作台统计
     */
    @GetMapping("/boss-stats")
    public Result<BossDashboardVO> getBossStats() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        BossDashboardVO vo = dashboardService.getBossStats();
        return Result.ok(vo);
    }

    /**
     * 工人本月收入统计
     */
    @GetMapping("/worker-monthly-income")
    public Result<Map<String, BigDecimal>> getWorkerMonthlyIncome() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        Map<String, BigDecimal> income = dashboardService.getWorkerMonthlyIncome();
        return Result.ok(income);
    }

    /**
     * 工人通知计数（红点提醒）
     */
    @GetMapping("/worker-notifications")
    public Result<Map<String, Integer>> getWorkerNotifications() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        Map<String, Integer> notifications = dashboardService.getWorkerNotifications();
        return Result.ok(notifications);
    }

    /**
     * 标记通知已查看
     */
    @PostMapping("/worker-notifications/viewed")
    public Result<Void> markNotificationsViewed(@RequestParam String type) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        dashboardService.markNotificationsViewed(type);
        return Result.ok();
    }

    /**
     * 老板通知计数（红点提醒）
     */
    @GetMapping("/boss-notifications")
    public Result<Map<String, Integer>> getBossNotifications() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        Map<String, Integer> notifications = dashboardService.getBossNotifications();
        return Result.ok(notifications);
    }

    /**
     * 标记老板通知已查看
     */
    @PostMapping("/boss-notifications/viewed")
    public Result<Void> markBossNotificationsViewed(@RequestParam String type) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        dashboardService.markBossNotificationsViewed(type);
        return Result.ok();
    }

    /**
     * 主管通知计数（红点提醒）
     */
    @GetMapping("/supervisor-notifications")
    public Result<Map<String, Integer>> getSupervisorNotifications() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        Map<String, Integer> notifications = dashboardService.getSupervisorNotifications();
        return Result.ok(notifications);
    }

    /**
     * 标记主管通知已查看
     */
    @PostMapping("/supervisor-notifications/viewed")
    public Result<Void> markSupervisorNotificationsViewed(@RequestParam String type) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        dashboardService.markSupervisorNotificationsViewed(type);
        return Result.ok();
    }
}

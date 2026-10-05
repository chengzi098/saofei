package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.service.AnalysisService;
import com.garment.vo.AnalysisVO.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据分析控制器（用于AI分析）
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 获取员工绩效分析（Top N）
     */
    @GetMapping("/employee-performance")
    public Result<List<EmployeePerformance>> getEmployeePerformance(
            @RequestParam(defaultValue = "10") int topN) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error(400, "未选择公司");
        }
        return Result.ok(analysisService.getEmployeePerformance(companyId, topN));
    }

    /**
     * 获取产量趋势分析（最近N天）
     */
    @GetMapping("/production-trend")
    public Result<List<ProductionTrend>> getProductionTrend(
            @RequestParam(defaultValue = "30") int days) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error(400, "未选择公司");
        }
        return Result.ok(analysisService.getProductionTrend(companyId, days));
    }

    /**
     * 获取款式进度分析
     */
    @GetMapping("/style-progress")
    public Result<List<StyleProgress>> getStyleProgress() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error(400, "未选择公司");
        }
        return Result.ok(analysisService.getStyleProgress(companyId));
    }

    /**
     * 获取工序效率分析
     */
    @GetMapping("/process-efficiency")
    public Result<List<ProcessEfficiency>> getProcessEfficiency() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error(400, "未选择公司");
        }
        return Result.ok(analysisService.getProcessEfficiency(companyId));
    }

    /**
     * 获取工资分布分析
     */
    @GetMapping("/wage-distribution")
    public Result<List<WageDistribution>> getWageDistribution() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error(400, "未选择公司");
        }
        return Result.ok(analysisService.getWageDistribution(companyId));
    }

    /**
     * 获取综合分析报告（一次性获取所有数据）
     */
    @GetMapping("/report")
    public Result<AnalysisReport> getAnalysisReport() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error(400, "未选择公司");
        }
        return Result.ok(analysisService.getAnalysisReport(companyId));
    }

    /**
     * 获取数据摘要（适合快速分析）
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error(400, "未选择公司");
        }
        return Result.ok(analysisService.getSummary(companyId));
    }
}

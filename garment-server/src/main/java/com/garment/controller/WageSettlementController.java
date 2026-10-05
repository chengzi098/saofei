package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.CreateSettlementDTO;
import com.garment.service.WageSettlementService;
import com.garment.vo.WageSettlementVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wage-settlement")
public class WageSettlementController {

    private final WageSettlementService wageSettlementService;

    public WageSettlementController(WageSettlementService wageSettlementService) {
        this.wageSettlementService = wageSettlementService;
    }

    /** 获取上次结算信息 */
    @GetMapping("/last")
    public Result<Map<String, Object>> getLastSettlement() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能查看");
        try { return Result.ok(wageSettlementService.getLastSettlement()); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 预览全员明细（不保存） */
    @PostMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestBody CreateSettlementDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能查看");
        try { return Result.ok(wageSettlementService.previewSettlement(dto.getStartDate(), dto.getEndDate())); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 批量确认员工（生成结算记录） */
    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@RequestBody Map<String, Object> body) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能操作");
        try {
            String startDate = (String) body.get("startDate");
            String endDate = (String) body.get("endDate");
            @SuppressWarnings("unchecked")
            List<Long> employeeIds = ((List<Number>) body.get("employeeIds")).stream()
                    .map(Number::longValue)
                    .toList();
            return Result.ok(wageSettlementService.confirmEmployees(startDate, endDate, employeeIds));
        }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 结算单列表（老板） */
    @GetMapping
    public Result<List<WageSettlementVO>> list() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能查看");
        return Result.ok(wageSettlementService.listSettlements());
    }

    /** 结算单详情（老板） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能查看");
        try { return Result.ok(wageSettlementService.getSettlementDetail(id)); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 某个工人的报工明细（老板查看，用于确认前审核） */
    @GetMapping("/{id}/worker/{workerId}")
    public Result<Map<String, Object>> workerDetail(@PathVariable Long id, @PathVariable Long workerId) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能查看");
        try { return Result.ok(wageSettlementService.getWorkerDetail(id, workerId)); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 设置员工可见性 */
    @PostMapping("/{id}/worker/{workerId}/visibility")
    public Result<Void> setVisibility(@PathVariable Long id, @PathVariable Long workerId,
                                       @RequestBody Map<String, Integer> body) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能操作");
        try { wageSettlementService.setVisibility(id, workerId, body.get("visible")); return Result.ok(); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 员工端：我的工资结算 */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> mySettlements() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能查看");
        try { return Result.ok(wageSettlementService.getMySettlements()); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 员工端：确认查看工资 */
    @PostMapping("/{id}/confirm-view")
    public Result<Void> confirmView(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能操作");
        try { wageSettlementService.confirmViewing(id); return Result.ok(); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 工资统计（按月查看报工工资汇总，不需要创建结算单） */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam String yearMonth) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能查看");
        try { return Result.ok(wageSettlementService.getWageStatistics(yearMonth)); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }

    /** 打印工资条数据（所有工人按工序汇总） */
    @GetMapping("/{id}/print-data")
    public Result<Map<String, Object>> printData(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) return Result.error(403, "超级管理员不能查看");
        try { return Result.ok(wageSettlementService.getPrintData(id)); }
        catch (RuntimeException e) { return Result.error(400, e.getMessage()); }
    }
}

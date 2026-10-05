package com.garment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.EmployeeWorkSummaryDto;
import com.garment.dto.WorkerSpeedDto;
import com.garment.dto.ReturnWorkRecordDTO;
import com.garment.dto.SubmitWorkRecordDTO;
import com.garment.service.EmployeeWorkDetailService;
import com.garment.service.WorkRecordService;
import com.garment.vo.WorkRecordVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/work-record")
public class WorkRecordController {

    private final WorkRecordService workRecordService;
    private final EmployeeWorkDetailService employeeWorkDetailService;

    public WorkRecordController(WorkRecordService workRecordService,
                                EmployeeWorkDetailService employeeWorkDetailService) {
        this.workRecordService = workRecordService;
        this.employeeWorkDetailService = employeeWorkDetailService;
    }

    /**
     * 提交报工（工人扫码后）—— 免审核直接生效
     */
    @PostMapping
    public Result<Void> submit(@RequestBody SubmitWorkRecordDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能报工");
        }
        workRecordService.submitWorkRecord(dto);
        return Result.ok();
    }

    /**
     * 报工列表（分页，支持按状态、工人、款号、床号、扎号、工序、日期范围筛选）
     */
    @GetMapping
    public Result<Page<WorkRecordVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long workerId,
            @RequestParam(required = false) String workerName,
            @RequestParam(required = false) String styleNo,
            @RequestParam(required = false) String bedNo,
            @RequestParam(required = false) String bundleNo,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能查看报工");
        }
        return Result.ok(workRecordService.listWorkRecords(pageNum, pageSize, status, workerId, workerName, styleNo, bedNo, bundleNo, processName, startDate, endDate));
    }

    /**
     * 报工筛选候选项：款号/床号/扎号/工序（按层级联动，去重，id 倒序），用于下拉框
     */
    @GetMapping("/filter-options")
    public Result<Map<String, List<String>>> filterOptions(
            @RequestParam(required = false) String styleNo,
            @RequestParam(required = false) String bedNo,
            @RequestParam(required = false) String bundleNo) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error("请先选择公司");
        }
        return Result.ok(workRecordService.getFilterOptions(companyId, styleNo, bedNo, bundleNo));
    }

    /**
     * 员工工作汇总（用于老板端汇总卡片：总件数、总金额、工作天数、记录条数）
     * 只统计 status=1 已通过；不传 workerId 时返回每个员工一行（前端可聚合为全员合计）
     */
    @GetMapping("/summary")
    public Result<List<EmployeeWorkSummaryDto>> summary(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long workerId) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能查看报工");
        }
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error("请先选择公司");
        }
        // 工人角色（type=2）只能查看自己的汇总
        Integer userType = TenantContext.getUserType();
        if (userType != null && userType == 2) {
            workerId = TenantContext.getUserId();
        }
        return Result.ok(employeeWorkDetailService.getEmployeeWorkSummary(companyId, startDate, endDate, workerId));
    }

    /**
     * 员工每扎作业耗时（相邻报工时间差法，用于作业速度柱状图）
     * 口径：同一员工·同床号·同工序的相邻两次报工时间差 = 后一扎作业耗时；首扎不计
     */
    @GetMapping("/worker-speed")
    public Result<List<WorkerSpeedDto>> workerSpeed(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Long workerId) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能查看报工");
        }
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error("请先选择公司");
        }
        return Result.ok(employeeWorkDetailService.getWorkerSpeed(companyId, workerId, startDate, endDate));
    }

    /**
     * 退回报工（主管扫码后退回已完成的工序）
     */
    @PutMapping("/bundle-process/{bundleProcessId}/return")
    public Result<Void> returnWorkRecord(@PathVariable Long bundleProcessId, @RequestBody ReturnWorkRecordDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能退回");
        }
        workRecordService.returnWorkRecord(bundleProcessId, dto);
        return Result.ok();
    }
}

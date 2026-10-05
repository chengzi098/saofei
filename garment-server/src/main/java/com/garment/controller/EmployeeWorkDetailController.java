package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.EmployeeWorkDetailDto;
import com.garment.service.EmployeeWorkDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 员工工作详情控制器
 */
@RestController
@RequestMapping("/api/analysis")
public class EmployeeWorkDetailController {

    @Autowired
    private EmployeeWorkDetailService employeeWorkDetailService;

    /**
     * 获取员工工作详情列表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param workerId  员工ID（可选）
     * @return 工作详情列表
     */
    @GetMapping("/employee-work-details")
    public Result<List<EmployeeWorkDetailDto>> getEmployeeWorkDetails(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long workerId) {

        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Result.error("请先选择公司");
        }

        // 如果是工人角色（type=2），只能查看自己的数据
        Integer userType = TenantContext.getUserType();
        if (userType != null && userType == 2) {
            workerId = TenantContext.getUserId();
        }

        List<EmployeeWorkDetailDto> details = employeeWorkDetailService.getEmployeeWorkDetails(
            companyId, startDate, endDate, workerId
        );

        return Result.ok(details);
    }
}

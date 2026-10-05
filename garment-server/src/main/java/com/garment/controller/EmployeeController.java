package com.garment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.AddEmployeeDTO;
import com.garment.dto.BatchAddEmployeeDTO;
import com.garment.service.EmployeeService;
import com.garment.vo.BatchAddResultVO;
import com.garment.vo.CheckPhoneVO;
import com.garment.vo.EmployeeVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * 添加员工
     */
    @PostMapping
    public Result<String> add(@RequestBody AddEmployeeDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作员工");
        }
        String tempPassword = employeeService.addEmployee(dto);
        return Result.ok(tempPassword);
    }

    /**
     * 批量添加员工（超级管理员专用）
     */
    @PostMapping("/batch")
    public Result<BatchAddResultVO> batchAdd(@RequestBody BatchAddEmployeeDTO dto) {
        if (!TenantContext.isSuperAdmin()) {
            return Result.error(403, "仅超级管理员可批量添加员工");
        }
        return Result.ok(employeeService.batchAddEmployees(dto));
    }

    /**
     * 员工列表（分页+搜索）
     */
    @GetMapping
    public Result<Page<EmployeeVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能查看员工列表");
        }
        return Result.ok(employeeService.listEmployees(pageNum, pageSize, keyword, status));
    }

    /**
     * 编辑员工
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AddEmployeeDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作员工");
        }
        employeeService.updateEmployee(id, dto);
        return Result.ok();
    }

    /**
     * 员工离职（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作员工");
        }
        employeeService.deleteEmployee(id);
        return Result.ok();
    }

    /**
     * 重置员工密码为随机密码
     */
    @PutMapping("/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作员工");
        }
        String tempPassword = employeeService.resetEmployeePassword(id);
        return Result.ok(tempPassword);
    }

    /**
     * 恢复离职员工
     */
    @PutMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作员工");
        }
        employeeService.restoreEmployee(id);
        return Result.ok();
    }

    /**
     * 检查手机号是否已注册
     */
    @GetMapping("/check-phone")
    public Result<CheckPhoneVO> checkPhone(@RequestParam String phone) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作员工");
        }
        return Result.ok(employeeService.checkPhone(phone));
    }
}

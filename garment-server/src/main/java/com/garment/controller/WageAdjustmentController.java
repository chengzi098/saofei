package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.SaveAdjustmentDTO;
import com.garment.service.WageAdjustmentService;
import com.garment.vo.WageAdjustmentVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wage-adjustment")
public class WageAdjustmentController {

    private final WageAdjustmentService wageAdjustmentService;

    public WageAdjustmentController(WageAdjustmentService wageAdjustmentService) {
        this.wageAdjustmentService = wageAdjustmentService;
    }

    /**
     * 添加调整记录（老板/主管）
     */
    @PostMapping
    public Result<WageAdjustmentVO> create(@RequestBody SaveAdjustmentDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作");
        }
        try {
            WageAdjustmentVO vo = wageAdjustmentService.create(dto);
            return Result.ok(vo);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 调整列表（老板/主管）
     */
    @GetMapping
    public Result<List<WageAdjustmentVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long workerId) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作");
        }
        try {
            List<WageAdjustmentVO> list = wageAdjustmentService.list(status, workerId);
            return Result.ok(list);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 审批（仅老板）
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作");
        }
        try {
            wageAdjustmentService.approve(id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 拒绝审批（仅老板）
     */
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作");
        }
        try {
            wageAdjustmentService.reject(id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 编辑（老板/主管，仅待审批状态）
     */
    @PostMapping("/{id}/update")
    public Result<WageAdjustmentVO> update(@PathVariable Long id, @RequestBody SaveAdjustmentDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作");
        }
        try {
            WageAdjustmentVO vo = wageAdjustmentService.update(id, dto);
            return Result.ok(vo);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 删除（老板/主管，留痕）
     */
    @PostMapping("/{id}/delete")
    public Result<Void> delete(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作");
        }
        try {
            wageAdjustmentService.delete(id);
            return Result.ok(null);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 工人查看自己的记录
     */
    @GetMapping("/my")
    public Result<List<WageAdjustmentVO>> myRecords() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作");
        }
        try {
            List<WageAdjustmentVO> list = wageAdjustmentService.myRecords();
            return Result.ok(list);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}

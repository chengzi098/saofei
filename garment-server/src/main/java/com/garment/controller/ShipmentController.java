package com.garment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.ShipBundleDTO;
import com.garment.service.ShipmentService;
import com.garment.vo.ShipmentItemVO;
import com.garment.vo.ShipmentSummaryVO;
import com.garment.vo.UnshippedBundleVO;
import org.springframework.web.bind.annotation.*;

/**
 * 出货接口（主管扫码出货 / 老板每日出货查看）
 */
@RestController
@RequestMapping("/api/shipment")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    /**
     * 标记一扎为已出货
     */
    @PostMapping("/ship")
    public Result<Void> ship(@RequestBody ShipBundleDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作出货");
        }
        shipmentService.ship(dto);
        return Result.ok();
    }

    /**
     * 撤销出货
     */
    @PostMapping("/unship")
    public Result<Void> unship(@RequestBody ShipBundleDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作出货");
        }
        shipmentService.unship(dto);
        return Result.ok();
    }

    /**
     * 每日出货明细分页
     */
    @GetMapping("/daily")
    public Result<Page<ShipmentItemVO>> daily(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        return Result.ok(shipmentService.listDaily(startDate, endDate, pageNum, pageSize));
    }

    /**
     * 出货汇总
     */
    @GetMapping("/summary")
    public Result<ShipmentSummaryVO> summary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        return Result.ok(shipmentService.summary(startDate, endDate));
    }

    /**
     * 未出货扎货清单（含生产进度）
     */
    @GetMapping("/unshipped")
    public Result<java.util.List<UnshippedBundleVO>> unshipped(
            @RequestParam(required = false) Long styleId,
            @RequestParam(required = false) Long bedId) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        return Result.ok(shipmentService.listUnshipped(styleId, bedId));
    }
}

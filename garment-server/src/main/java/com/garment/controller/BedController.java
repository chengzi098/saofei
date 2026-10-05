package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.SaveBedDTO;
import com.garment.service.BedService;
import com.garment.vo.BedVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BedController {

    private final BedService bedService;

    public BedController(BedService bedService) {
        this.bedService = bedService;
    }

    /**
     * 获取款式下的所有床号
     */
    @GetMapping("/api/style/{styleId}/bed")
    public Result<List<BedVO>> listBeds(@PathVariable Long styleId) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作床号");
        }
        return Result.ok(bedService.listBeds(styleId));
    }

    /**
     * 新增床号
     */
    @PostMapping("/api/style/{styleId}/bed")
    public Result<BedVO> addBed(@PathVariable Long styleId, @RequestBody SaveBedDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作床号");
        }
        return Result.ok(bedService.addBed(styleId, dto));
    }

    /**
     * 编辑床号
     */
    @PutMapping("/api/bed/{id}")
    public Result<Void> updateBed(@PathVariable Long id, @RequestBody SaveBedDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作床号");
        }
        bedService.updateBed(id, dto);
        return Result.ok();
    }

    /**
     * 删除床号
     */
    @DeleteMapping("/api/bed/{id}")
    public Result<Void> deleteBed(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作床号");
        }
        bedService.deleteBed(id);
        return Result.ok();
    }
}

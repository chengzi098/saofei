package com.garment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.SaveBundleListDTO;
import com.garment.dto.UpdateBundleDTO;
import com.garment.service.BundleService;
import com.garment.vo.BundleVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bundle")
public class BundleController {

    private final BundleService bundleService;

    public BundleController(BundleService bundleService) {
        this.bundleService = bundleService;
    }

    /**
     * 获取当前用户的扎号前缀
     */
    @GetMapping("/my-prefix")
    public Result<String> getMyPrefix() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.ok("A");
        }
        return Result.ok(bundleService.getMyPrefix());
    }

    /**
     * 扎货列表（分页+按款式/床号筛选）
     */
    @GetMapping
    public Result<Page<BundleVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long styleId,
            @RequestParam(required = false) Long bedId,
            @RequestParam(required = false) String bundleNo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer printed) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作扎货");
        }
        return Result.ok(bundleService.listBundles(pageNum, pageSize, styleId, bedId, bundleNo, keyword, printed));
    }

    /**
     * 扎货详情（含工序快照）
     */
    @GetMapping("/{id}")
    public Result<BundleVO> getById(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作扎货");
        }
        return Result.ok(bundleService.getBundleDetail(id));
    }

    /**
     * 扫码查询扎货信息（工人扫码用）
     */
    @GetMapping("/scan/{qrCode}")
    public Result<BundleVO> scan(@PathVariable String qrCode) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能扫码");
        }
        return Result.ok(bundleService.scanBundle(qrCode));
    }

    /**
     * 批量保存扎货（主管分扎，按床号操作）
     */
    @PostMapping("/batch")
    public Result<List<BundleVO>> batchSave(@RequestBody SaveBundleListDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作扎货");
        }
        return Result.ok(bundleService.batchSave(dto.getBedId(), dto.getBundles()));
    }

    /**
     * 更新扎货信息
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateBundleDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作扎货");
        }
        bundleService.updateBundle(id, dto);
        return Result.ok();
    }

    /**
     * 批量标记扎货为已打印
     */
    @PostMapping("/mark-printed")
    public Result<Void> markPrinted(@RequestBody List<Long> ids) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作扎货");
        }
        bundleService.markPrinted(ids);
        return Result.ok();
    }

    /**
     * 删除扎货（仅待开始的可删）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作扎货");
        }
        bundleService.deleteBundle(id);
        return Result.ok();
    }
}

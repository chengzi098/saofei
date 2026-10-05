package com.garment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.SaveStyleDTO;
import com.garment.service.StyleService;
import com.garment.vo.DashboardStyleVO;
import com.garment.vo.StyleVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/style")
public class StyleController {

    private final StyleService styleService;

    public StyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    /**
     * 款式列表（分页+搜索）
     */
    @GetMapping
    public Result<Page<StyleVO>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作款式");
        }
        return Result.ok(styleService.listStyles(pageNum, pageSize, keyword, startDate, endDate));
    }

    /**
     * 新增款式
     */
    @PostMapping
    public Result<Long> add(@RequestBody SaveStyleDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作款式");
        }
        Long id = styleService.addStyle(dto);
        return Result.ok(id);
    }

    /**
     * 编辑款式
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SaveStyleDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作款式");
        }
        styleService.updateStyle(id, dto);
        return Result.ok();
    }

    /**
     * 删除款式
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作款式");
        }
        styleService.deleteStyle(id);
        return Result.ok();
    }

    /**
     * 归档款式
     */
    @PutMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作款式");
        }
        styleService.archiveStyle(id);
        return Result.ok();
    }

    /**
     * 取消归档
     */
    @PutMapping("/{id}/unarchive")
    public Result<Void> unarchive(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作款式");
        }
        styleService.unarchiveStyle(id);
        return Result.ok();
    }

    /**
     * 查询已归档款式列表（历史工作）
     */
    @GetMapping("/history")
    public Result<List<DashboardStyleVO>> getHistory() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员请先选择公司");
        }
        return Result.ok(styleService.getArchivedStyles());
    }
}

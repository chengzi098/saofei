package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.QuickImportDTO;
import com.garment.service.QuickImportService;
import com.garment.vo.QuickImportVO;
import org.springframework.web.bind.annotation.*;

/**
 * 一键录入API（供AI调用）
 */
@RestController
public class QuickImportController {

    private final QuickImportService quickImportService;

    public QuickImportController(QuickImportService quickImportService) {
        this.quickImportService = quickImportService;
    }

    /**
     * 一键录入：自动查找或创建款号、床号，批量创建扎货
     *
     * 请求示例：
     * {
     *   "styleNo": "TX-001",
     *   "styleName": "夏季T恤",
     *   "bedNo": "1号床",
     *   "bedName": "大货",
     *   "processes": [
     *     { "name": "裁剪", "price": 0.5 },
     *     { "name": "缝制", "price": 1.0 }
     *   ],
     *   "bundles": [
     *     { "quantity": 50, "size": "M" },
     *     { "quantity": 50, "size": "L" }
     *   ]
     * }
     */
    @PostMapping("/api/quick-import")
    public Result<QuickImportVO> quickImport(@RequestBody QuickImportDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作");
        }
        return Result.ok(quickImportService.quickImport(dto));
    }
}

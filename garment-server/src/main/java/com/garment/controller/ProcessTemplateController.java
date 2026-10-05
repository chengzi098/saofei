package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.dto.SaveProcessTemplateDTO;
import com.garment.service.ProcessTemplateService;
import com.garment.vo.ProcessTemplateVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProcessTemplateController {

    private final ProcessTemplateService processTemplateService;

    public ProcessTemplateController(ProcessTemplateService processTemplateService) {
        this.processTemplateService = processTemplateService;
    }

    /**
     * 模板列表
     */
    @GetMapping("/api/process-template")
    public Result<List<ProcessTemplateVO>> list() {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作工序模板");
        }
        return Result.ok(processTemplateService.list());
    }

    /**
     * 模板详情
     */
    @GetMapping("/api/process-template/{id}")
    public Result<ProcessTemplateVO> getDetail(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作工序模板");
        }
        return Result.ok(processTemplateService.getDetail(id));
    }

    /**
     * 新增模板
     */
    @PostMapping("/api/process-template")
    public Result<ProcessTemplateVO> save(@RequestBody SaveProcessTemplateDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作工序模板");
        }
        return Result.ok(processTemplateService.save(dto));
    }

    /**
     * 编辑模板
     */
    @PutMapping("/api/process-template/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SaveProcessTemplateDTO dto) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作工序模板");
        }
        processTemplateService.update(id, dto);
        return Result.ok();
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/api/process-template/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (TenantContext.isSuperAdmin() && !TenantContext.isPenetrating()) {
            return Result.error(403, "超级管理员不能操作工序模板");
        }
        processTemplateService.delete(id);
        return Result.ok();
    }
}

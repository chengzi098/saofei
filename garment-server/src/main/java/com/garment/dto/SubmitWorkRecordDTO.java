package com.garment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 提交报工DTO
 */
public class SubmitWorkRecordDTO {

    @NotNull(message = "扎货ID不能为空")
    private Long bundleId;

    @NotNull(message = "工序ID不能为空")
    private Long bundleProcessId;

    /** 指定员工ID（老板指派时使用，选填） */
    private Long workerId;

    /** 员工备注（选填） */
    private String remark;

    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
    public Long getBundleProcessId() { return bundleProcessId; }
    public void setBundleProcessId(Long bundleProcessId) { this.bundleProcessId = bundleProcessId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

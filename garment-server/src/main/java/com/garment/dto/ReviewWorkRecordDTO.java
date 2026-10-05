package com.garment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 审核报工DTO
 */
public class ReviewWorkRecordDTO {

    @NotNull(message = "审核状态不能为空")
    private Integer status; // 1通过 2驳回

    private String remark;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

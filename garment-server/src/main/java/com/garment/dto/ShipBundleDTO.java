package com.garment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 出货DTO
 */
public class ShipBundleDTO {

    @NotNull(message = "扎货ID不能为空")
    private Long bundleId;

    private String remark;

    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

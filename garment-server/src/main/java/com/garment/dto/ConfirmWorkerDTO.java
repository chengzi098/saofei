package com.garment.dto;

import java.math.BigDecimal;

public class ConfirmWorkerDTO {

    private BigDecimal supplementAmount;
    private String remark;

    public BigDecimal getSupplementAmount() { return supplementAmount; }
    public void setSupplementAmount(BigDecimal supplementAmount) { this.supplementAmount = supplementAmount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

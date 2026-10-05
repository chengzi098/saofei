package com.garment.dto;

import java.math.BigDecimal;

public class SaveAdjustmentDTO {

    private Long workerId;
    private Integer type;
    private BigDecimal amount;
    private String remark;

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}

package com.garment.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("work_record")
public class WorkRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long companyId;
    private Long workerId;
    private Long bundleId;
    private Long bundleProcessId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private Integer status;
    private Long settlementId;
    private String workerRemark;
    private Long reviewerId;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private String returnReason;
    private Long returnerId;
    private LocalDateTime returnTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
    public Long getBundleProcessId() { return bundleProcessId; }
    public void setBundleProcessId(Long bundleProcessId) { this.bundleProcessId = bundleProcessId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public String getWorkerRemark() { return workerRemark; }
    public void setWorkerRemark(String workerRemark) { this.workerRemark = workerRemark; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public Long getReturnerId() { return returnerId; }
    public void setReturnerId(Long returnerId) { this.returnerId = returnerId; }
    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}

package com.garment.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wage_settlement_detail")
public class WageSettlementDetail {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long settlementId;
    private Long companyId;
    private Long workerId;
    private Integer totalQuantity;
    private BigDecimal baseAmount;
    private BigDecimal supplementAmount;
    private BigDecimal finalAmount;
    private Integer status;
    private Integer visibleToEmployee;
    private Integer employeeConfirmed;
    private LocalDateTime employeeConfirmTime;
    private LocalDateTime confirmedTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
    public BigDecimal getSupplementAmount() { return supplementAmount; }
    public void setSupplementAmount(BigDecimal supplementAmount) { this.supplementAmount = supplementAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getVisibleToEmployee() { return visibleToEmployee; }
    public void setVisibleToEmployee(Integer visibleToEmployee) { this.visibleToEmployee = visibleToEmployee; }
    public Integer getEmployeeConfirmed() { return employeeConfirmed; }
    public void setEmployeeConfirmed(Integer employeeConfirmed) { this.employeeConfirmed = employeeConfirmed; }
    public LocalDateTime getEmployeeConfirmTime() { return employeeConfirmTime; }
    public void setEmployeeConfirmTime(LocalDateTime employeeConfirmTime) { this.employeeConfirmTime = employeeConfirmTime; }
    public LocalDateTime getConfirmedTime() { return confirmedTime; }
    public void setConfirmedTime(LocalDateTime confirmedTime) { this.confirmedTime = confirmedTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

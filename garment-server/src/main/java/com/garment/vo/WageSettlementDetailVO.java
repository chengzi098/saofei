package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WageSettlementDetailVO {

    private Long id;
    private Long workerId;
    private String workerName;
    private String workerPhone;
    private String bankCard;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public String getWorkerPhone() { return workerPhone; }
    public void setWorkerPhone(String workerPhone) { this.workerPhone = workerPhone; }
    public String getBankCard() { return bankCard; }
    public void setBankCard(String bankCard) { this.bankCard = bankCard; }
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
}

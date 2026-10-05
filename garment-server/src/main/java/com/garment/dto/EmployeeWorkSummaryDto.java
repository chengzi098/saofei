package com.garment.dto;

import java.math.BigDecimal;

/**
 * 员工工作汇总 DTO（用于老板端汇总卡片）
 */
public class EmployeeWorkSummaryDto {

    private Long workerId;
    private String workerName;
    private Integer totalQuantity;   // 总件数
    private BigDecimal totalAmount;   // 总金额
    private Integer workDays;         // 工作天数（有报工的不同日期数）
    private Integer recordCount;      // 报工记录条数

    public EmployeeWorkSummaryDto() {
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getWorkDays() {
        return workDays;
    }

    public void setWorkDays(Integer workDays) {
        this.workDays = workDays;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }
}

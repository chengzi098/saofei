package com.garment.dto;

import java.math.BigDecimal;

/**
 * 员工工作详情 DTO
 */
public class EmployeeWorkDetailDto {

    private Long workerId;
    private String workerName;
    private String bedNo;
    private String bundleNo;
    private String processName;
    private Integer quantity;
    private BigDecimal amount;
    private String date;

    public EmployeeWorkDetailDto() {
    }

    public EmployeeWorkDetailDto(Long workerId, String workerName, String bedNo, String bundleNo,
                                  String processName, Integer quantity, BigDecimal amount, String date) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.bedNo = bedNo;
        this.bundleNo = bundleNo;
        this.processName = processName;
        this.quantity = quantity;
        this.amount = amount;
        this.date = date;
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

    public String getBedNo() {
        return bedNo;
    }

    public void setBedNo(String bedNo) {
        this.bedNo = bedNo;
    }

    public String getBundleNo() {
        return bundleNo;
    }

    public void setBundleNo(String bundleNo) {
        this.bundleNo = bundleNo;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

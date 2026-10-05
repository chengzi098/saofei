package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报工记录VO
 */
public class WorkRecordVO {

    private Long id;
    private Long workerId;
    private String workerName;
    private String workerPhone;
    private Long bundleId;
    private String bundleNo;
    private Integer bundleQuantity;
    private String styleNo;
    private String styleName;
    private String bedNo;
    private Long bundleProcessId;
    private String processName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal amount;
    private Integer status;
    private String workerRemark;
    private String reviewRemark;
    private LocalDateTime reviewTime;
    private String returnReason;
    private String returnerName;
    private LocalDateTime returnTime;
    private LocalDateTime createTime;

    // 扎货工序进度
    private Integer totalProcesses;
    private Integer completedProcesses;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public String getWorkerPhone() { return workerPhone; }
    public void setWorkerPhone(String workerPhone) { this.workerPhone = workerPhone; }
    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public Integer getBundleQuantity() { return bundleQuantity; }
    public void setBundleQuantity(Integer bundleQuantity) { this.bundleQuantity = bundleQuantity; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public String getStyleName() { return styleName; }
    public void setStyleName(String styleName) { this.styleName = styleName; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public Long getBundleProcessId() { return bundleProcessId; }
    public void setBundleProcessId(Long bundleProcessId) { this.bundleProcessId = bundleProcessId; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getWorkerRemark() { return workerRemark; }
    public void setWorkerRemark(String workerRemark) { this.workerRemark = workerRemark; }
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public String getReturnerName() { return returnerName; }
    public void setReturnerName(String returnerName) { this.returnerName = returnerName; }
    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getTotalProcesses() { return totalProcesses; }
    public void setTotalProcesses(Integer totalProcesses) { this.totalProcesses = totalProcesses; }
    public Integer getCompletedProcesses() { return completedProcesses; }
    public void setCompletedProcesses(Integer completedProcesses) { this.completedProcesses = completedProcesses; }
}

package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WageSettlementVO {

    private Long id;
    private String startDate;
    private String endDate;
    private BigDecimal totalAmount;
    private Integer workerCount;
    private Integer confirmedCount;
    private Integer status;
    private Long creatorId;
    private String creatorName;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getWorkerCount() { return workerCount; }
    public void setWorkerCount(Integer workerCount) { this.workerCount = workerCount; }
    public Integer getConfirmedCount() { return confirmedCount; }
    public void setConfirmedCount(Integer confirmedCount) { this.confirmedCount = confirmedCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

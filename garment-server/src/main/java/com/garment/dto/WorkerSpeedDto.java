package com.garment.dto;

import java.time.LocalDateTime;

/**
 * 员工作业速度 DTO（用于"作业速度"柱状图）
 * 耗时口径：同一员工·同床号·同工序的相邻两次报工时间差
 */
public class WorkerSpeedDto {

    private String bundleNo;          // 扎号
    private String bedNo;             // 床号
    private String processName;       // 工序名称
    private Long durationMinutes;      // 作业耗时（分钟，首扎为 null）
    private Integer quantity;         // 该扎件数
    private LocalDateTime completeTime; // 该扎报工时间

    public WorkerSpeedDto() {
    }

    public String getBundleNo() {
        return bundleNo;
    }

    public void setBundleNo(String bundleNo) {
        this.bundleNo = bundleNo;
    }

    public String getBedNo() {
        return bedNo;
    }

    public void setBedNo(String bedNo) {
        this.bedNo = bedNo;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }
}

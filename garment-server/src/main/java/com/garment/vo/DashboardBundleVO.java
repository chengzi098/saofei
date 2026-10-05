package com.garment.vo;

import java.util.List;

public class DashboardBundleVO {
    private Long bundleId;
    private String bundleNo;
    private String size;
    private Integer quantity;
    private Integer progressPercent;
    private List<DashboardProcessVO> processes;

    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public List<DashboardProcessVO> getProcesses() { return processes; }
    public void setProcesses(List<DashboardProcessVO> processes) { this.processes = processes; }
}

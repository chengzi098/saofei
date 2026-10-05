package com.garment.vo;

import java.util.List;

public class DashboardBedVO {
    private Long bedId;
    private String bedNo;
    private String name;
    private Integer quantity;
    private Integer progressPercent;
    private List<DashboardProcessVO> processes;
    private Boolean myBed;
    private Integer myBundleCount;
    private Integer totalBundleCount;
    private List<DashboardBundleVO> bundles;

    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public List<DashboardProcessVO> getProcesses() { return processes; }
    public void setProcesses(List<DashboardProcessVO> processes) { this.processes = processes; }
    public Boolean getMyBed() { return myBed; }
    public void setMyBed(Boolean myBed) { this.myBed = myBed; }
    public Integer getMyBundleCount() { return myBundleCount; }
    public void setMyBundleCount(Integer myBundleCount) { this.myBundleCount = myBundleCount; }
    public Integer getTotalBundleCount() { return totalBundleCount; }
    public void setTotalBundleCount(Integer totalBundleCount) { this.totalBundleCount = totalBundleCount; }
    public List<DashboardBundleVO> getBundles() { return bundles; }
    public void setBundles(List<DashboardBundleVO> bundles) { this.bundles = bundles; }
}

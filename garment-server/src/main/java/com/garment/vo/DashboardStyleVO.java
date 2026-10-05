package com.garment.vo;

import java.util.List;

public class DashboardStyleVO {
    private Long styleId;
    private String styleNo;
    private Integer totalQuantity;
    private Integer progressPercent;
    private List<DashboardBedVO> beds;

    public Long getStyleId() { return styleId; }
    public void setStyleId(Long styleId) { this.styleId = styleId; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public List<DashboardBedVO> getBeds() { return beds; }
    public void setBeds(List<DashboardBedVO> beds) { this.beds = beds; }
}

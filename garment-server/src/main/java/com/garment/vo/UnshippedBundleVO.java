package com.garment.vo;

import java.time.LocalDateTime;

/**
 * 未出货扎货VO（含生产进度）
 */
public class UnshippedBundleVO {

    private Long id;
    private String bundleNo;
    private String size;
    private Integer quantity;
    private Long styleId;
    private String styleNo;
    private String styleName;
    private Long bedId;
    private String bedNo;
    /** 扎状态: 0待开始 1进行中 2已完成 */
    private Integer status;
    /** 已完成工序数 */
    private int completedProcesses;
    /** 总工序数 */
    private int totalProcesses;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getStyleId() { return styleId; }
    public void setStyleId(Long styleId) { this.styleId = styleId; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public String getStyleName() { return styleName; }
    public void setStyleName(String styleName) { this.styleName = styleName; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public int getCompletedProcesses() { return completedProcesses; }
    public void setCompletedProcesses(int completedProcesses) { this.completedProcesses = completedProcesses; }
    public int getTotalProcesses() { return totalProcesses; }
    public void setTotalProcesses(int totalProcesses) { this.totalProcesses = totalProcesses; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 床号列表VO
 */
public class BedVO {

    private Long id;
    private Long styleId;
    private String bedNo;
    private String name;
    private String colors;
    private Integer quantity;
    private String remark;
    private Integer bundleCount;
    private Integer processCount;
    private Boolean hasWorkRecords;
    private List<ProcessItem> processes;
    private LocalDateTime createTime;

    public static class ProcessItem {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer seq;
        private Boolean priceLocked;
        private Integer totalQuantity;      // 总数量（扎货总件数）
        private Integer completedQuantity;  // 已完成数量（报工数量）
        private Integer dayQuantity;        // 最高一天的完成数量
        private Double dayHours;            // 最高一天的工作时长
        private Double speedPerHour;        // 平均速度（件/小时）

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getSeq() { return seq; }
        public void setSeq(Integer seq) { this.seq = seq; }
        public Boolean getPriceLocked() { return priceLocked; }
        public void setPriceLocked(Boolean priceLocked) { this.priceLocked = priceLocked; }
        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
        public Integer getCompletedQuantity() { return completedQuantity; }
        public void setCompletedQuantity(Integer completedQuantity) { this.completedQuantity = completedQuantity; }
        public Integer getDayQuantity() { return dayQuantity; }
        public void setDayQuantity(Integer dayQuantity) { this.dayQuantity = dayQuantity; }
        public Double getDayHours() { return dayHours; }
        public void setDayHours(Double dayHours) { this.dayHours = dayHours; }
        public Double getSpeedPerHour() { return speedPerHour; }
        public void setSpeedPerHour(Double speedPerHour) { this.speedPerHour = speedPerHour; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStyleId() { return styleId; }
    public void setStyleId(Long styleId) { this.styleId = styleId; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColors() { return colors; }
    public void setColors(String colors) { this.colors = colors; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getBundleCount() { return bundleCount; }
    public void setBundleCount(Integer bundleCount) { this.bundleCount = bundleCount; }
    public Integer getProcessCount() { return processCount; }
    public void setProcessCount(Integer processCount) { this.processCount = processCount; }
    public Boolean getHasWorkRecords() { return hasWorkRecords; }
    public void setHasWorkRecords(Boolean hasWorkRecords) { this.hasWorkRecords = hasWorkRecords; }
    public List<ProcessItem> getProcesses() { return processes; }
    public void setProcesses(List<ProcessItem> processes) { this.processes = processes; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

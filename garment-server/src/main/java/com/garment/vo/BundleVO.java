package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 扎货详情VO
 */
public class BundleVO {

    private Long id;
    private String bundleNo;
    private Integer quantity;
    private String size;
    private String qrCode;
    private Integer status;
    private Integer printed;
    private Integer shipped;
    private LocalDateTime shippedTime;
    private String shipperName;
    private Long styleId;
    private String styleNo;
    private String styleName;
    private Long bedId;
    private String bedNo;
    private String bedName;
    private String colors;
    private String color;
    private LocalDateTime createTime;
    private List<ProcessItem> processes;

    public static class ProcessItem {
        private Long id;
        private String processName;
        private BigDecimal price;
        private Integer seq;
        private Integer status;
        // 已完成工序的报工记录信息（用于主管查看和退回）
        private Long workRecordId;
        private String workerName;
        private String workerRemark;
        private LocalDateTime workTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getSeq() { return seq; }
        public void setSeq(Integer seq) { this.seq = seq; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public Long getWorkRecordId() { return workRecordId; }
        public void setWorkRecordId(Long workRecordId) { this.workRecordId = workRecordId; }
        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }
        public String getWorkerRemark() { return workerRemark; }
        public void setWorkerRemark(String workerRemark) { this.workerRemark = workerRemark; }
        public LocalDateTime getWorkTime() { return workTime; }
        public void setWorkTime(LocalDateTime workTime) { this.workTime = workTime; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getPrinted() { return printed; }
    public void setPrinted(Integer printed) { this.printed = printed; }
    public Integer getShipped() { return shipped; }
    public void setShipped(Integer shipped) { this.shipped = shipped; }
    public LocalDateTime getShippedTime() { return shippedTime; }
    public void setShippedTime(LocalDateTime shippedTime) { this.shippedTime = shippedTime; }
    public String getShipperName() { return shipperName; }
    public void setShipperName(String shipperName) { this.shipperName = shipperName; }
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
    public String getBedName() { return bedName; }
    public void setBedName(String bedName) { this.bedName = bedName; }
    public String getColors() { return colors; }
    public void setColors(String colors) { this.colors = colors; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<ProcessItem> getProcesses() { return processes; }
    public void setProcesses(List<ProcessItem> processes) { this.processes = processes; }
}

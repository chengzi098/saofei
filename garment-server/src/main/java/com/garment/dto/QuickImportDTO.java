package com.garment.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 一键录入请求DTO
 */
public class QuickImportDTO {

    private String styleNo;        // 款号（必填）
    private String styleName;      // 款式名称（新建款号时必填）
    private String bedNo;          // 床号（必填）
    private String bedName;        // 床号名称（默认"大货"）
    private String colors;         // 颜色（可选）
    private List<ProcessItem> processes;  // 工序列表（可选）
    private List<BundleItem> bundles;     // 扎货列表（必填）

    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public String getStyleName() { return styleName; }
    public void setStyleName(String styleName) { this.styleName = styleName; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public String getBedName() { return bedName; }
    public void setBedName(String bedName) { this.bedName = bedName; }
    public String getColors() { return colors; }
    public void setColors(String colors) { this.colors = colors; }
    public List<ProcessItem> getProcesses() { return processes; }
    public void setProcesses(List<ProcessItem> processes) { this.processes = processes; }
    public List<BundleItem> getBundles() { return bundles; }
    public void setBundles(List<BundleItem> bundles) { this.bundles = bundles; }

    /**
     * 工序项
     */
    public static class ProcessItem {
        private String name;
        private BigDecimal price;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    /**
     * 扎货项
     */
    public static class BundleItem {
        private Integer quantity;  // 件数
        private String size;       // 尺码

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
    }
}

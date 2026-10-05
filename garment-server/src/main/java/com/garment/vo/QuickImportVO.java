package com.garment.vo;

import java.util.List;

/**
 * 一键录入响应VO
 */
public class QuickImportVO {

    private Long styleId;
    private String styleNo;
    private boolean isNewStyle;
    private Long bedId;
    private String bedNo;
    private boolean isNewBed;
    private int bundleCount;
    private int totalQuantity;
    private List<BundleInfo> bundles;

    public Long getStyleId() { return styleId; }
    public void setStyleId(Long styleId) { this.styleId = styleId; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public boolean isNewStyle() { return isNewStyle; }
    public void setNewStyle(boolean newStyle) { isNewStyle = newStyle; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public boolean isNewBed() { return isNewBed; }
    public void setNewBed(boolean newBed) { isNewBed = newBed; }
    public int getBundleCount() { return bundleCount; }
    public void setBundleCount(int bundleCount) { this.bundleCount = bundleCount; }
    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    public List<BundleInfo> getBundles() { return bundles; }
    public void setBundles(List<BundleInfo> bundles) { this.bundles = bundles; }

    /**
     * 扎货信息
     */
    public static class BundleInfo {
        private Long id;
        private String bundleNo;
        private Integer quantity;
        private String size;
        private String qrCode;

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
    }
}

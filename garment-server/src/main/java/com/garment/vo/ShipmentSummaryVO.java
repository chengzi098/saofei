package com.garment.vo;

/**
 * 出货汇总VO（按日期范围统计）
 */
public class ShipmentSummaryVO {

    /** 出货总扎数 */
    private long totalBundles;
    /** 出货总件数 */
    private long totalQuantity;
    /** 涉及款号数 */
    private long styleCount;
    /** 涉及出货人次数（去重） */
    private long shipperCount;

    public long getTotalBundles() { return totalBundles; }
    public void setTotalBundles(long totalBundles) { this.totalBundles = totalBundles; }
    public long getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(long totalQuantity) { this.totalQuantity = totalQuantity; }
    public long getStyleCount() { return styleCount; }
    public void setStyleCount(long styleCount) { this.styleCount = styleCount; }
    public long getShipperCount() { return shipperCount; }
    public void setShipperCount(long shipperCount) { this.shipperCount = shipperCount; }
}

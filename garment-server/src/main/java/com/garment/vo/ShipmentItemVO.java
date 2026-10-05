package com.garment.vo;

import java.time.LocalDateTime;

/**
 * 出货明细VO（单扎出货记录）
 */
public class ShipmentItemVO {

    private Long id;
    private Long bundleId;
    private String bundleNo;
    private String size;
    private Integer quantity;
    private Long styleId;
    private String styleNo;
    private String styleName;
    private Long bedId;
    private String bedNo;
    private String shipperName;
    private LocalDateTime shippedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
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
    public String getShipperName() { return shipperName; }
    public void setShipperName(String shipperName) { this.shipperName = shipperName; }
    public LocalDateTime getShippedTime() { return shippedTime; }
    public void setShippedTime(LocalDateTime shippedTime) { this.shippedTime = shippedTime; }
}

package com.garment.dto;

public class UpdateBundleDTO {

    private String bundleNo;
    private Integer quantity;
    private String size;
    private String color;

    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}

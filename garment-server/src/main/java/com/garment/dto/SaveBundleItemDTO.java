package com.garment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 单扎信息
 */
public class SaveBundleItemDTO {

    @NotNull(message = "件数不能为空")
    @Min(value = 1, message = "件数至少为1")
    private Integer quantity;

    private String size;
    private String color;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}

package com.garment.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("bundle")
public class Bundle {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long companyId;
    private Long styleId;
    private Long bedId;
    private String bundleNo;
    private Integer quantity;
    private String size;
    private String color;
    private String qrCode;
    private Integer status;
    private Integer printed;
    private Integer shipped;
    private LocalDateTime shippedTime;
    private Long shipperId;
    private Long creatorId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getStyleId() { return styleId; }
    public void setStyleId(Long styleId) { this.styleId = styleId; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
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
    public Long getShipperId() { return shipperId; }
    public void setShipperId(Long shipperId) { this.shipperId = shipperId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}

package com.garment.vo;

import java.time.LocalDateTime;

/**
 * 款式列表VO
 */
public class StyleVO {

    private Long id;
    private String styleNo;
    private String name;
    private Integer bedCount;
    private LocalDateTime createTime;
    private Boolean hasZeroPriceProcess;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getBedCount() { return bedCount; }
    public void setBedCount(Integer bedCount) { this.bedCount = bedCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Boolean getHasZeroPriceProcess() { return hasZeroPriceProcess; }
    public void setHasZeroPriceProcess(Boolean hasZeroPriceProcess) { this.hasZeroPriceProcess = hasZeroPriceProcess; }
}

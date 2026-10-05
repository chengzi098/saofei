package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公司信息 VO（含基本资料和统计数据）
 */
public class CompanyInfoVO {

    // === 公司基本资料 ===
    private Long companyId;
    private String companyName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private Integer status;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;

    // === 统计数据 ===
    private Integer workerCount;        // 工人数量
    private Integer supervisorCount;    // 主管数量
    private Integer styleCount;         // 款式数量
    private Integer bedCount;           // 床次总数
    private Integer monthQuantity;      // 本月产量（件数）
    private BigDecimal monthAmount;     // 本月工资总额

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getWorkerCount() { return workerCount; }
    public void setWorkerCount(Integer workerCount) { this.workerCount = workerCount; }

    public Integer getSupervisorCount() { return supervisorCount; }
    public void setSupervisorCount(Integer supervisorCount) { this.supervisorCount = supervisorCount; }

    public Integer getStyleCount() { return styleCount; }
    public void setStyleCount(Integer styleCount) { this.styleCount = styleCount; }

    public Integer getBedCount() { return bedCount; }
    public void setBedCount(Integer bedCount) { this.bedCount = bedCount; }

    public Integer getMonthQuantity() { return monthQuantity; }
    public void setMonthQuantity(Integer monthQuantity) { this.monthQuantity = monthQuantity; }

    public BigDecimal getMonthAmount() { return monthAmount; }
    public void setMonthAmount(BigDecimal monthAmount) { this.monthAmount = monthAmount; }
}

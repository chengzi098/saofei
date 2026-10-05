package com.garment.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工列表VO
 */
public class EmployeeVO {

    private Long id;            // user_company.id
    private Long userId;
    private String realName;
    private String phone;
    private Integer gender;     // 0女 1男
    private String idCard;
    private String bankCard;
    private String role;        // boss/supervisor/worker
    private String bundlePrefix; // 分扎字母前缀(A/B/C...)
    private LocalDate hireDate;
    private Integer status;     // 0离职 1在职
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getBankCard() { return bankCard; }
    public void setBankCard(String bankCard) { this.bankCard = bankCard; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getBundlePrefix() { return bundlePrefix; }
    public void setBundlePrefix(String bundlePrefix) { this.bundlePrefix = bundlePrefix; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

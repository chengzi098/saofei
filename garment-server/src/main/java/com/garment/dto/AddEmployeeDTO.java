package com.garment.dto;

import java.time.LocalDate;

/**
 * 添加员工请求DTO
 */
public class AddEmployeeDTO {

    private String phone;
    private String realName;
    private Integer gender;
    private String idCard;
    private String bankCard;
    private String role;
    private LocalDate hireDate;
    private Long existingUserId;  // 手机号已注册时传入
    private String password;      // 自定义密码，为空则自动生成随机密码

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getBankCard() { return bankCard; }
    public void setBankCard(String bankCard) { this.bankCard = bankCard; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public Long getExistingUserId() { return existingUserId; }
    public void setExistingUserId(Long existingUserId) { this.existingUserId = existingUserId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

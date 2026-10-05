package com.garment.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 批量添加员工请求DTO（超级管理员专用）
 */
public class BatchAddEmployeeDTO {

    /** 目标公司ID */
    private Long companyId;

    /** 统一初始密码 */
    private String password;

    /** 默认职位（可选，默认worker） */
    private String role;

    /** 员工列表 */
    private List<EmployeeItem> employees;

    public static class EmployeeItem {
        /** 手机号（必填） */
        private String phone;
        /** 姓名（必填） */
        private String realName;
        /** 性别（可选，0=女, 1=男） */
        private Integer gender;
        /** 入职时间（可选，默认今天） */
        private LocalDate hireDate;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public Integer getGender() { return gender; }
        public void setGender(Integer gender) { this.gender = gender; }
        public LocalDate getHireDate() { return hireDate; }
        public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public List<EmployeeItem> getEmployees() { return employees; }
    public void setEmployees(List<EmployeeItem> employees) { this.employees = employees; }
}

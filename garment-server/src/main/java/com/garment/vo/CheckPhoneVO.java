package com.garment.vo;

import java.util.List;

/**
 * 检查手机号返回VO
 */
public class CheckPhoneVO {

    private boolean exists;
    private Long userId;
    private String realName;
    private List<String> existingCompanies;

    public boolean isExists() { return exists; }
    public void setExists(boolean exists) { this.exists = exists; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public List<String> getExistingCompanies() { return existingCompanies; }
    public void setExistingCompanies(List<String> existingCompanies) { this.existingCompanies = existingCompanies; }
}

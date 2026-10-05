package com.garment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String realName;
    private String phone;
    private String idCard;
    private Integer gender;
    private String bankCard;
    private Integer type;
    private Integer status;
    private Integer needChangePassword;
    private LocalDateTime lastViewRecordsAt;
    private LocalDateTime lastViewAdjustmentAt;
    private LocalDateTime lastViewSettlementAt;
    private LocalDateTime lastViewReturnAt;
    private LocalDateTime lastViewReviewAt;
    private LocalDateTime lastViewSupervisorAdjAt;
    private LocalDateTime lastViewBossReturnAt;
    private LocalDateTime lastViewBossReviewAt;
    private LocalDateTime lastViewBossAdjAt;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getBankCard() { return bankCard; }
    public void setBankCard(String bankCard) { this.bankCard = bankCard; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getNeedChangePassword() { return needChangePassword; }
    public void setNeedChangePassword(Integer needChangePassword) { this.needChangePassword = needChangePassword; }
    public LocalDateTime getLastViewRecordsAt() { return lastViewRecordsAt; }
    public void setLastViewRecordsAt(LocalDateTime lastViewRecordsAt) { this.lastViewRecordsAt = lastViewRecordsAt; }
    public LocalDateTime getLastViewAdjustmentAt() { return lastViewAdjustmentAt; }
    public void setLastViewAdjustmentAt(LocalDateTime lastViewAdjustmentAt) { this.lastViewAdjustmentAt = lastViewAdjustmentAt; }
    public LocalDateTime getLastViewSettlementAt() { return lastViewSettlementAt; }
    public void setLastViewSettlementAt(LocalDateTime lastViewSettlementAt) { this.lastViewSettlementAt = lastViewSettlementAt; }
    public LocalDateTime getLastViewReturnAt() { return lastViewReturnAt; }
    public void setLastViewReturnAt(LocalDateTime lastViewReturnAt) { this.lastViewReturnAt = lastViewReturnAt; }
    public LocalDateTime getLastViewReviewAt() { return lastViewReviewAt; }
    public void setLastViewReviewAt(LocalDateTime lastViewReviewAt) { this.lastViewReviewAt = lastViewReviewAt; }
    public LocalDateTime getLastViewSupervisorAdjAt() { return lastViewSupervisorAdjAt; }
    public void setLastViewSupervisorAdjAt(LocalDateTime lastViewSupervisorAdjAt) { this.lastViewSupervisorAdjAt = lastViewSupervisorAdjAt; }
    public LocalDateTime getLastViewBossReturnAt() { return lastViewBossReturnAt; }
    public void setLastViewBossReturnAt(LocalDateTime lastViewBossReturnAt) { this.lastViewBossReturnAt = lastViewBossReturnAt; }
    public LocalDateTime getLastViewBossReviewAt() { return lastViewBossReviewAt; }
    public void setLastViewBossReviewAt(LocalDateTime lastViewBossReviewAt) { this.lastViewBossReviewAt = lastViewBossReviewAt; }
    public LocalDateTime getLastViewBossAdjAt() { return lastViewBossAdjAt; }
    public void setLastViewBossAdjAt(LocalDateTime lastViewBossAdjAt) { this.lastViewBossAdjAt = lastViewBossAdjAt; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}

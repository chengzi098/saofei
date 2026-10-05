package com.garment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("company_renew_log")
public class CompanyRenewLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long companyId;
    private LocalDateTime prevExpireTime;
    private LocalDateTime newExpireTime;
    private Integer months;
    private String remark;
    private Long operatorId;
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public LocalDateTime getPrevExpireTime() { return prevExpireTime; }
    public void setPrevExpireTime(LocalDateTime prevExpireTime) { this.prevExpireTime = prevExpireTime; }

    public LocalDateTime getNewExpireTime() { return newExpireTime; }
    public void setNewExpireTime(LocalDateTime newExpireTime) { this.newExpireTime = newExpireTime; }

    public Integer getMonths() { return months; }
    public void setMonths(Integer months) { this.months = months; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}

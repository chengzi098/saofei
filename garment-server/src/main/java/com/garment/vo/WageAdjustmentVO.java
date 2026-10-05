package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WageAdjustmentVO {

    private Long id;
    private Long workerId;
    private String workerName;
    private Long operatorId;
    private String operatorName;
    private Integer type;
    private BigDecimal amount;
    private String remark;
    private Integer status;
    private Long approverId;
    private String approverName;
    private LocalDateTime approveTime;
    private LocalDateTime deleteTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }

    public LocalDateTime getDeleteTime() { return deleteTime; }
    public void setDeleteTime(LocalDateTime deleteTime) { this.deleteTime = deleteTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

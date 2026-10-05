package com.garment.vo;

public class DashboardProcessVO {
    private Long bundleProcessId;
    private String processName;
    private Integer status;
    private String statusText;
    private String operatorName;
    private String completeTime;

    public Long getBundleProcessId() { return bundleProcessId; }
    public void setBundleProcessId(Long bundleProcessId) { this.bundleProcessId = bundleProcessId; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getCompleteTime() { return completeTime; }
    public void setCompleteTime(String completeTime) { this.completeTime = completeTime; }
}

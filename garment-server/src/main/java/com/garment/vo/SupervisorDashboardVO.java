package com.garment.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 主管工作台统计数据
 */
public class SupervisorDashboardVO {

    /** 我创建的扎货总数 */
    private int myBundleCount;

    /** 我的扎货总件数 */
    private int myTotalQuantity;

    /** 今日报工条数（本公司） */
    private int todayReportCount;

    /** 今日产值（本公司报工金额合计） */
    private BigDecimal todayAmount;

    /** 进行中的款式数 */
    private int activeStyleCount;

    /** 退回记录数（本公司近7天） */
    private int returnCount;

    /** 我的款式进度概览 */
    private List<MyStyleProgress> myStyles;

    /** 最近报工记录（前5条） */
    private List<RecentReport> recentReports;

    public int getMyBundleCount() { return myBundleCount; }
    public void setMyBundleCount(int myBundleCount) { this.myBundleCount = myBundleCount; }

    public int getMyTotalQuantity() { return myTotalQuantity; }
    public void setMyTotalQuantity(int myTotalQuantity) { this.myTotalQuantity = myTotalQuantity; }

    public int getTodayReportCount() { return todayReportCount; }
    public void setTodayReportCount(int todayReportCount) { this.todayReportCount = todayReportCount; }

    public BigDecimal getTodayAmount() { return todayAmount; }
    public void setTodayAmount(BigDecimal todayAmount) { this.todayAmount = todayAmount; }

    public int getActiveStyleCount() { return activeStyleCount; }
    public void setActiveStyleCount(int activeStyleCount) { this.activeStyleCount = activeStyleCount; }

    public int getReturnCount() { return returnCount; }
    public void setReturnCount(int returnCount) { this.returnCount = returnCount; }

    public List<MyStyleProgress> getMyStyles() { return myStyles; }
    public void setMyStyles(List<MyStyleProgress> myStyles) { this.myStyles = myStyles; }

    public List<RecentReport> getRecentReports() { return recentReports; }
    public void setRecentReports(List<RecentReport> recentReports) { this.recentReports = recentReports; }

    /**
     * 我负责的款式进度
     */
    public static class MyStyleProgress {
        private Long styleId;
        private String styleNo;
        private int myBundleCount;
        private int totalBundleCount;
        private int progressPercent;

        public Long getStyleId() { return styleId; }
        public void setStyleId(Long styleId) { this.styleId = styleId; }
        public String getStyleNo() { return styleNo; }
        public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
        public int getMyBundleCount() { return myBundleCount; }
        public void setMyBundleCount(int myBundleCount) { this.myBundleCount = myBundleCount; }
        public int getTotalBundleCount() { return totalBundleCount; }
        public void setTotalBundleCount(int totalBundleCount) { this.totalBundleCount = totalBundleCount; }
        public int getProgressPercent() { return progressPercent; }
        public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    }

    /**
     * 最近报工记录
     */
    public static class RecentReport {
        private Long id;
        private String workerName;
        private String processName;
        private String bundleNo;
        private int quantity;
        private BigDecimal amount;
        private String createTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        public String getBundleNo() { return bundleNo; }
        public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
    }
}

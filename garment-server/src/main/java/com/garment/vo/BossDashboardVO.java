package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 老板工作台统计数据
 */
public class BossDashboardVO {

    /** 公司到期时间 */
    private LocalDateTime expireTime;

    /** 今日报工产值（本公司今日已通过报工金额合计） */
    private BigDecimal todayAmount;

    /** 今日报工条数 */
    private int todayReportCount;

    /** 本月报工金额（本公司本月已通过报工金额合计） */
    private BigDecimal monthAmount;

    /** 本周报工金额（本公司本周已通过报工金额合计） */
    private BigDecimal weekAmount;

    /** 本月工资（工资结算单总额） */
    private BigDecimal monthWage;

    /** 今日活跃员工数（今日报工的不同 workerId 去重计数） */
    private int activeEmployeeCount;

    /** 在职员工总数（不含老板） */
    private int totalEmployeeCount;

    /** 进行中款式数（有未完成扎货的款式） */
    private int activeStyleCount;

    /** 近7天退回记录数 */
    private int returnCount;

    /** 全公司款式进度概览 */
    private List<StyleProgress> styles;

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public BigDecimal getTodayAmount() { return todayAmount; }
    public void setTodayAmount(BigDecimal todayAmount) { this.todayAmount = todayAmount; }

    public int getTodayReportCount() { return todayReportCount; }
    public void setTodayReportCount(int todayReportCount) { this.todayReportCount = todayReportCount; }

    public BigDecimal getMonthAmount() { return monthAmount; }
    public void setMonthAmount(BigDecimal monthAmount) { this.monthAmount = monthAmount; }

    public BigDecimal getWeekAmount() { return weekAmount; }
    public void setWeekAmount(BigDecimal weekAmount) { this.weekAmount = weekAmount; }

    public BigDecimal getMonthWage() { return monthWage; }
    public void setMonthWage(BigDecimal monthWage) { this.monthWage = monthWage; }

    public int getActiveEmployeeCount() { return activeEmployeeCount; }
    public void setActiveEmployeeCount(int activeEmployeeCount) { this.activeEmployeeCount = activeEmployeeCount; }

    public int getTotalEmployeeCount() { return totalEmployeeCount; }
    public void setTotalEmployeeCount(int totalEmployeeCount) { this.totalEmployeeCount = totalEmployeeCount; }

    public int getActiveStyleCount() { return activeStyleCount; }
    public void setActiveStyleCount(int activeStyleCount) { this.activeStyleCount = activeStyleCount; }

    public int getReturnCount() { return returnCount; }
    public void setReturnCount(int returnCount) { this.returnCount = returnCount; }

    public List<StyleProgress> getStyles() { return styles; }
    public void setStyles(List<StyleProgress> styles) { this.styles = styles; }

    /**
     * 款式进度（全公司）
     */
    public static class StyleProgress {
        private Long styleId;
        private String styleNo;
        private int totalBundleCount;
        private int progressPercent;

        public Long getStyleId() { return styleId; }
        public void setStyleId(Long styleId) { this.styleId = styleId; }
        public String getStyleNo() { return styleNo; }
        public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
        public int getTotalBundleCount() { return totalBundleCount; }
        public void setTotalBundleCount(int totalBundleCount) { this.totalBundleCount = totalBundleCount; }
        public int getProgressPercent() { return progressPercent; }
        public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    }
}

package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 超级管理员全局统计数据
 */
public class AdminDashboardVO {

    /** 入驻公司总数 */
    private int totalCompanyCount;

    /** 正常状态公司数 */
    private int activeCompanyCount;

    /** 禁用状态公司数 */
    private int disabledCompanyCount;

    /** 在职员工总数（所有公司，不含老板） */
    private int totalEmployeeCount;

    /** 总款式数 */
    private int totalStyleCount;

    /** 本月全局报工产值 */
    private BigDecimal monthAmount;

    /** 今日全局报工条数 */
    private int todayReportCount;

    /** 今日全局报工产值 */
    private BigDecimal todayAmount;

    /** 近30天新增公司数 */
    private int newCompanyCount;

    /** 即将到期公司列表（30天内） */
    private List<ExpiringCompany> expiringCompanies;

    /** 最近活跃公司（按本月产值排序Top5） */
    private List<ActiveCompany> activeCompanies;

    /** 总扎货数（所有公司） */
    private int totalBundleCount;

    /** 总报工条数（所有公司，本月已通过） */
    private int totalWorkRecordCount;

    /** 近6个月产值趋势 */
    private List<MonthlyTrend> monthTrends;

    /** 各公司员工分布（Top10） */
    private List<CompanyEmployeeStat> companyEmployeeStats;

    public int getTotalCompanyCount() { return totalCompanyCount; }
    public void setTotalCompanyCount(int totalCompanyCount) { this.totalCompanyCount = totalCompanyCount; }

    public int getActiveCompanyCount() { return activeCompanyCount; }
    public void setActiveCompanyCount(int activeCompanyCount) { this.activeCompanyCount = activeCompanyCount; }

    public int getDisabledCompanyCount() { return disabledCompanyCount; }
    public void setDisabledCompanyCount(int disabledCompanyCount) { this.disabledCompanyCount = disabledCompanyCount; }

    public int getTotalEmployeeCount() { return totalEmployeeCount; }
    public void setTotalEmployeeCount(int totalEmployeeCount) { this.totalEmployeeCount = totalEmployeeCount; }

    public int getTotalStyleCount() { return totalStyleCount; }
    public void setTotalStyleCount(int totalStyleCount) { this.totalStyleCount = totalStyleCount; }

    public BigDecimal getMonthAmount() { return monthAmount; }
    public void setMonthAmount(BigDecimal monthAmount) { this.monthAmount = monthAmount; }

    public int getTodayReportCount() { return todayReportCount; }
    public void setTodayReportCount(int todayReportCount) { this.todayReportCount = todayReportCount; }

    public BigDecimal getTodayAmount() { return todayAmount; }
    public void setTodayAmount(BigDecimal todayAmount) { this.todayAmount = todayAmount; }

    public int getNewCompanyCount() { return newCompanyCount; }
    public void setNewCompanyCount(int newCompanyCount) { this.newCompanyCount = newCompanyCount; }

    public List<ExpiringCompany> getExpiringCompanies() { return expiringCompanies; }
    public void setExpiringCompanies(List<ExpiringCompany> expiringCompanies) { this.expiringCompanies = expiringCompanies; }

    public List<ActiveCompany> getActiveCompanies() { return activeCompanies; }
    public void setActiveCompanies(List<ActiveCompany> activeCompanies) { this.activeCompanies = activeCompanies; }

    public int getTotalBundleCount() { return totalBundleCount; }
    public void setTotalBundleCount(int totalBundleCount) { this.totalBundleCount = totalBundleCount; }

    public int getTotalWorkRecordCount() { return totalWorkRecordCount; }
    public void setTotalWorkRecordCount(int totalWorkRecordCount) { this.totalWorkRecordCount = totalWorkRecordCount; }

    public List<MonthlyTrend> getMonthTrends() { return monthTrends; }
    public void setMonthTrends(List<MonthlyTrend> monthTrends) { this.monthTrends = monthTrends; }

    public List<CompanyEmployeeStat> getCompanyEmployeeStats() { return companyEmployeeStats; }
    public void setCompanyEmployeeStats(List<CompanyEmployeeStat> companyEmployeeStats) { this.companyEmployeeStats = companyEmployeeStats; }

    /**
     * 即将到期公司
     */
    public static class ExpiringCompany {
        private Long id;
        private String name;
        private LocalDateTime expireTime;
        private int remainingDays;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDateTime getExpireTime() { return expireTime; }
        public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
        public int getRemainingDays() { return remainingDays; }
        public void setRemainingDays(int remainingDays) { this.remainingDays = remainingDays; }
    }

    /**
     * 活跃公司（按产值排序）
     */
    public static class ActiveCompany {
        private Long id;
        private String name;
        private BigDecimal monthAmount;
        private int employeeCount;
        private int styleCount;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getMonthAmount() { return monthAmount; }
        public void setMonthAmount(BigDecimal monthAmount) { this.monthAmount = monthAmount; }
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        public int getStyleCount() { return styleCount; }
        public void setStyleCount(int styleCount) { this.styleCount = styleCount; }
    }

    /**
     * 月度趋势数据
     */
    public static class MonthlyTrend {
        private String yearMonth;
        private BigDecimal amount;

        public String getYearMonth() { return yearMonth; }
        public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /**
     * 公司员工分布统计
     */
    public static class CompanyEmployeeStat {
        private Long companyId;
        private String companyName;
        private int employeeCount;
        private BigDecimal monthAmount;

        public Long getCompanyId() { return companyId; }
        public void setCompanyId(Long companyId) { this.companyId = companyId; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        public BigDecimal getMonthAmount() { return monthAmount; }
        public void setMonthAmount(BigDecimal monthAmount) { this.monthAmount = monthAmount; }
    }
}

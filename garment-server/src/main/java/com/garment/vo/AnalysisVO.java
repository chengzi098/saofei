package com.garment.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据分析 VO（用于AI分析）
 */
public class AnalysisVO {

    // === 员工绩效分析 ===
    public static class EmployeePerformance {
        private Long workerId;
        private String workerName;
        private Integer totalQuantity;      // 总产量
        private BigDecimal totalAmount;     // 总工资
        private Integer monthQuantity;      // 本月产量
        private BigDecimal monthAmount;     // 本月工资
        private Integer workDays;           // 工作天数
        private BigDecimal avgDailyOutput;  // 日均产量

        // getters and setters
        public Long getWorkerId() { return workerId; }
        public void setWorkerId(Long workerId) { this.workerId = workerId; }
        public String getWorkerName() { return workerName; }
        public void setWorkerName(String workerName) { this.workerName = workerName; }
        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public Integer getMonthQuantity() { return monthQuantity; }
        public void setMonthQuantity(Integer monthQuantity) { this.monthQuantity = monthQuantity; }
        public BigDecimal getMonthAmount() { return monthAmount; }
        public void setMonthAmount(BigDecimal monthAmount) { this.monthAmount = monthAmount; }
        public Integer getWorkDays() { return workDays; }
        public void setWorkDays(Integer workDays) { this.workDays = workDays; }
        public BigDecimal getAvgDailyOutput() { return avgDailyOutput; }
        public void setAvgDailyOutput(BigDecimal avgDailyOutput) { this.avgDailyOutput = avgDailyOutput; }
    }

    // === 产量趋势分析 ===
    public static class ProductionTrend {
        private String date;                // 日期
        private Integer quantity;           // 产量
        private BigDecimal amount;          // 产值
        private Integer recordCount;        // 报工次数

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Integer getRecordCount() { return recordCount; }
        public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    }

    // === 款式进度分析 ===
    public static class StyleProgress {
        private Long styleId;
        private String styleNo;
        private String styleName;
        private Integer totalBundles;       // 总扎数
        private Integer completedBundles;   // 已完成扎数
        private Integer totalQuantity;      // 总件数
        private Integer completedQuantity;  // 已完成件数
        private BigDecimal progressPercent; // 进度百分比
        private BigDecimal totalAmount;     // 总产值

        public Long getStyleId() { return styleId; }
        public void setStyleId(Long styleId) { this.styleId = styleId; }
        public String getStyleNo() { return styleNo; }
        public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
        public String getStyleName() { return styleName; }
        public void setStyleName(String styleName) { this.styleName = styleName; }
        public Integer getTotalBundles() { return totalBundles; }
        public void setTotalBundles(Integer totalBundles) { this.totalBundles = totalBundles; }
        public Integer getCompletedBundles() { return completedBundles; }
        public void setCompletedBundles(Integer completedBundles) { this.completedBundles = completedBundles; }
        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
        public Integer getCompletedQuantity() { return completedQuantity; }
        public void setCompletedQuantity(Integer completedQuantity) { this.completedQuantity = completedQuantity; }
        public BigDecimal getProgressPercent() { return progressPercent; }
        public void setProgressPercent(BigDecimal progressPercent) { this.progressPercent = progressPercent; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    // === 工序效率分析 ===
    public static class ProcessEfficiency {
        private Long processId;
        private String processName;
        private Integer totalQuantity;      // 总产量
        private BigDecimal avgPrice;        // 平均单价
        private Integer workerCount;        // 参与工人数
        private BigDecimal totalAmount;     // 总产值

        public Long getProcessId() { return processId; }
        public void setProcessId(Long processId) { this.processId = processId; }
        public String getProcessName() { return processName; }
        public void setProcessName(String processName) { this.processName = processName; }
        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
        public BigDecimal getAvgPrice() { return avgPrice; }
        public void setAvgPrice(BigDecimal avgPrice) { this.avgPrice = avgPrice; }
        public Integer getWorkerCount() { return workerCount; }
        public void setWorkerCount(Integer workerCount) { this.workerCount = workerCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    // === 工资分布分析 ===
    public static class WageDistribution {
        private String range;               // 工资区间
        private Integer count;              // 人数
        private BigDecimal percentage;      // 占比

        public String getRange() { return range; }
        public void setRange(String range) { this.range = range; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public BigDecimal getPercentage() { return percentage; }
        public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    }

    // === 综合分析报告 ===
    public static class AnalysisReport {
        private CompanyInfoVO companyInfo;
        private List<EmployeePerformance> topPerformers;
        private List<ProductionTrend> productionTrends;
        private List<StyleProgress> styleProgress;
        private List<ProcessEfficiency> processEfficiency;
        private List<WageDistribution> wageDistribution;
        private Map<String, Object> summary;

        public CompanyInfoVO getCompanyInfo() { return companyInfo; }
        public void setCompanyInfo(CompanyInfoVO companyInfo) { this.companyInfo = companyInfo; }
        public List<EmployeePerformance> getTopPerformers() { return topPerformers; }
        public void setTopPerformers(List<EmployeePerformance> topPerformers) { this.topPerformers = topPerformers; }
        public List<ProductionTrend> getProductionTrends() { return productionTrends; }
        public void setProductionTrends(List<ProductionTrend> productionTrends) { this.productionTrends = productionTrends; }
        public List<StyleProgress> getStyleProgress() { return styleProgress; }
        public void setStyleProgress(List<StyleProgress> styleProgress) { this.styleProgress = styleProgress; }
        public List<ProcessEfficiency> getProcessEfficiency() { return processEfficiency; }
        public void setProcessEfficiency(List<ProcessEfficiency> processEfficiency) { this.processEfficiency = processEfficiency; }
        public List<WageDistribution> getWageDistribution() { return wageDistribution; }
        public void setWageDistribution(List<WageDistribution> wageDistribution) { this.wageDistribution = wageDistribution; }
        public Map<String, Object> getSummary() { return summary; }
        public void setSummary(Map<String, Object> summary) { this.summary = summary; }
    }
}

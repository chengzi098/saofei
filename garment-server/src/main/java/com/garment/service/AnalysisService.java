package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.entity.*;
import com.garment.mapper.*;
import com.garment.vo.AnalysisVO.*;
import com.garment.vo.CompanyInfoVO;
import org.springframework.stereotype.Service;

import java.lang.Process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据分析服务（用于AI分析）
 */
@Service
public class AnalysisService {

    private final CompanyService companyService;
    private final UserCompanyMapper userCompanyMapper;
    private final UserMapper userMapper;
    private final WorkRecordMapper workRecordMapper;
    private final StyleMapper styleMapper;
    private final BedMapper bedMapper;
    private final BundleMapper bundleMapper;
    private final BundleProcessMapper bundleProcessMapper;
    private final ProcessMapper processMapper;

    public AnalysisService(CompanyService companyService,
                          UserCompanyMapper userCompanyMapper,
                          UserMapper userMapper,
                          WorkRecordMapper workRecordMapper,
                          StyleMapper styleMapper,
                          BedMapper bedMapper,
                          BundleMapper bundleMapper,
                          BundleProcessMapper bundleProcessMapper,
                          ProcessMapper processMapper) {
        this.companyService = companyService;
        this.userCompanyMapper = userCompanyMapper;
        this.userMapper = userMapper;
        this.workRecordMapper = workRecordMapper;
        this.styleMapper = styleMapper;
        this.bedMapper = bedMapper;
        this.bundleMapper = bundleMapper;
        this.bundleProcessMapper = bundleProcessMapper;
        this.processMapper = processMapper;
    }

    /**
     * 获取员工绩效分析（Top N）
     */
    public List<EmployeePerformance> getEmployeePerformance(Long companyId, int topN) {
        // 获取所有工人
        List<UserCompany> workers = userCompanyMapper.selectList(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getRole, "worker")
                        .eq(UserCompany::getStatus, 1)
        );

        if (workers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> workerIds = workers.stream()
                .map(UserCompany::getUserId)
                .collect(Collectors.toList());

        // 获取所有工人信息
        Map<Long, User> userMap = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getId, workerIds)
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        // 获取所有工作记录
        List<WorkRecord> allRecords = workRecordMapper.selectList(
                new LambdaQueryWrapper<WorkRecord>()
                        .eq(WorkRecord::getCompanyId, companyId)
                        .in(WorkRecord::getWorkerId, workerIds)
        );

        // 计算本月时间范围
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // 按工人分组统计
        Map<Long, List<WorkRecord>> recordsByWorker = allRecords.stream()
                .collect(Collectors.groupingBy(WorkRecord::getWorkerId));

        List<EmployeePerformance> performances = new ArrayList<>();
        for (UserCompany worker : workers) {
            Long workerId = worker.getUserId();
            User user = userMap.get(workerId);
            if (user == null) continue;

            List<WorkRecord> workerRecords = recordsByWorker.getOrDefault(workerId, Collections.emptyList());

            EmployeePerformance perf = new EmployeePerformance();
            perf.setWorkerId(workerId);
            perf.setWorkerName(user.getRealName() != null ? user.getRealName() : user.getUsername());

            // 总产量和总工资
            int totalQty = workerRecords.stream()
                    .filter(r -> r.getQuantity() != null)
                    .mapToInt(WorkRecord::getQuantity)
                    .sum();
            BigDecimal totalAmt = workerRecords.stream()
                    .filter(r -> r.getAmount() != null)
                    .map(WorkRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            perf.setTotalQuantity(totalQty);
            perf.setTotalAmount(totalAmt);

            // 本月产量和工资
            List<WorkRecord> monthRecords = workerRecords.stream()
                    .filter(r -> r.getCreateTime() != null &&
                                 r.getCreateTime().isAfter(monthStart) &&
                                 r.getCreateTime().isBefore(monthEnd))
                    .collect(Collectors.toList());

            int monthQty = monthRecords.stream()
                    .filter(r -> r.getQuantity() != null)
                    .mapToInt(WorkRecord::getQuantity)
                    .sum();
            BigDecimal monthAmt = monthRecords.stream()
                    .filter(r -> r.getAmount() != null)
                    .map(WorkRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            perf.setMonthQuantity(monthQty);
            perf.setMonthAmount(monthAmt);

            // 工作天数
            long workDays = workerRecords.stream()
                    .filter(r -> r.getCreateTime() != null)
                    .map(r -> r.getCreateTime().toLocalDate())
                    .distinct()
                    .count();
            perf.setWorkDays((int) workDays);

            // 日均产量
            if (workDays > 0) {
                perf.setAvgDailyOutput(BigDecimal.valueOf(totalQty)
                        .divide(BigDecimal.valueOf(workDays), 2, RoundingMode.HALF_UP));
            } else {
                perf.setAvgDailyOutput(BigDecimal.ZERO);
            }

            performances.add(perf);
        }

        // 按本月产量排序，取Top N
        return performances.stream()
                .sorted((a, b) -> b.getMonthQuantity().compareTo(a.getMonthQuantity()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * 获取产量趋势分析（最近N天）
     */
    public List<ProductionTrend> getProductionTrend(Long companyId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<WorkRecord> records = workRecordMapper.selectList(
                new LambdaQueryWrapper<WorkRecord>()
                        .eq(WorkRecord::getCompanyId, companyId)
                        .ge(WorkRecord::getCreateTime, startDate)
        );

        // 按日期分组
        Map<String, List<WorkRecord>> recordsByDate = records.stream()
                .filter(r -> r.getCreateTime() != null)
                .collect(Collectors.groupingBy(r -> r.getCreateTime().format(formatter)));

        List<ProductionTrend> trends = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(formatter);
            List<WorkRecord> dayRecords = recordsByDate.getOrDefault(dateStr, Collections.emptyList());

            ProductionTrend trend = new ProductionTrend();
            trend.setDate(dateStr);
            trend.setQuantity(dayRecords.stream()
                    .filter(r -> r.getQuantity() != null)
                    .mapToInt(WorkRecord::getQuantity)
                    .sum());
            trend.setAmount(dayRecords.stream()
                    .filter(r -> r.getAmount() != null)
                    .map(WorkRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            trend.setRecordCount(dayRecords.size());

            trends.add(trend);
        }

        return trends;
    }

    /**
     * 获取款式进度分析
     */
    public List<StyleProgress> getStyleProgress(Long companyId) {
        List<Style> styles = styleMapper.selectList(
                new LambdaQueryWrapper<Style>()
                        .eq(Style::getCompanyId, companyId)
                        .eq(Style::getArchived, 0)
        );

        if (styles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> styleIds = styles.stream().map(Style::getId).collect(Collectors.toList());

        // 获取所有床次
        List<Bed> beds = bedMapper.selectList(
                new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getCompanyId, companyId)
                        .in(Bed::getStyleId, styleIds)
        );

        // 获取所有扎
        List<Bundle> bundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getCompanyId, companyId)
                        .in(Bundle::getStyleId, styleIds)
        );

        // 获取所有扎工序
        List<BundleProcess> bundleProcesses = bundleProcessMapper.selectList(
                new LambdaQueryWrapper<BundleProcess>()
                        .eq(BundleProcess::getCompanyId, companyId)
                        .in(BundleProcess::getBundleId, bundles.stream()
                                .map(Bundle::getId)
                                .collect(Collectors.toList()))
        );

        // 获取所有工作记录
        List<WorkRecord> workRecords = workRecordMapper.selectList(
                new LambdaQueryWrapper<WorkRecord>()
                        .eq(WorkRecord::getCompanyId, companyId)
                        .in(WorkRecord::getBundleId, bundles.stream()
                                .map(Bundle::getId)
                                .collect(Collectors.toList()))
        );

        // 按款式分组统计
        Map<Long, List<Bundle>> bundlesByStyle = bundles.stream()
                .collect(Collectors.groupingBy(Bundle::getStyleId));

        Map<Long, List<WorkRecord>> recordsByBundle = workRecords.stream()
                .filter(r -> r.getBundleId() != null)
                .collect(Collectors.groupingBy(WorkRecord::getBundleId));

        List<StyleProgress> progressList = new ArrayList<>();
        for (Style style : styles) {
            List<Bundle> styleBundles = bundlesByStyle.getOrDefault(style.getId(), Collections.emptyList());

            StyleProgress progress = new StyleProgress();
            progress.setStyleId(style.getId());
            progress.setStyleNo(style.getStyleNo());
            progress.setStyleName(style.getName());
            progress.setTotalBundles(styleBundles.size());

            int totalQty = styleBundles.stream()
                    .filter(b -> b.getQuantity() != null)
                    .mapToInt(Bundle::getQuantity)
                    .sum();
            progress.setTotalQuantity(totalQty);

            // 计算已完成扎数和件数
            int completedBundles = 0;
            int completedQty = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (Bundle bundle : styleBundles) {
                List<WorkRecord> bundleRecords = recordsByBundle.getOrDefault(bundle.getId(), Collections.emptyList());
                if (!bundleRecords.isEmpty()) {
                    completedBundles++;
                    completedQty += bundleRecords.stream()
                            .filter(r -> r.getQuantity() != null)
                            .mapToInt(WorkRecord::getQuantity)
                            .sum();
                    totalAmount = totalAmount.add(bundleRecords.stream()
                            .filter(r -> r.getAmount() != null)
                            .map(WorkRecord::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                }
            }

            progress.setCompletedBundles(completedBundles);
            progress.setCompletedQuantity(completedQty);
            progress.setTotalAmount(totalAmount);

            // 计算进度百分比
            if (totalQty > 0) {
                progress.setProgressPercent(BigDecimal.valueOf(completedQty)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalQty), 2, RoundingMode.HALF_UP));
            } else {
                progress.setProgressPercent(BigDecimal.ZERO);
            }

            progressList.add(progress);
        }

        return progressList.stream()
                .sorted((a, b) -> b.getProgressPercent().compareTo(a.getProgressPercent()))
                .collect(Collectors.toList());
    }

    /**
     * 获取工序效率分析
     */
    public List<ProcessEfficiency> getProcessEfficiency(Long companyId) {
        // 获取所有工作记录
        List<WorkRecord> records = workRecordMapper.selectList(
                new LambdaQueryWrapper<WorkRecord>()
                        .eq(WorkRecord::getCompanyId, companyId)
        );

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有扎工序信息
        List<Long> bundleProcessIds = records.stream()
                .filter(r -> r.getBundleProcessId() != null)
                .map(WorkRecord::getBundleProcessId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, BundleProcess> bundleProcessMap = bundleProcessMapper.selectList(
                new LambdaQueryWrapper<BundleProcess>().in(BundleProcess::getId, bundleProcessIds)
        ).stream().collect(Collectors.toMap(BundleProcess::getId, bp -> bp));

        // 按工序名称分组统计（因为BundleProcess没有processId，使用processName）
        Map<String, List<WorkRecord>> recordsByProcessName = new HashMap<>();
        for (WorkRecord record : records) {
            if (record.getBundleProcessId() != null) {
                BundleProcess bp = bundleProcessMap.get(record.getBundleProcessId());
                if (bp != null) {
                    String processName = bp.getProcessName() != null ? bp.getProcessName() : "未知工序";
                    recordsByProcessName.computeIfAbsent(processName, k -> new ArrayList<>()).add(record);
                }
            }
        }

        List<ProcessEfficiency> efficiencies = new ArrayList<>();
        for (Map.Entry<String, List<WorkRecord>> entry : recordsByProcessName.entrySet()) {
            String processName = entry.getKey();
            List<WorkRecord> processRecords = entry.getValue();

            ProcessEfficiency eff = new ProcessEfficiency();
            eff.setProcessId(0L); // 使用0作为默认ID
            eff.setProcessName(processName);

            int totalQty = processRecords.stream()
                    .filter(r -> r.getQuantity() != null)
                    .mapToInt(WorkRecord::getQuantity)
                    .sum();
            BigDecimal totalAmt = processRecords.stream()
                    .filter(r -> r.getAmount() != null)
                    .map(WorkRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            eff.setTotalQuantity(totalQty);
            eff.setTotalAmount(totalAmt);

            // 计算平均单价
            if (totalQty > 0) {
                eff.setAvgPrice(totalAmt.divide(BigDecimal.valueOf(totalQty), 4, RoundingMode.HALF_UP));
            } else {
                eff.setAvgPrice(BigDecimal.ZERO);
            }

            // 参与工人数
            long workerCount = processRecords.stream()
                    .filter(r -> r.getWorkerId() != null)
                    .map(WorkRecord::getWorkerId)
                    .distinct()
                    .count();
            eff.setWorkerCount((int) workerCount);

            efficiencies.add(eff);
        }

        return efficiencies.stream()
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .collect(Collectors.toList());
    }

    /**
     * 获取工资分布分析
     */
    public List<WageDistribution> getWageDistribution(Long companyId) {
        // 获取所有工人
        List<UserCompany> workers = userCompanyMapper.selectList(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getRole, "worker")
                        .eq(UserCompany::getStatus, 1)
        );

        if (workers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> workerIds = workers.stream()
                .map(UserCompany::getUserId)
                .collect(Collectors.toList());

        // 计算本月时间范围
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // 获取本月所有工作记录
        List<WorkRecord> monthRecords = workRecordMapper.selectList(
                new LambdaQueryWrapper<WorkRecord>()
                        .eq(WorkRecord::getCompanyId, companyId)
                        .in(WorkRecord::getWorkerId, workerIds)
                        .ge(WorkRecord::getCreateTime, monthStart)
                        .le(WorkRecord::getCreateTime, monthEnd)
        );

        // 按工人分组计算本月工资
        Map<Long, BigDecimal> wageByWorker = new HashMap<>();
        for (WorkRecord record : monthRecords) {
            if (record.getWorkerId() != null && record.getAmount() != null) {
                wageByWorker.merge(record.getWorkerId(), record.getAmount(), BigDecimal::add);
            }
        }

        // 定义工资区间
        int[][] ranges = {
            {0, 1000},
            {1000, 2000},
            {2000, 3000},
            {3000, 5000},
            {5000, 8000},
            {8000, 10000},
            {10000, Integer.MAX_VALUE}
        };

        String[] rangeLabels = {
            "0-1000元",
            "1000-2000元",
            "2000-3000元",
            "3000-5000元",
            "5000-8000元",
            "8000-10000元",
            "10000元以上"
        };

        List<WageDistribution> distributions = new ArrayList<>();
        int totalWorkers = wageByWorker.size();

        for (int i = 0; i < ranges.length; i++) {
            int min = ranges[i][0];
            int max = ranges[i][1];

            long count = wageByWorker.values().stream()
                    .filter(wage -> wage.compareTo(BigDecimal.valueOf(min)) >= 0 &&
                                   (max == Integer.MAX_VALUE || wage.compareTo(BigDecimal.valueOf(max)) < 0))
                    .count();

            WageDistribution dist = new WageDistribution();
            dist.setRange(rangeLabels[i]);
            dist.setCount((int) count);

            if (totalWorkers > 0) {
                dist.setPercentage(BigDecimal.valueOf(count)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalWorkers), 2, RoundingMode.HALF_UP));
            } else {
                dist.setPercentage(BigDecimal.ZERO);
            }

            distributions.add(dist);
        }

        return distributions;
    }

    /**
     * 获取综合分析报告
     */
    public AnalysisReport getAnalysisReport(Long companyId) {
        AnalysisReport report = new AnalysisReport();

        // 公司信息
        report.setCompanyInfo(companyService.getCompanyInfo(companyId));

        // 员工绩效 Top 10
        report.setTopPerformers(getEmployeePerformance(companyId, 10));

        // 产量趋势（最近30天）
        report.setProductionTrends(getProductionTrend(companyId, 30));

        // 款式进度
        report.setStyleProgress(getStyleProgress(companyId));

        // 工序效率
        report.setProcessEfficiency(getProcessEfficiency(companyId));

        // 工资分布
        report.setWageDistribution(getWageDistribution(companyId));

        // 数据摘要
        report.setSummary(buildSummary(report));

        return report;
    }

    /**
     * 获取数据摘要
     */
    public Map<String, Object> getSummary(Long companyId) {
        AnalysisReport report = getAnalysisReport(companyId);
        return buildSummary(report);
    }

    /**
     * 构建数据摘要
     */
    private Map<String, Object> buildSummary(AnalysisReport report) {
        Map<String, Object> summary = new LinkedHashMap<>();

        CompanyInfoVO company = report.getCompanyInfo();
        if (company != null) {
            summary.put("公司名称", company.getCompanyName());
            summary.put("员工总数", company.getWorkerCount() + company.getSupervisorCount());
            summary.put("工人数量", company.getWorkerCount());
            summary.put("主管数量", company.getSupervisorCount());
            summary.put("活跃款式数", company.getStyleCount());
            summary.put("床次总数", company.getBedCount());
            summary.put("本月产量", company.getMonthQuantity() + "件");
            summary.put("本月工资总额", company.getMonthAmount() + "元");
        }

        // Top 3 员工
        if (report.getTopPerformers() != null && !report.getTopPerformers().isEmpty()) {
            List<Map<String, Object>> topPerformers = new ArrayList<>();
            for (int i = 0; i < Math.min(3, report.getTopPerformers().size()); i++) {
                EmployeePerformance perf = report.getTopPerformers().get(i);
                Map<String, Object> perfMap = new LinkedHashMap<>();
                perfMap.put("姓名", perf.getWorkerName());
                perfMap.put("本月产量", perf.getMonthQuantity() + "件");
                perfMap.put("本月工资", perf.getMonthAmount() + "元");
                topPerformers.add(perfMap);
            }
            summary.put("Top3员工", topPerformers);
        }

        // 款式进度概览
        if (report.getStyleProgress() != null && !report.getStyleProgress().isEmpty()) {
            long completedStyles = report.getStyleProgress().stream()
                    .filter(s -> s.getProgressPercent().compareTo(BigDecimal.valueOf(100)) >= 0)
                    .count();
            summary.put("已完成款式", completedStyles + "/" + report.getStyleProgress().size());
        }

        // 工资分布概览
        if (report.getWageDistribution() != null && !report.getWageDistribution().isEmpty()) {
            Optional<WageDistribution> maxDist = report.getWageDistribution().stream()
                    .max(Comparator.comparingInt(WageDistribution::getCount));
            if (maxDist.isPresent() && maxDist.get().getCount() > 0) {
                summary.put("主要工资区间", maxDist.get().getRange());
            }
        }

        return summary;
    }
}

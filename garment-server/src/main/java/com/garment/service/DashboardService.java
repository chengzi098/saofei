package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.common.TenantContext;
import com.garment.entity.*;
import com.garment.mapper.*;
import com.garment.vo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserCompanyMapper userCompanyMapper;
    private final StyleMapper styleMapper;
    private final BedMapper bedMapper;
    private final BundleMapper bundleMapper;
    private final BundleProcessMapper bundleProcessMapper;
    private final WorkRecordMapper workRecordMapper;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;
    private final WageSettlementMapper wageSettlementMapper;
    private final WageAdjustmentMapper wageAdjustmentMapper;
    private final WageSettlementDetailMapper wageSettlementDetailMapper;

    public DashboardService(UserCompanyMapper userCompanyMapper, StyleMapper styleMapper,
                            BedMapper bedMapper, BundleMapper bundleMapper,
                            BundleProcessMapper bundleProcessMapper,
                            WorkRecordMapper workRecordMapper, UserMapper userMapper,
                            CompanyMapper companyMapper, WageSettlementMapper wageSettlementMapper,
                            WageAdjustmentMapper wageAdjustmentMapper,
                            WageSettlementDetailMapper wageSettlementDetailMapper) {
        this.userCompanyMapper = userCompanyMapper;
        this.styleMapper = styleMapper;
        this.bedMapper = bedMapper;
        this.bundleMapper = bundleMapper;
        this.bundleProcessMapper = bundleProcessMapper;
        this.workRecordMapper = workRecordMapper;
        this.userMapper = userMapper;
        this.companyMapper = companyMapper;
        this.wageSettlementMapper = wageSettlementMapper;
        this.wageAdjustmentMapper = wageAdjustmentMapper;
        this.wageSettlementDetailMapper = wageSettlementDetailMapper;
    }

    public DashboardVO getDashboard() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        // 查当前用户在本公司的角色
        LambdaQueryWrapper<UserCompany> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(UserCompany::getCompanyId, companyId)
                .eq(UserCompany::getUserId, userId)
                .eq(UserCompany::getStatus, 1);
        UserCompany userCompany = userCompanyMapper.selectOne(roleWrapper);
        String role = userCompany != null ? userCompany.getRole() : "worker";
        boolean isSupervisor = "supervisor".equals(role);

        // 查本公司所有 bundle（所有角色都看全公司数据）
        LambdaQueryWrapper<Bundle> bundleWrapper = new LambdaQueryWrapper<>();
        bundleWrapper.eq(Bundle::getCompanyId, companyId).eq(Bundle::getDeleted, 0);
        List<Bundle> allBundles = bundleMapper.selectList(bundleWrapper);

        if (allBundles.isEmpty()) {
            DashboardVO emptyVo = new DashboardVO();
            emptyVo.setStyles(new ArrayList<>());
            return emptyVo;
        }

        // 查所有关联的 style（排除已归档）
        Set<Long> styleIds = allBundles.stream().map(Bundle::getStyleId).collect(Collectors.toSet());
        Map<Long, Style> styleMap = styleMapper.selectBatchIds(styleIds).stream()
                .filter(s -> s.getArchived() == null || s.getArchived() == 0)
                .collect(Collectors.toMap(Style::getId, s -> s));

        // 过滤掉已归档款式关联的 bundle
        allBundles = allBundles.stream()
                .filter(b -> styleMap.containsKey(b.getStyleId()))
                .collect(Collectors.toList());
        if (allBundles.isEmpty()) {
            DashboardVO emptyVo = new DashboardVO();
            emptyVo.setStyles(new ArrayList<>());
            return emptyVo;
        }
        styleIds = allBundles.stream().map(Bundle::getStyleId).collect(Collectors.toSet());

        // 查所有关联的 bed
        Set<Long> bedIds = allBundles.stream().map(Bundle::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Bed> bedMap = new HashMap<>();
        if (!bedIds.isEmpty()) {
            bedMap = bedMapper.selectBatchIds(bedIds).stream()
                    .collect(Collectors.toMap(Bed::getId, b -> b));
        }

        // 查所有工序快照
        Set<Long> bundleIds = allBundles.stream().map(Bundle::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<BundleProcess> processWrapper = new LambdaQueryWrapper<>();
        processWrapper.in(BundleProcess::getBundleId, bundleIds);
        List<BundleProcess> allProcesses = bundleProcessMapper.selectList(processWrapper);
        Map<Long, List<BundleProcess>> processMap = allProcesses.stream()
                .collect(Collectors.groupingBy(BundleProcess::getBundleId));

        // 按 style 分组 bundle
        Map<Long, List<Bundle>> styleBundleMap = allBundles.stream()
                .collect(Collectors.groupingBy(Bundle::getStyleId));

        // 组装数据
        List<DashboardStyleVO> styleVOs = new ArrayList<>();
        for (Long styleId : styleIds) {
            Style style = styleMap.get(styleId);
            if (style == null) continue;

            DashboardStyleVO styleVO = new DashboardStyleVO();
            styleVO.setStyleId(styleId);
            styleVO.setStyleNo(style.getStyleNo());

            List<Bundle> styleBundles = styleBundleMap.getOrDefault(styleId, Collections.emptyList());
            int totalQty = styleBundles.stream().mapToInt(Bundle::getQuantity).sum();
            styleVO.setTotalQuantity(totalQty);

            // 按 bed 分组
            Map<Long, List<Bundle>> bedBundleMap = styleBundles.stream()
                    .filter(b -> b.getBedId() != null)
                    .collect(Collectors.groupingBy(Bundle::getBedId));

            // 包含没有 bed_id 的 bundle
            List<Bundle> noBedBundles = styleBundles.stream()
                    .filter(b -> b.getBedId() == null)
                    .collect(Collectors.toList());

            List<DashboardBedVO> bedVOs = new ArrayList<>();
            int totalBundleProgress = 0;
            int bundleCount = 0;

            for (Map.Entry<Long, List<Bundle>> entry : bedBundleMap.entrySet()) {
                Bed bed = bedMap.get(entry.getKey());
                if (bed == null) continue;

                DashboardBedVO bedVO = new DashboardBedVO();
                bedVO.setBedId(bed.getId());
                bedVO.setBedNo(bed.getBedNo());
                bedVO.setName(bed.getName());

                List<Bundle> bedBundles = entry.getValue();
                int bedQty = bedBundles.stream().mapToInt(Bundle::getQuantity).sum();
                bedVO.setQuantity(bedQty);
                bedVO.setTotalBundleCount(bedBundles.size());

                // 统计当前用户在该床下创建的扎货数
                long myBundlesInBed = bedBundles.stream().filter(b -> b.getCreatorId().equals(userId)).count();
                bedVO.setMyBundleCount((int) myBundlesInBed);
                bedVO.setMyBed(isSupervisor && myBundlesInBed > 0);

                // 计算该床下所有 bundle 的平均进度
                int bedProgress = 0;
                List<DashboardProcessVO> displayProcesses = new ArrayList<>();
                List<DashboardBundleVO> bundleVOs = new ArrayList<>();

                for (Bundle bundle : bedBundles) {
                    List<BundleProcess> processes = processMap.getOrDefault(bundle.getId(), Collections.emptyList());
                    int completed = (int) processes.stream().filter(p -> p.getStatus() == 2).count();
                    int bundleProgress = processes.size() > 0 ? Math.round(completed * 100f / processes.size()) : 0;
                    bedProgress += bundleProgress;
                    totalBundleProgress += bundleProgress;
                    bundleCount++;

                    // 用第一个有工序的 bundle 的工序作为该床的展示
                    if (displayProcesses.isEmpty() && !processes.isEmpty()) {
                        for (BundleProcess p : processes) {
                            DashboardProcessVO pv = new DashboardProcessVO();
                            pv.setBundleProcessId(p.getId());
                            pv.setProcessName(p.getProcessName());
                            pv.setStatus(p.getStatus());
                            pv.setStatusText(p.getStatus() == 2 ? "已完成" : p.getStatus() == 1 ? "进行中" : "未开始");
                            if (p.getStatus() == 2 && p.getOperatorName() != null) {
                                pv.setOperatorName(p.getOperatorName());
                                if (p.getCompleteTime() != null) {
                                    pv.setCompleteTime(p.getCompleteTime().toLocalDate().toString() + " " +
                                        p.getCompleteTime().toLocalTime().toString().substring(0, 5));
                                }
                            }
                            displayProcesses.add(pv);
                        }
                    }

                    // 组装 bundle 详情（主管和老板可见）
                    if (isSupervisor || "boss".equals(role)) {
                        DashboardBundleVO bundleVO = new DashboardBundleVO();
                        bundleVO.setBundleId(bundle.getId());
                        bundleVO.setBundleNo(bundle.getBundleNo());
                        bundleVO.setSize(bundle.getSize());
                        bundleVO.setQuantity(bundle.getQuantity());
                        bundleVO.setProgressPercent(bundleProgress);

                        List<DashboardProcessVO> bundleProcessVOs = new ArrayList<>();
                        for (BundleProcess p : processes) {
                            DashboardProcessVO pv = new DashboardProcessVO();
                            pv.setBundleProcessId(p.getId());
                            pv.setProcessName(p.getProcessName());
                            pv.setStatus(p.getStatus());
                            pv.setStatusText(p.getStatus() == 2 ? "已完成" : p.getStatus() == 1 ? "进行中" : "未开始");
                            if (p.getStatus() == 2 && p.getOperatorName() != null) {
                                pv.setOperatorName(p.getOperatorName());
                                if (p.getCompleteTime() != null) {
                                    pv.setCompleteTime(p.getCompleteTime().toLocalDate().toString() + " " +
                                        p.getCompleteTime().toLocalTime().toString().substring(0, 5));
                                }
                            }
                            bundleProcessVOs.add(pv);
                        }
                        bundleVO.setProcesses(bundleProcessVOs);
                        bundleVOs.add(bundleVO);
                    }
                }
                bedVO.setProgressPercent(bedBundles.size() > 0 ? Math.round(bedProgress * 1f / bedBundles.size()) : 0);
                bedVO.setProcesses(displayProcesses);
                bedVO.setBundles(bundleVOs);
                bedVOs.add(bedVO);
            }

            // 处理没有 bed_id 的 bundle
            for (Bundle bundle : noBedBundles) {
                List<BundleProcess> processes = processMap.getOrDefault(bundle.getId(), Collections.emptyList());
                int completed = (int) processes.stream().filter(p -> p.getStatus() == 2).count();
                int bundleProgress = processes.size() > 0 ? Math.round(completed * 100f / processes.size()) : 0;
                totalBundleProgress += bundleProgress;
                bundleCount++;
            }

            // 款式进度 = 所有 bundle 的平均进度
            styleVO.setProgressPercent(bundleCount > 0 ? Math.round(totalBundleProgress * 1f / bundleCount) : 0);
            styleVO.setBeds(bedVOs);
            styleVOs.add(styleVO);
        }

        // 排序：生产中（0<进度<100）在前，已完成在后，未开始最后
        styleVOs.sort((a, b) -> {
            int pa = a.getProgressPercent();
            int pb = b.getProgressPercent();
            if (pa > 0 && pa < 100 && (pb == 0 || pb == 100)) return -1;
            if (pb > 0 && pb < 100 && (pa == 0 || pa == 100)) return 1;
            return Integer.compare(pb, pa);
        });

        DashboardVO vo = new DashboardVO();
        vo.setStyles(styleVOs);
        return vo;
    }

    /**
     * 主管工作台统计
     */
    public SupervisorDashboardVO getSupervisorStats() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        SupervisorDashboardVO vo = new SupervisorDashboardVO();

        // 1. 我创建的扎货
        LambdaQueryWrapper<Bundle> myBundleWrapper = new LambdaQueryWrapper<>();
        myBundleWrapper.eq(Bundle::getCompanyId, companyId)
                .eq(Bundle::getCreatorId, userId)
                .eq(Bundle::getDeleted, 0);
        List<Bundle> myBundles = bundleMapper.selectList(myBundleWrapper);
        vo.setMyBundleCount(myBundles.size());
        vo.setMyTotalQuantity(myBundles.stream().mapToInt(Bundle::getQuantity).sum());

        // 2. 今日报工统计（本公司）
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LambdaQueryWrapper<WorkRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .ge(WorkRecord::getCreateTime, todayStart)
                .le(WorkRecord::getCreateTime, todayEnd);
        List<WorkRecord> todayRecords = workRecordMapper.selectList(todayWrapper);
        vo.setTodayReportCount(todayRecords.size());
        vo.setTodayAmount(todayRecords.stream()
                .map(WorkRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 3. 进行中的款式数（有未完成 bundle 的款式）
        LambdaQueryWrapper<Bundle> activeBundleWrapper = new LambdaQueryWrapper<>();
        activeBundleWrapper.eq(Bundle::getCompanyId, companyId)
                .eq(Bundle::getDeleted, 0)
                .ne(Bundle::getStatus, 2);
        List<Bundle> activeBundles = bundleMapper.selectList(activeBundleWrapper);
        long activeStyles = activeBundles.stream()
                .map(Bundle::getStyleId)
                .distinct()
                .count();
        vo.setActiveStyleCount((int) activeStyles);

        // 4. 退回记录数（近7天）
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<WorkRecord> returnWrapper = new LambdaQueryWrapper<>();
        returnWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 3)
                .ge(WorkRecord::getReturnTime, weekAgo);
        long returnCount = workRecordMapper.selectCount(returnWrapper);
        vo.setReturnCount((int) returnCount);

        // 5. 我的款式进度概览
        if (!myBundles.isEmpty()) {
            Set<Long> myStyleIds = myBundles.stream().map(Bundle::getStyleId).collect(Collectors.toSet());
            Map<Long, Style> myStyleMap = styleMapper.selectBatchIds(myStyleIds).stream()
                    .collect(Collectors.toMap(Style::getId, s -> s));

            // 查我所有扎货的工序
            Set<Long> myBundleIds = myBundles.stream().map(Bundle::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<BundleProcess> myProcessWrapper = new LambdaQueryWrapper<>();
            myProcessWrapper.in(BundleProcess::getBundleId, myBundleIds);
            List<BundleProcess> myProcesses = bundleProcessMapper.selectList(myProcessWrapper);
            Map<Long, List<BundleProcess>> myProcessMap = myProcesses.stream()
                    .collect(Collectors.groupingBy(BundleProcess::getBundleId));

            // 查每个款式的总扎货数
            LambdaQueryWrapper<Bundle> styleBundleWrapper = new LambdaQueryWrapper<>();
            styleBundleWrapper.eq(Bundle::getCompanyId, companyId).eq(Bundle::getDeleted, 0);
            List<Bundle> allBundles = bundleMapper.selectList(styleBundleWrapper);
            Map<Long, Long> styleBundleCountMap = allBundles.stream()
                    .collect(Collectors.groupingBy(Bundle::getStyleId, Collectors.counting()));

            // 按款式分组我的扎货
            Map<Long, List<Bundle>> myStyleBundleMap = myBundles.stream()
                    .collect(Collectors.groupingBy(Bundle::getStyleId));

            List<SupervisorDashboardVO.MyStyleProgress> styleProgressList = new ArrayList<>();
            for (Map.Entry<Long, List<Bundle>> entry : myStyleBundleMap.entrySet()) {
                Long styleId = entry.getKey();
                Style style = myStyleMap.get(styleId);
                if (style == null) continue;

                SupervisorDashboardVO.MyStyleProgress sp = new SupervisorDashboardVO.MyStyleProgress();
                sp.setStyleId(styleId);
                sp.setStyleNo(style.getStyleNo());
                sp.setMyBundleCount(entry.getValue().size());
                sp.setTotalBundleCount(styleBundleCountMap.getOrDefault(styleId, 0L).intValue());

                // 计算我的扎货平均进度
                int totalProgress = 0;
                int count = 0;
                for (Bundle b : entry.getValue()) {
                    List<BundleProcess> processes = myProcessMap.getOrDefault(b.getId(), Collections.emptyList());
                    if (!processes.isEmpty()) {
                        long completed = processes.stream().filter(p -> p.getStatus() == 2).count();
                        totalProgress += Math.round(completed * 100f / processes.size());
                        count++;
                    }
                }
                sp.setProgressPercent(count > 0 ? Math.round(totalProgress * 1f / count) : 0);
                styleProgressList.add(sp);
            }
            // 按进度排序，进行中的在前
            styleProgressList.sort((a, b) -> {
                if (a.getProgressPercent() > 0 && a.getProgressPercent() < 100
                        && (b.getProgressPercent() == 0 || b.getProgressPercent() == 100)) return -1;
                if (b.getProgressPercent() > 0 && b.getProgressPercent() < 100
                        && (a.getProgressPercent() == 0 || a.getProgressPercent() == 100)) return 1;
                return Integer.compare(b.getProgressPercent(), a.getProgressPercent());
            });
            vo.setMyStyles(styleProgressList);
        } else {
            vo.setMyStyles(new ArrayList<>());
        }

        // 6. 最近报工记录（本公司前5条）
        LambdaQueryWrapper<WorkRecord> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.eq(WorkRecord::getCompanyId, companyId)
                .orderByDesc(WorkRecord::getCreateTime)
                .last("LIMIT 5");
        List<WorkRecord> recentRecords = workRecordMapper.selectList(recentWrapper);

        // 批量查工人姓名
        Set<Long> workerIds = recentRecords.stream().map(WorkRecord::getWorkerId).collect(Collectors.toSet());
        Map<Long, String> workerNameMap = new HashMap<>();
        if (!workerIds.isEmpty()) {
            workerNameMap = userMapper.selectBatchIds(workerIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
        }

        // 批量查 bundleNo
        Set<Long> recentBundleIds = recentRecords.stream().map(WorkRecord::getBundleId).collect(Collectors.toSet());
        Map<Long, String> bundleNoMap = new HashMap<>();
        if (!recentBundleIds.isEmpty()) {
            bundleNoMap = bundleMapper.selectBatchIds(recentBundleIds).stream()
                    .collect(Collectors.toMap(Bundle::getId, Bundle::getBundleNo));
        }

        // 批量查工序名
        Set<Long> recentProcessIds = recentRecords.stream().map(WorkRecord::getBundleProcessId).collect(Collectors.toSet());
        Map<Long, String> processNameMap = new HashMap<>();
        if (!recentProcessIds.isEmpty()) {
            processNameMap = bundleProcessMapper.selectBatchIds(recentProcessIds).stream()
                    .collect(Collectors.toMap(BundleProcess::getId, BundleProcess::getProcessName));
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        List<SupervisorDashboardVO.RecentReport> recentReports = new ArrayList<>();
        for (WorkRecord wr : recentRecords) {
            SupervisorDashboardVO.RecentReport r = new SupervisorDashboardVO.RecentReport();
            r.setId(wr.getId());
            r.setWorkerName(workerNameMap.getOrDefault(wr.getWorkerId(), "未知"));
            r.setProcessName(processNameMap.getOrDefault(wr.getBundleProcessId(), "未知工序"));
            r.setBundleNo(bundleNoMap.getOrDefault(wr.getBundleId(), "未知"));
            r.setQuantity(wr.getQuantity() != null ? wr.getQuantity() : 0);
            r.setAmount(wr.getAmount());
            r.setCreateTime(wr.getCreateTime() != null ? wr.getCreateTime().format(dtf) : "");
            recentReports.add(r);
        }
        vo.setRecentReports(recentReports);

        return vo;
    }

    /**
     * 老板工作台统计
     */
    public BossDashboardVO getBossStats() {
        Long companyId = TenantContext.getCompanyId();
        BossDashboardVO vo = new BossDashboardVO();

        // 查询公司到期时间
        Company company = companyMapper.selectById(companyId);
        if (company != null) {
            vo.setExpireTime(company.getExpireTime());
        }

        // 今日时间范围
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 本月时间范围
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);

        // 1. 今日报工统计（已通过）
        LambdaQueryWrapper<WorkRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .ge(WorkRecord::getCreateTime, todayStart)
                .le(WorkRecord::getCreateTime, todayEnd);
        List<WorkRecord> todayRecords = workRecordMapper.selectList(todayWrapper);
        vo.setTodayReportCount(todayRecords.size());
        vo.setTodayAmount(todayRecords.stream()
                .map(WorkRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 2. 本月报工金额（已通过）
        LambdaQueryWrapper<WorkRecord> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .ge(WorkRecord::getCreateTime, monthStart)
                .le(WorkRecord::getCreateTime, todayEnd);
        List<WorkRecord> monthRecords = workRecordMapper.selectList(monthWrapper);
        vo.setMonthAmount(monthRecords.stream()
                .map(WorkRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 2.5 本周报工金额（已通过，本周一到今天）
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDateTime weekStartTime = LocalDateTime.of(weekStart, LocalTime.MIN);
        LambdaQueryWrapper<WorkRecord> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .ge(WorkRecord::getCreateTime, weekStartTime)
                .le(WorkRecord::getCreateTime, todayEnd);
        List<WorkRecord> weekRecords = workRecordMapper.selectList(weekWrapper);
        vo.setWeekAmount(weekRecords.stream()
                .map(WorkRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 2.6 本月工资（工资结算单）
        String currentMonthStart = today.withDayOfMonth(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentMonthEnd = today.withDayOfMonth(today.lengthOfMonth()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LambdaQueryWrapper<WageSettlement> wageWrapper = new LambdaQueryWrapper<>();
        wageWrapper.eq(WageSettlement::getCompanyId, companyId)
                .ge(WageSettlement::getStartDate, currentMonthStart)
                .le(WageSettlement::getEndDate, currentMonthEnd);
        WageSettlement wageSettlement = wageSettlementMapper.selectOne(wageWrapper);
        vo.setMonthWage(wageSettlement != null && wageSettlement.getTotalAmount() != null
                ? wageSettlement.getTotalAmount() : BigDecimal.ZERO);

        // 3. 今日活跃员工数（今日报工的不同 workerId 去重）
        long activeWorkers = todayRecords.stream()
                .map(WorkRecord::getWorkerId)
                .distinct()
                .count();
        vo.setActiveEmployeeCount((int) activeWorkers);

        // 4. 在职员工总数（不含老板）
        LambdaQueryWrapper<UserCompany> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(UserCompany::getCompanyId, companyId)
                .eq(UserCompany::getStatus, 1)
                .ne(UserCompany::getRole, "boss");
        long totalEmployees = userCompanyMapper.selectCount(empWrapper);
        vo.setTotalEmployeeCount((int) totalEmployees);

        // 5. 进行中款式数（有未完成扎货的款式）
        LambdaQueryWrapper<Bundle> activeBundleWrapper = new LambdaQueryWrapper<>();
        activeBundleWrapper.eq(Bundle::getCompanyId, companyId)
                .eq(Bundle::getDeleted, 0)
                .ne(Bundle::getStatus, 2);
        List<Bundle> activeBundles = bundleMapper.selectList(activeBundleWrapper);
        long activeStyles = activeBundles.stream()
                .map(Bundle::getStyleId)
                .distinct()
                .count();
        vo.setActiveStyleCount((int) activeStyles);

        // 6. 近7天退回记录数
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<WorkRecord> returnWrapper = new LambdaQueryWrapper<>();
        returnWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 3)
                .ge(WorkRecord::getReturnTime, weekAgo);
        long returnCount = workRecordMapper.selectCount(returnWrapper);
        vo.setReturnCount((int) returnCount);

        // 7. 全公司款式进度概览
        LambdaQueryWrapper<Bundle> allBundleWrapper = new LambdaQueryWrapper<>();
        allBundleWrapper.eq(Bundle::getCompanyId, companyId).eq(Bundle::getDeleted, 0);
        List<Bundle> allBundles = bundleMapper.selectList(allBundleWrapper);

        if (!allBundles.isEmpty()) {
            // 查所有工序快照
            Set<Long> allBundleIds = allBundles.stream().map(Bundle::getId).collect(Collectors.toSet());
            LambdaQueryWrapper<BundleProcess> procWrapper = new LambdaQueryWrapper<>();
            procWrapper.in(BundleProcess::getBundleId, allBundleIds);
            List<BundleProcess> allProcesses = bundleProcessMapper.selectList(procWrapper);
            Map<Long, List<BundleProcess>> processMap = allProcesses.stream()
                    .collect(Collectors.groupingBy(BundleProcess::getBundleId));

            // 按款式分组
            Map<Long, List<Bundle>> styleBundleMap = allBundles.stream()
                    .collect(Collectors.groupingBy(Bundle::getStyleId));
            Set<Long> styleIds = styleBundleMap.keySet();
            Map<Long, Style> styleMap = styleMapper.selectBatchIds(styleIds).stream()
                    .filter(s -> s.getArchived() == null || s.getArchived() == 0)
                    .collect(Collectors.toMap(Style::getId, s -> s));

            List<BossDashboardVO.StyleProgress> styleProgressList = new ArrayList<>();
            for (Map.Entry<Long, List<Bundle>> entry : styleBundleMap.entrySet()) {
                Long styleId = entry.getKey();
                Style style = styleMap.get(styleId);
                if (style == null) continue;

                BossDashboardVO.StyleProgress sp = new BossDashboardVO.StyleProgress();
                sp.setStyleId(styleId);
                sp.setStyleNo(style.getStyleNo());
                sp.setTotalBundleCount(entry.getValue().size());

                // 计算该款式所有 bundle 的平均进度
                int totalProgress = 0;
                int count = 0;
                for (Bundle b : entry.getValue()) {
                    List<BundleProcess> processes = processMap.getOrDefault(b.getId(), Collections.emptyList());
                    if (!processes.isEmpty()) {
                        long completed = processes.stream().filter(p -> p.getStatus() == 2).count();
                        totalProgress += Math.round(completed * 100f / processes.size());
                        count++;
                    }
                }
                sp.setProgressPercent(count > 0 ? Math.round(totalProgress * 1f / count) : 0);
                styleProgressList.add(sp);
            }
            // 排序：进行中在前，已完成在后
            styleProgressList.sort((a, b) -> {
                if (a.getProgressPercent() > 0 && a.getProgressPercent() < 100
                        && (b.getProgressPercent() == 0 || b.getProgressPercent() == 100)) return -1;
                if (b.getProgressPercent() > 0 && b.getProgressPercent() < 100
                        && (a.getProgressPercent() == 0 || a.getProgressPercent() == 100)) return 1;
                return Integer.compare(b.getProgressPercent(), a.getProgressPercent());
            });
            vo.setStyles(styleProgressList);
        } else {
            vo.setStyles(new ArrayList<>());
        }

        return vo;
    }

    /**
     * 工人本月收入统计
     * 计算方式：本月已通过报工工资 + 本月已审批工资调整（加钱为正、扣钱为负）
     * 被退回的报工不纳入总收入
     */
    public Map<String, BigDecimal> getWorkerMonthlyIncome() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        // 本月时间范围
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        LocalDateTime monthEnd = LocalDateTime.of(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()), LocalTime.MAX);

        // 1. 本月已通过报工工资（status=1）
        LambdaQueryWrapper<WorkRecord> workWrapper = new LambdaQueryWrapper<>();
        workWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getWorkerId, userId)
                .eq(WorkRecord::getStatus, 1)
                .ge(WorkRecord::getCreateTime, monthStart)
                .le(WorkRecord::getCreateTime, monthEnd);
        List<WorkRecord> passedRecords = workRecordMapper.selectList(workWrapper);
        BigDecimal workAmount = passedRecords.stream()
                .map(WorkRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. 本月已退回报工金额（仅供参考，不纳入总收入）
        LambdaQueryWrapper<WorkRecord> returnWrapper = new LambdaQueryWrapper<>();
        returnWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getWorkerId, userId)
                .eq(WorkRecord::getStatus, 3)
                .ge(WorkRecord::getCreateTime, monthStart)
                .le(WorkRecord::getCreateTime, monthEnd);
        List<WorkRecord> returnedRecords = workRecordMapper.selectList(returnWrapper);
        BigDecimal returnedAmount = returnedRecords.stream()
                .map(WorkRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 本月已审批工资调整（status=1）
        LambdaQueryWrapper<WageAdjustment> adjWrapper = new LambdaQueryWrapper<>();
        adjWrapper.eq(WageAdjustment::getCompanyId, companyId)
                .eq(WageAdjustment::getWorkerId, userId)
                .eq(WageAdjustment::getStatus, 1)
                .ge(WageAdjustment::getCreateTime, monthStart)
                .le(WageAdjustment::getCreateTime, monthEnd);
        List<WageAdjustment> adjustments = wageAdjustmentMapper.selectList(adjWrapper);
        BigDecimal adjustmentAmount = BigDecimal.ZERO;
        for (WageAdjustment adj : adjustments) {
            if (adj.getType() == 1) {
                // 加钱
                adjustmentAmount = adjustmentAmount.add(adj.getAmount() != null ? adj.getAmount() : BigDecimal.ZERO);
            } else if (adj.getType() == 2) {
                // 扣钱
                adjustmentAmount = adjustmentAmount.subtract(adj.getAmount() != null ? adj.getAmount() : BigDecimal.ZERO);
            }
        }

        // 总收入 = 报工工资 + 工资调整
        BigDecimal totalAmount = workAmount.add(adjustmentAmount);

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("workAmount", workAmount);
        result.put("adjustmentAmount", adjustmentAmount);
        result.put("totalAmount", totalAmount);
        result.put("returnedAmount", returnedAmount);
        return result;
    }

    /**
     * 工人通知计数（红点提醒）
     * 只统计上次查看之后的新记录
     */
    public Map<String, Integer> getWorkerNotifications() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        // 查用户的最后查看时间
        User user = userMapper.selectById(userId);
        LocalDateTime lastViewRecords = user != null ? user.getLastViewRecordsAt() : null;
        LocalDateTime lastViewAdjustment = user != null ? user.getLastViewAdjustmentAt() : null;
        LocalDateTime lastViewSettlement = user != null ? user.getLastViewSettlementAt() : null;

        // 1. 被退回的报工记录（status=3，且在上次查看之后）
        LambdaQueryWrapper<WorkRecord> returnWrapper = new LambdaQueryWrapper<>();
        returnWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getWorkerId, userId)
                .eq(WorkRecord::getStatus, 3);
        if (lastViewRecords != null) {
            returnWrapper.ge(WorkRecord::getCreateTime, lastViewRecords);
        }
        long returnedCount = workRecordMapper.selectCount(returnWrapper);

        // 2. 未删除的工资调整（status!=2，且在上次查看之后）
        LambdaQueryWrapper<WageAdjustment> adjWrapper = new LambdaQueryWrapper<>();
        adjWrapper.eq(WageAdjustment::getCompanyId, companyId)
                .eq(WageAdjustment::getWorkerId, userId)
                .ne(WageAdjustment::getStatus, 2);
        if (lastViewAdjustment != null) {
            adjWrapper.ge(WageAdjustment::getCreateTime, lastViewAdjustment);
        }
        long adjustmentCount = wageAdjustmentMapper.selectCount(adjWrapper);

        // 3. 待签名的工资结算单（detail.status=1，且在上次查看之后）
        LambdaQueryWrapper<WageSettlementDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(WageSettlementDetail::getCompanyId, companyId)
                .eq(WageSettlementDetail::getWorkerId, userId)
                .eq(WageSettlementDetail::getStatus, 1);
        if (lastViewSettlement != null) {
            detailWrapper.ge(WageSettlementDetail::getCreateTime, lastViewSettlement);
        }
        List<WageSettlementDetail> pendingDetails = wageSettlementDetailMapper.selectList(detailWrapper);

        int settlementCount = 0;
        if (!pendingDetails.isEmpty()) {
            Set<Long> settlementIds = pendingDetails.stream()
                    .map(WageSettlementDetail::getSettlementId)
                    .collect(Collectors.toSet());
            LambdaQueryWrapper<WageSettlement> sw = new LambdaQueryWrapper<>();
            sw.in(WageSettlement::getId, settlementIds)
                    .eq(WageSettlement::getCompanyId, companyId);
            List<WageSettlement> settlements = wageSettlementMapper.selectList(sw);
            long signingCount = settlements.stream()
                    .filter(s -> s.getStatus() != null && s.getStatus() == 2)
                    .count();
            settlementCount = (int) signingCount;
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("returnedCount", (int) returnedCount);
        result.put("adjustmentCount", (int) adjustmentCount);
        result.put("settlementCount", settlementCount);
        return result;
    }

    /**
     * 老板通知计数（红点提醒）
     * 三种通知：退回、新报工、工资调整（待审批）
     */
    public Map<String, Integer> getBossNotifications() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        User user = userMapper.selectById(userId);
        LocalDateTime lastViewReturn = user != null ? user.getLastViewBossReturnAt() : null;
        LocalDateTime lastViewReview = user != null ? user.getLastViewBossReviewAt() : null;
        LocalDateTime lastViewAdj = user != null ? user.getLastViewBossAdjAt() : null;

        // 1. 退回通知：本公司所有被退回的报工记录（status=3）
        LambdaQueryWrapper<WorkRecord> returnWrapper = new LambdaQueryWrapper<>();
        returnWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 3);
        if (lastViewReturn != null) {
            returnWrapper.ge(WorkRecord::getReturnTime, lastViewReturn);
        }
        Long returnCnt = workRecordMapper.selectCount(returnWrapper);

        // 2. 新报工通知：本公司新提交的报工记录（status=1）
        LambdaQueryWrapper<WorkRecord> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1);
        if (lastViewReview != null) {
            reviewWrapper.ge(WorkRecord::getCreateTime, lastViewReview);
        }
        Long reviewCnt = workRecordMapper.selectCount(reviewWrapper);

        // 3. 工资调整通知：本公司所有待审批调整记录（不限操作人）
        LambdaQueryWrapper<WageAdjustment> adjWrapper = new LambdaQueryWrapper<>();
        adjWrapper.eq(WageAdjustment::getCompanyId, companyId)
                .eq(WageAdjustment::getStatus, 0);
        if (lastViewAdj != null) {
            adjWrapper.ge(WageAdjustment::getCreateTime, lastViewAdj);
        }
        Long adjCnt = wageAdjustmentMapper.selectCount(adjWrapper);

        Map<String, Integer> result = new HashMap<>();
        result.put("returnedCount", returnCnt != null ? returnCnt.intValue() : 0);
        result.put("reviewCount", reviewCnt != null ? reviewCnt.intValue() : 0);
        result.put("adjustmentCount", adjCnt != null ? adjCnt.intValue() : 0);
        return result;
    }

    /**
     * 标记老板通知已查看
     * @param type 类型：return / review / adjustment / all
     */
    public void markBossNotificationsViewed(String type) {
        Long userId = TenantContext.getUserId();
        User user = new User();
        user.setId(userId);
        LocalDateTime now = LocalDateTime.now();

        switch (type) {
            case "return":
                user.setLastViewBossReturnAt(now);
                break;
            case "review":
                user.setLastViewBossReviewAt(now);
                break;
            case "adjustment":
                user.setLastViewBossAdjAt(now);
                break;
            case "all":
                user.setLastViewBossReturnAt(now);
                user.setLastViewBossReviewAt(now);
                user.setLastViewBossAdjAt(now);
                break;
            default:
                break;
        }
        userMapper.updateById(user);
    }

    /**
     * 主管通知计数（红点提醒）
     * 三种通知：退回、报工审批（待审核）、工资调整（待审批）
     */
    public Map<String, Integer> getSupervisorNotifications() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        User user = userMapper.selectById(userId);
        LocalDateTime lastViewReturn = user != null ? user.getLastViewReturnAt() : null;
        LocalDateTime lastViewReview = user != null ? user.getLastViewReviewAt() : null;
        LocalDateTime lastViewAdj = user != null ? user.getLastViewSupervisorAdjAt() : null;

        // 1. 退回通知：本公司所有被退回的报工记录（status=3）
        LambdaQueryWrapper<WorkRecord> returnWrapper = new LambdaQueryWrapper<>();
        returnWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 3);
        if (lastViewReturn != null) {
            returnWrapper.ge(WorkRecord::getReturnTime, lastViewReturn);
        }
        Long returnCnt = workRecordMapper.selectCount(returnWrapper);
        int returnedCount = returnCnt != null ? returnCnt.intValue() : 0;

        // 2. 新报工通知：本公司新提交的报工记录（status=1，主管可查看后决定是否退回）
        LambdaQueryWrapper<WorkRecord> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1);
        if (lastViewReview != null) {
            reviewWrapper.ge(WorkRecord::getCreateTime, lastViewReview);
        }
        Long reviewCnt = workRecordMapper.selectCount(reviewWrapper);

        // 3. 工资调整通知：主管创建的待审批调整记录
        LambdaQueryWrapper<WageAdjustment> adjWrapper = new LambdaQueryWrapper<>();
        adjWrapper.eq(WageAdjustment::getCompanyId, companyId)
                .eq(WageAdjustment::getOperatorId, userId)
                .eq(WageAdjustment::getStatus, 0);
        if (lastViewAdj != null) {
            adjWrapper.ge(WageAdjustment::getCreateTime, lastViewAdj);
        }
        Long adjCnt = wageAdjustmentMapper.selectCount(adjWrapper);

        Map<String, Integer> result = new HashMap<>();
        result.put("returnedCount", returnedCount);
        result.put("reviewCount", reviewCnt != null ? reviewCnt.intValue() : 0);
        result.put("adjustmentCount", adjCnt != null ? adjCnt.intValue() : 0);
        return result;
    }

    /**
     * 标记主管通知已查看
     * @param type 类型：return / review / adjustment / all
     */
    public void markSupervisorNotificationsViewed(String type) {
        Long userId = TenantContext.getUserId();
        User user = new User();
        user.setId(userId);
        LocalDateTime now = LocalDateTime.now();

        switch (type) {
            case "return":
                user.setLastViewReturnAt(now);
                break;
            case "review":
                user.setLastViewReviewAt(now);
                break;
            case "adjustment":
                user.setLastViewSupervisorAdjAt(now);
                break;
            case "all":
                user.setLastViewReturnAt(now);
                user.setLastViewReviewAt(now);
                user.setLastViewSupervisorAdjAt(now);
                break;
            default:
                break;
        }
        userMapper.updateById(user);
    }

    /**
     * 超级管理员全局统计
     */
    public AdminDashboardVO getAdminStats() {
        AdminDashboardVO vo = new AdminDashboardVO();

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime thirtyDaysLater = LocalDateTime.now().plusDays(30);

        // 1. 公司统计（不含逻辑删除的）
        List<Company> allCompanies = companyMapper.selectList(null);
        int totalCompany = allCompanies.size();
        int activeCompany = 0;
        int disabledCompany = 0;
        for (Company c : allCompanies) {
            if (c.getStatus() != null && c.getStatus() == 1) {
                activeCompany++;
            } else {
                disabledCompany++;
            }
        }
        vo.setTotalCompanyCount(totalCompany);
        vo.setActiveCompanyCount(activeCompany);
        vo.setDisabledCompanyCount(disabledCompany);

        // 2. 在职员工总数（所有公司，不含老板）
        LambdaQueryWrapper<UserCompany> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(UserCompany::getStatus, 1)
                .ne(UserCompany::getRole, "boss");
        long totalEmployee = userCompanyMapper.selectCount(empWrapper);
        vo.setTotalEmployeeCount((int) totalEmployee);

        // 3. 总款式数
        LambdaQueryWrapper<Style> styleWrapper = new LambdaQueryWrapper<>();
        styleWrapper.eq(Style::getDeleted, 0);
        long totalStyle = styleMapper.selectCount(styleWrapper);
        vo.setTotalStyleCount((int) totalStyle);

        // 4. 今日全局报工统计
        LambdaQueryWrapper<WorkRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(WorkRecord::getStatus, 1)
                .ge(WorkRecord::getCreateTime, todayStart)
                .le(WorkRecord::getCreateTime, todayEnd);
        List<WorkRecord> todayRecords = workRecordMapper.selectList(todayWrapper);
        vo.setTodayReportCount(todayRecords.size());
        vo.setTodayAmount(todayRecords.stream()
                .map(WorkRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 5. 本月全局报工产值
        LambdaQueryWrapper<WorkRecord> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.eq(WorkRecord::getStatus, 1)
                .ge(WorkRecord::getCreateTime, monthStart)
                .le(WorkRecord::getCreateTime, todayEnd);
        List<WorkRecord> monthRecords = workRecordMapper.selectList(monthWrapper);
        vo.setMonthAmount(monthRecords.stream()
                .map(WorkRecord::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 6. 近30天新增公司数
        LambdaQueryWrapper<Company> newCompanyWrapper = new LambdaQueryWrapper<>();
        newCompanyWrapper.ge(Company::getCreateTime, thirtyDaysAgo);
        long newCompanyCount = companyMapper.selectCount(newCompanyWrapper);
        vo.setNewCompanyCount((int) newCompanyCount);

        // 7. 即将到期公司（30天内，按到期时间升序）
        List<Company> expiringList = allCompanies.stream()
                .filter(c -> c.getExpireTime() != null)
                .filter(c -> {
                    LocalDateTime expire = c.getExpireTime();
                    return expire.isAfter(LocalDateTime.now()) && expire.isBefore(thirtyDaysLater);
                })
                .sorted(Comparator.comparing(Company::getExpireTime))
                .collect(Collectors.toList());

        List<AdminDashboardVO.ExpiringCompany> expiringCompanies = new ArrayList<>();
        for (Company c : expiringList) {
            AdminDashboardVO.ExpiringCompany ec = new AdminDashboardVO.ExpiringCompany();
            ec.setId(c.getId());
            ec.setName(c.getName());
            ec.setExpireTime(c.getExpireTime());
            long days = java.time.Duration.between(LocalDateTime.now(), c.getExpireTime()).toDays();
            ec.setRemainingDays((int) Math.max(0, days));
            expiringCompanies.add(ec);
        }
        vo.setExpiringCompanies(expiringCompanies);

        // 8. 总扎货数
        LambdaQueryWrapper<Bundle> bundleWrapper = new LambdaQueryWrapper<>();
        bundleWrapper.eq(Bundle::getDeleted, 0);
        long totalBundle = bundleMapper.selectCount(bundleWrapper);
        vo.setTotalBundleCount((int) totalBundle);

        // 9. 本月总报工条数（已通过）
        vo.setTotalWorkRecordCount(monthRecords.size());

        // 10. 近6个月产值趋势
        List<AdminDashboardVO.MonthlyTrend> monthTrends = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter ymFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDateTime mStart = LocalDateTime.of(month.withDayOfMonth(1), LocalTime.MIN);
            LocalDateTime mEnd = LocalDateTime.of(month.withDayOfMonth(month.lengthOfMonth()), LocalTime.MAX);

            LambdaQueryWrapper<WorkRecord> trendWrapper = new LambdaQueryWrapper<>();
            trendWrapper.eq(WorkRecord::getStatus, 1)
                    .ge(WorkRecord::getCreateTime, mStart)
                    .le(WorkRecord::getCreateTime, mEnd);
            List<WorkRecord> mRecords = workRecordMapper.selectList(trendWrapper);
            BigDecimal mAmount = mRecords.stream()
                    .map(WorkRecord::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            AdminDashboardVO.MonthlyTrend trend = new AdminDashboardVO.MonthlyTrend();
            trend.setYearMonth(month.format(ymFmt));
            trend.setAmount(mAmount);
            monthTrends.add(trend);
        }
        vo.setMonthTrends(monthTrends);

        // 11. 最近活跃公司（按本月产值排序Top5）
        // 先按公司分组统计本月产值
        Map<Long, BigDecimal> companyAmountMap = new HashMap<>();
        for (WorkRecord wr : monthRecords) {
            Long cid = wr.getCompanyId();
            if (cid == null) continue;
            BigDecimal amt = wr.getAmount() != null ? wr.getAmount() : BigDecimal.ZERO;
            companyAmountMap.merge(cid, amt, BigDecimal::add);
        }

        // 查各公司员工数
        LambdaQueryWrapper<UserCompany> allEmpWrapper = new LambdaQueryWrapper<>();
        allEmpWrapper.eq(UserCompany::getStatus, 1).ne(UserCompany::getRole, "boss");
        List<UserCompany> allUserCompanies = userCompanyMapper.selectList(allEmpWrapper);
        Map<Long, Long> companyEmployeeMap = allUserCompanies.stream()
                .collect(Collectors.groupingBy(UserCompany::getCompanyId, Collectors.counting()));

        // 查各公司款式数
        LambdaQueryWrapper<Style> allStyleWrapper = new LambdaQueryWrapper<>();
        allStyleWrapper.eq(Style::getDeleted, 0);
        List<Style> allStyles = styleMapper.selectList(allStyleWrapper);
        Map<Long, Long> companyStyleMap = allStyles.stream()
                .collect(Collectors.groupingBy(Style::getCompanyId, Collectors.counting()));

        // 组装活跃公司列表并排序
        List<AdminDashboardVO.ActiveCompany> activeCompanies = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : companyAmountMap.entrySet()) {
            Long cid = entry.getKey();
            Company company = allCompanies.stream()
                    .filter(c -> c.getId().equals(cid))
                    .findFirst()
                    .orElse(null);
            if (company == null) continue;

            AdminDashboardVO.ActiveCompany ac = new AdminDashboardVO.ActiveCompany();
            ac.setId(cid);
            ac.setName(company.getName());
            ac.setMonthAmount(entry.getValue());
            ac.setEmployeeCount(companyEmployeeMap.getOrDefault(cid, 0L).intValue());
            ac.setStyleCount(companyStyleMap.getOrDefault(cid, 0L).intValue());
            activeCompanies.add(ac);
        }

        // 按产值降序取Top5
        activeCompanies.sort((a, b) -> b.getMonthAmount().compareTo(a.getMonthAmount()));
        if (activeCompanies.size() > 5) {
            activeCompanies = activeCompanies.subList(0, 5);
        }
        vo.setActiveCompanies(activeCompanies);

        // 12. 各公司员工分布（按员工数排序Top10）
        Map<Long, BigDecimal> companyAmountForStat = companyAmountMap;
        List<AdminDashboardVO.CompanyEmployeeStat> companyEmployeeStats = new ArrayList<>();
        for (Company c : allCompanies) {
            if (c.getStatus() == null || c.getStatus() != 1) continue;
            AdminDashboardVO.CompanyEmployeeStat stat = new AdminDashboardVO.CompanyEmployeeStat();
            stat.setCompanyId(c.getId());
            stat.setCompanyName(c.getName());
            stat.setEmployeeCount(companyEmployeeMap.getOrDefault(c.getId(), 0L).intValue());
            stat.setMonthAmount(companyAmountForStat.getOrDefault(c.getId(), BigDecimal.ZERO));
            companyEmployeeStats.add(stat);
        }
        // 按员工数降序取Top10
        companyEmployeeStats.sort((a, b) -> Integer.compare(b.getEmployeeCount(), a.getEmployeeCount()));
        if (companyEmployeeStats.size() > 10) {
            companyEmployeeStats = companyEmployeeStats.subList(0, 10);
        }
        vo.setCompanyEmployeeStats(companyEmployeeStats);

        return vo;
    }

    /**
     * 标记通知已查看（更新最后查看时间）
     * @param type 类型：records / adjustment / settlement
     */
    public void markNotificationsViewed(String type) {
        Long userId = TenantContext.getUserId();
        User user = new User();
        user.setId(userId);
        LocalDateTime now = LocalDateTime.now();

        switch (type) {
            case "records":
                user.setLastViewRecordsAt(now);
                break;
            case "adjustment":
                user.setLastViewAdjustmentAt(now);
                break;
            case "settlement":
                user.setLastViewSettlementAt(now);
                break;
            case "all":
                user.setLastViewRecordsAt(now);
                user.setLastViewAdjustmentAt(now);
                user.setLastViewSettlementAt(now);
                break;
            default:
                break;
        }
        userMapper.updateById(user);
    }
}

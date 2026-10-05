package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.common.TenantContext;
import com.garment.entity.*;
import com.garment.mapper.*;
import com.garment.vo.WageSettlementDetailVO;
import com.garment.vo.WageSettlementVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WageSettlementService {

    private final WageSettlementMapper settlementMapper;
    private final WageSettlementDetailMapper detailMapper;
    private final WorkRecordMapper workRecordMapper;
    private final UserMapper userMapper;
    private final BundleMapper bundleMapper;
    private final StyleMapper styleMapper;
    private final BedMapper bedMapper;
    private final WageAdjustmentMapper adjustmentMapper;
    private final BundleProcessMapper bundleProcessMapper;
    private final CompanyMapper companyMapper;
    private final UserCompanyMapper userCompanyMapper;

    public WageSettlementService(WageSettlementMapper settlementMapper,
                                  WageSettlementDetailMapper detailMapper,
                                  WorkRecordMapper workRecordMapper,
                                  UserMapper userMapper,
                                  BundleMapper bundleMapper,
                                  StyleMapper styleMapper,
                                  BedMapper bedMapper,
                                  WageAdjustmentMapper adjustmentMapper,
                                  BundleProcessMapper bundleProcessMapper,
                                  CompanyMapper companyMapper,
                                  UserCompanyMapper userCompanyMapper) {
        this.settlementMapper = settlementMapper;
        this.detailMapper = detailMapper;
        this.workRecordMapper = workRecordMapper;
        this.userMapper = userMapper;
        this.bundleMapper = bundleMapper;
        this.styleMapper = styleMapper;
        this.bedMapper = bedMapper;
        this.adjustmentMapper = adjustmentMapper;
        this.bundleProcessMapper = bundleProcessMapper;
        this.companyMapper = companyMapper;
        this.userCompanyMapper = userCompanyMapper;
    }

    /**
     * 获取上次结算信息（用于前端自动填充下次起始日期）
     */
    public Map<String, Object> getLastSettlement() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 获取公司创建时间
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new RuntimeException("公司不存在");
        }

        LambdaQueryWrapper<WageSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WageSettlement::getCompanyId, companyId)
                .orderByDesc(WageSettlement::getCreateTime)
                .last("LIMIT 1");
        WageSettlement last = settlementMapper.selectOne(wrapper);

        Map<String, Object> result = new HashMap<>();
        // 公司创建日期（格式化为yyyy-MM-dd）
        String companyCreateDate = company.getCreateTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        result.put("companyCreateDate", companyCreateDate);

        if (last != null) {
            result.put("hasSettlement", true);
            result.put("lastEndDate", last.getEndDate());
            // 下次起始日期 = 上次结束日期 + 1天
            LocalDate nextStart = LocalDate.parse(last.getEndDate()).plusDays(1);
            result.put("nextStartDate", nextStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            result.put("hasSettlement", false);
            // 没有结算记录时，起始日期为公司创建日期
            result.put("nextStartDate", companyCreateDate);
        }
        return result;
    }

    /**
     * 预览全员明细（不保存到数据库）
     */
    public Map<String, Object> previewSettlement(String startDate, String endDate) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 查询时间段内所有已通过的报工记录（status=1，未结算 settlement_id IS NULL）
        LambdaQueryWrapper<WorkRecord> wrWrapper = new LambdaQueryWrapper<>();
        wrWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .isNull(WorkRecord::getSettlementId)
                .ge(WorkRecord::getCreateTime, startDate + "T00:00:00")
                .lt(WorkRecord::getCreateTime, endDate + "T23:59:59");
        List<WorkRecord> allRecords = workRecordMapper.selectList(wrWrapper);

        // 检查是否有"待定价"记录（单价为0）
        List<WorkRecord> pendingPriceRecords = allRecords.stream()
                .filter(r -> r.getPrice() == null || r.getPrice().compareTo(BigDecimal.ZERO) == 0)
                .collect(Collectors.toList());
        boolean hasPendingPrice = !pendingPriceRecords.isEmpty();
        int pendingCount = pendingPriceRecords.size();

        // 按工人分组汇总
        Map<Long, List<WorkRecord>> grouped = allRecords.stream()
                .collect(Collectors.groupingBy(WorkRecord::getWorkerId));

        // 查询时间段内所有已审批的工资调整（status=1，未结算 settlement_id IS NULL）
        List<WageAdjustment> adjustments = adjustmentMapper.selectList(
                new LambdaQueryWrapper<WageAdjustment>()
                        .eq(WageAdjustment::getCompanyId, companyId)
                        .eq(WageAdjustment::getStatus, 1)
                        .isNull(WageAdjustment::getSettlementId)
                        .ge(WageAdjustment::getCreateTime, startDate + "T00:00:00")
                        .lt(WageAdjustment::getCreateTime, endDate + "T23:59:59")
        );
        // 按工人分组汇总调整金额（加钱为正，扣钱为负）
        Map<Long, BigDecimal> adjustmentMap = new HashMap<>();
        for (WageAdjustment adj : adjustments) {
            BigDecimal adjAmount = adj.getType() == 1 ? adj.getAmount() : adj.getAmount().negate();
            adjustmentMap.merge(adj.getWorkerId(), adjAmount, BigDecimal::add);
        }

        // 查询公司所有活跃员工（包括没有报工记录的）
        List<UserCompany> allUcList = userCompanyMapper.selectList(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getStatus, 1)
        );
        // 排除老板
        Set<Long> bossIds = allUcList.stream()
                .filter(uc -> "boss".equals(uc.getRole()))
                .map(UserCompany::getUserId)
                .collect(Collectors.toSet());
        allUcList.removeIf(uc -> "boss".equals(uc.getRole()));

        Set<Long> allWorkerIds = allUcList.stream().map(UserCompany::getUserId).collect(Collectors.toSet());
        // 合并有报工记录的工人ID（排除老板）
        for (Long wid : grouped.keySet()) {
            if (!bossIds.contains(wid)) {
                allWorkerIds.add(wid);
            }
        }

        // 查询工人信息
        Map<Long, User> userMap = new HashMap<>();
        Map<Long, String> roleMap = new HashMap<>();
        if (!allWorkerIds.isEmpty()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, allWorkerIds));
            for (User u : users) userMap.put(u.getId(), u);

            // 查询职位
            for (UserCompany uc : allUcList) {
                roleMap.put(uc.getUserId(), uc.getRole());
            }
        }

        // 查询关联的扎货、款式、床号、工序信息
        Set<Long> bundleIds = allRecords.stream().map(WorkRecord::getBundleId).collect(Collectors.toSet());
        Map<Long, Bundle> bundleMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            List<Bundle> bundles = bundleMapper.selectList(new LambdaQueryWrapper<Bundle>().in(Bundle::getId, bundleIds));
            for (Bundle b : bundles) bundleMap.put(b.getId(), b);
        }

        Set<Long> styleIds = bundleMap.values().stream().map(Bundle::getStyleId).collect(Collectors.toSet());
        Map<Long, Style> styleMap = new HashMap<>();
        if (!styleIds.isEmpty()) {
            List<Style> styles = styleMapper.selectList(new LambdaQueryWrapper<Style>().in(Style::getId, styleIds));
            for (Style s : styles) styleMap.put(s.getId(), s);
        }

        Set<Long> bedIds = bundleMap.values().stream().map(Bundle::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Bed> bedMap = new HashMap<>();
        if (!bedIds.isEmpty()) {
            List<Bed> beds = bedMapper.selectList(new LambdaQueryWrapper<Bed>().in(Bed::getId, bedIds));
            for (Bed b : beds) bedMap.put(b.getId(), b);
        }

        Set<Long> bpIds = allRecords.stream().map(WorkRecord::getBundleProcessId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BundleProcess> bpMap = new HashMap<>();
        if (!bpIds.isEmpty()) {
            List<BundleProcess> bps = bundleProcessMapper.selectList(new LambdaQueryWrapper<BundleProcess>().in(BundleProcess::getId, bpIds));
            for (BundleProcess bp : bps) bpMap.put(bp.getId(), bp);
        }

        // 构建预览数据（包含所有活跃员工，没有报工记录的显示为0）
        List<Map<String, Object>> previewList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Long workerId : allWorkerIds) {
            List<WorkRecord> records = grouped.get(workerId);
            BigDecimal supplementAmount = adjustmentMap.getOrDefault(workerId, BigDecimal.ZERO);

            int totalQuantity = 0;
            BigDecimal baseAmount = BigDecimal.ZERO;
            List<Map<String, Object>> detailRecords = new ArrayList<>();

            if (records != null && !records.isEmpty()) {
                totalQuantity = records.stream().mapToInt(WorkRecord::getQuantity).sum();
                baseAmount = records.stream()
                        .map(WorkRecord::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 构建报工明细
                for (WorkRecord wr : records) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", wr.getId());
                    item.put("quantity", wr.getQuantity());
                    item.put("price", wr.getPrice());
                    item.put("amount", wr.getAmount());
                    item.put("createTime", wr.getCreateTime());

                    Bundle bundle = bundleMap.get(wr.getBundleId());
                    if (bundle != null) {
                        item.put("bundleNo", bundle.getBundleNo());
                        Style style = styleMap.get(bundle.getStyleId());
                        if (style != null) {
                            item.put("styleNo", style.getStyleNo());
                        }
                        if (bundle.getBedId() != null) {
                            Bed bed = bedMap.get(bundle.getBedId());
                            if (bed != null) {
                                item.put("bedNo", bed.getBedNo());
                            }
                        }
                    }
                    if (wr.getBundleProcessId() != null) {
                        BundleProcess bp = bpMap.get(wr.getBundleProcessId());
                        if (bp != null) {
                            item.put("processName", bp.getProcessName());
                        }
                    }
                    detailRecords.add(item);
                }
            }

            BigDecimal finalAmount = baseAmount.add(supplementAmount);

            User user = userMap.get(workerId);

            Map<String, Object> workerData = new HashMap<>();
            workerData.put("workerId", workerId);
            workerData.put("workerName", user != null ? user.getRealName() : "未知");
            workerData.put("workerPhone", user != null ? user.getPhone() : null);
            workerData.put("workerRole", roleMap.getOrDefault(workerId, "worker"));
            workerData.put("totalQuantity", totalQuantity);
            workerData.put("baseAmount", baseAmount);
            workerData.put("supplementAmount", supplementAmount);
            workerData.put("finalAmount", finalAmount);
            workerData.put("workRecords", detailRecords);
            previewList.add(workerData);

            totalAmount = totalAmount.add(finalAmount);
        }

        // 按工资降序排序
        previewList.sort((a, b) -> ((BigDecimal) b.get("finalAmount")).compareTo((BigDecimal) a.get("finalAmount")));

        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("workerCount", previewList.size());
        result.put("totalAmount", totalAmount);
        result.put("workers", previewList);
        // 添加"待定价"警告信息
        result.put("hasPendingPrice", hasPendingPrice);
        result.put("pendingPriceCount", pendingCount);
        return result;
    }

    /**
     * 批量确认员工（生成结算记录）
     */
    @Transactional
    public Map<String, Object> confirmEmployees(String startDate, String endDate, List<Long> employeeIds) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new RuntimeException("请选择要确认的员工");
        }

        // 查找或创建结算单
        LambdaQueryWrapper<WageSettlement> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(WageSettlement::getCompanyId, companyId)
                .eq(WageSettlement::getStartDate, startDate)
                .eq(WageSettlement::getEndDate, endDate);
        WageSettlement settlement = settlementMapper.selectOne(checkWrapper);

        if (settlement == null) {
            settlement = new WageSettlement();
            settlement.setCompanyId(companyId);
            settlement.setStartDate(startDate);
            settlement.setEndDate(endDate);
            settlement.setWorkerCount(0);
            settlement.setConfirmedCount(0);
            settlement.setStatus(0);
            settlement.setCreatorId(userId);
            settlement.setTotalAmount(BigDecimal.ZERO);
            settlementMapper.insert(settlement);
        }

        // 查询时间段内所有已通过的报工记录
        LambdaQueryWrapper<WorkRecord> wrWrapper = new LambdaQueryWrapper<>();
        wrWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .isNull(WorkRecord::getSettlementId)
                .in(WorkRecord::getWorkerId, employeeIds)
                .ge(WorkRecord::getCreateTime, startDate + "T00:00:00")
                .lt(WorkRecord::getCreateTime, endDate + "T23:59:59");
        List<WorkRecord> allRecords = workRecordMapper.selectList(wrWrapper);

        // 检查是否有"待定价"记录（单价为0），如果有则阻止结算
        List<WorkRecord> pendingPriceRecords = allRecords.stream()
                .filter(r -> r.getPrice() == null || r.getPrice().compareTo(BigDecimal.ZERO) == 0)
                .collect(Collectors.toList());
        if (!pendingPriceRecords.isEmpty()) {
            throw new RuntimeException("存在" + pendingPriceRecords.size() + "条\"待定价\"记录，请先完善工序单价后再结算");
        }

        // 按工人分组汇总
        Map<Long, List<WorkRecord>> grouped = allRecords.stream()
                .collect(Collectors.groupingBy(WorkRecord::getWorkerId));

        // 查询工资调整记录
        List<WageAdjustment> adjustments = adjustmentMapper.selectList(
                new LambdaQueryWrapper<WageAdjustment>()
                        .eq(WageAdjustment::getCompanyId, companyId)
                        .eq(WageAdjustment::getStatus, 1)
                        .isNull(WageAdjustment::getSettlementId)
                        .in(WageAdjustment::getWorkerId, employeeIds)
                        .ge(WageAdjustment::getCreateTime, startDate + "T00:00:00")
                        .lt(WageAdjustment::getCreateTime, endDate + "T23:59:59")
        );
        Map<Long, BigDecimal> adjustmentMap = new HashMap<>();
        for (WageAdjustment adj : adjustments) {
            BigDecimal adjAmount = adj.getType() == 1 ? adj.getAmount() : adj.getAmount().negate();
            adjustmentMap.merge(adj.getWorkerId(), adjAmount, BigDecimal::add);
        }

        int confirmedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 为每个员工创建明细
        for (Long workerId : employeeIds) {
            List<WorkRecord> records = grouped.get(workerId);
            if (records == null || records.isEmpty()) {
                continue;
            }

            int totalQuantity = records.stream().mapToInt(WorkRecord::getQuantity).sum();
            BigDecimal baseAmount = records.stream()
                    .map(WorkRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal supplementAmount = adjustmentMap.getOrDefault(workerId, BigDecimal.ZERO);
            BigDecimal finalAmount = baseAmount.add(supplementAmount);

            // 跳过0元工资的员工
            if (finalAmount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // 创建明细
            WageSettlementDetail detail = new WageSettlementDetail();
            detail.setSettlementId(settlement.getId());
            detail.setCompanyId(companyId);
            detail.setWorkerId(workerId);
            detail.setTotalQuantity(totalQuantity);
            detail.setBaseAmount(baseAmount);
            detail.setSupplementAmount(supplementAmount);
            detail.setFinalAmount(finalAmount);
            detail.setStatus(1); // 已确认
            detail.setVisibleToEmployee(1); // 默认可见
            detail.setEmployeeConfirmed(0);
            detail.setConfirmedTime(LocalDateTime.now());
            detailMapper.insert(detail);

            // 标记 work_record 的 settlement_id
            for (WorkRecord wr : records) {
                wr.setSettlementId(settlement.getId());
                workRecordMapper.updateById(wr);
            }

            // 标记 wage_adjustment 的 settlement_id
            for (WageAdjustment adj : adjustments) {
                if (adj.getWorkerId().equals(workerId)) {
                    adj.setSettlementId(settlement.getId());
                    adjustmentMapper.updateById(adj);
                }
            }

            confirmedCount++;
            totalAmount = totalAmount.add(finalAmount);
        }

        // 更新结算单
        settlement.setWorkerCount(settlement.getWorkerCount() + confirmedCount);
        settlement.setConfirmedCount(settlement.getConfirmedCount() + confirmedCount);
        settlement.setTotalAmount(settlement.getTotalAmount().add(totalAmount));
        settlement.setStatus(1); // 已结算
        settlementMapper.updateById(settlement);

        // 返回结算单信息
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlement.getId());
        result.put("confirmedCount", confirmedCount);
        result.put("totalAmount", totalAmount);
        return result;
    }

    /**
     * 结算单列表（老板查看）
     */
    public List<WageSettlementVO> listSettlements() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        LambdaQueryWrapper<WageSettlement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WageSettlement::getCompanyId, companyId)
                .orderByDesc(WageSettlement::getCreateTime);
        List<WageSettlement> list = settlementMapper.selectList(wrapper);

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 结算单详情（含工人明细列表）
     */
    public Map<String, Object> getSettlementDetail(Long settlementId) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        WageSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null || !settlement.getCompanyId().equals(companyId)) {
            throw new RuntimeException("结算单不存在");
        }

        List<WageSettlementDetailVO> detailVOs = getDetailVOs(settlementId);

        Map<String, Object> result = new HashMap<>();
        result.put("settlement", toVO(settlement));
        result.put("details", detailVOs);
        return result;
    }

    /**
     * 某个工人的报工明细（老板查看，用于确认前审核）
     */
    public Map<String, Object> getWorkerDetail(Long settlementId, Long workerId) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        WageSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null || !settlement.getCompanyId().equals(companyId)) {
            throw new RuntimeException("结算单不存在");
        }

        // 查找该工人的结算明细
        LambdaQueryWrapper<WageSettlementDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(WageSettlementDetail::getSettlementId, settlementId)
                .eq(WageSettlementDetail::getWorkerId, workerId);
        WageSettlementDetail detail = detailMapper.selectOne(dw);
        if (detail == null) {
            throw new RuntimeException("该工人不在此结算单中");
        }

        // 查询该工人该时间段的报工记录
        LambdaQueryWrapper<WorkRecord> wrWrapper = new LambdaQueryWrapper<>();
        wrWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getWorkerId, workerId)
                .eq(WorkRecord::getStatus, 1)
                .eq(WorkRecord::getSettlementId, settlementId)
                .ge(WorkRecord::getCreateTime, settlement.getStartDate() + "T00:00:00")
                .lt(WorkRecord::getCreateTime, settlement.getEndDate() + "T23:59:59")
                .orderByDesc(WorkRecord::getCreateTime);
        List<WorkRecord> records = workRecordMapper.selectList(wrWrapper);

        // 查询工人信息
        User user = userMapper.selectById(workerId);

        // 查询关联的扎货信息（获取款号和床号）
        Set<Long> bundleIds = records.stream().map(WorkRecord::getBundleId).collect(Collectors.toSet());
        Map<Long, Bundle> bundleMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            List<Bundle> bundles = bundleMapper.selectList(new LambdaQueryWrapper<Bundle>().in(Bundle::getId, bundleIds));
            for (Bundle b : bundles) bundleMap.put(b.getId(), b);
        }

        // 查询款式信息
        Set<Long> styleIds = bundleMap.values().stream().map(Bundle::getStyleId).collect(Collectors.toSet());
        Map<Long, Style> styleMap = new HashMap<>();
        if (!styleIds.isEmpty()) {
            List<Style> styles = styleMapper.selectList(new LambdaQueryWrapper<Style>().in(Style::getId, styleIds));
            for (Style s : styles) styleMap.put(s.getId(), s);
        }

        // 查询床号信息
        Set<Long> bedIds = bundleMap.values().stream().map(Bundle::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Bed> bedMap = new HashMap<>();
        if (!bedIds.isEmpty()) {
            List<Bed> beds = bedMapper.selectList(new LambdaQueryWrapper<Bed>().in(Bed::getId, bedIds));
            for (Bed b : beds) bedMap.put(b.getId(), b);
        }

        // 查询工序信息
        Set<Long> bpIds = records.stream().map(WorkRecord::getBundleProcessId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BundleProcess> bpMap = new HashMap<>();
        if (!bpIds.isEmpty()) {
            List<BundleProcess> bps = bundleProcessMapper.selectList(new LambdaQueryWrapper<BundleProcess>().in(BundleProcess::getId, bpIds));
            for (BundleProcess bp : bps) bpMap.put(bp.getId(), bp);
        }

        // 构建富化后的报工记录
        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (WorkRecord wr : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", wr.getId());
            item.put("quantity", wr.getQuantity());
            item.put("price", wr.getPrice());
            item.put("amount", wr.getAmount());
            item.put("createTime", wr.getCreateTime());

            Bundle bundle = bundleMap.get(wr.getBundleId());
            if (bundle != null) {
                item.put("bundleNo", bundle.getBundleNo());
                Style style = styleMap.get(bundle.getStyleId());
                if (style != null) {
                    item.put("styleNo", style.getStyleNo());
                }
                if (bundle.getBedId() != null) {
                    Bed bed = bedMap.get(bundle.getBedId());
                    if (bed != null) {
                        item.put("bedNo", bed.getBedNo());
                    }
                }
            }
            if (wr.getBundleProcessId() != null) {
                BundleProcess bp = bpMap.get(wr.getBundleProcessId());
                if (bp != null) {
                    item.put("processName", bp.getProcessName());
                }
            }
            enrichedRecords.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        WageSettlementDetailVO detailVO = toDetailVO(detail, user);
        result.put("detail", detailVO);
        result.put("workRecords", enrichedRecords);
        return result;
    }

    /**
     * 打印工资条数据 — 所有工人按(款号+床号+工序)汇总
     */
    public Map<String, Object> getPrintData(Long settlementId) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        WageSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null || !settlement.getCompanyId().equals(companyId)) {
            throw new RuntimeException("结算单不存在");
        }

        // 获取所有工人明细
        LambdaQueryWrapper<WageSettlementDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(WageSettlementDetail::getSettlementId, settlementId)
                .orderByAsc(WageSettlementDetail::getId);
        List<WageSettlementDetail> details = detailMapper.selectList(dw);

        Set<Long> workerIds = details.stream().map(WageSettlementDetail::getWorkerId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!workerIds.isEmpty()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, workerIds));
            for (User u : users) userMap.put(u.getId(), u);
        }

        // 批量查询所有报工记录
        LambdaQueryWrapper<WorkRecord> wrWrapper = new LambdaQueryWrapper<>();
        wrWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .in(WorkRecord::getWorkerId, workerIds)
                .eq(WorkRecord::getSettlementId, settlementId)
                .ge(WorkRecord::getCreateTime, settlement.getStartDate() + "T00:00:00")
                .lt(WorkRecord::getCreateTime, settlement.getEndDate() + "T23:59:59");
        List<WorkRecord> allRecords = workRecordMapper.selectList(wrWrapper);

        // 查询关联的扎货、款式、床号、工序信息
        Set<Long> bundleIds = allRecords.stream().map(WorkRecord::getBundleId).collect(Collectors.toSet());
        Map<Long, Bundle> bundleMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            List<Bundle> bundles = bundleMapper.selectList(new LambdaQueryWrapper<Bundle>().in(Bundle::getId, bundleIds));
            for (Bundle b : bundles) bundleMap.put(b.getId(), b);
        }

        Set<Long> styleIds = bundleMap.values().stream().map(Bundle::getStyleId).collect(Collectors.toSet());
        Map<Long, Style> styleMap = new HashMap<>();
        if (!styleIds.isEmpty()) {
            List<Style> styles = styleMapper.selectList(new LambdaQueryWrapper<Style>().in(Style::getId, styleIds));
            for (Style s : styles) styleMap.put(s.getId(), s);
        }

        Set<Long> bedIds = bundleMap.values().stream().map(Bundle::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Bed> bedMap = new HashMap<>();
        if (!bedIds.isEmpty()) {
            List<Bed> beds = bedMapper.selectList(new LambdaQueryWrapper<Bed>().in(Bed::getId, bedIds));
            for (Bed b : beds) bedMap.put(b.getId(), b);
        }

        Set<Long> bpIds = allRecords.stream().map(WorkRecord::getBundleProcessId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BundleProcess> bpMap = new HashMap<>();
        if (!bpIds.isEmpty()) {
            List<BundleProcess> bps = bundleProcessMapper.selectList(new LambdaQueryWrapper<BundleProcess>().in(BundleProcess::getId, bpIds));
            for (BundleProcess bp : bps) bpMap.put(bp.getId(), bp);
        }

        // 按工人分组
        Map<Long, List<WorkRecord>> recordsByWorker = allRecords.stream()
                .collect(Collectors.groupingBy(WorkRecord::getWorkerId));

        // 为每个工人构建工序汇总
        List<Map<String, Object>> workerList = new ArrayList<>();
        for (WageSettlementDetail d : details) {
            User user = userMap.get(d.getWorkerId());
            Map<String, Object> workerData = new HashMap<>();
            workerData.put("workerId", d.getWorkerId());
            workerData.put("workerName", user != null ? user.getRealName() : "未知");
            workerData.put("totalQuantity", d.getTotalQuantity());
            workerData.put("baseAmount", d.getBaseAmount());
            workerData.put("supplementAmount", d.getSupplementAmount() != null ? d.getSupplementAmount() : BigDecimal.ZERO);
            workerData.put("finalAmount", d.getFinalAmount() != null ? d.getFinalAmount() : d.getBaseAmount());

            List<WorkRecord> workerRecords = recordsByWorker.getOrDefault(d.getWorkerId(), Collections.emptyList());

            // 按 (styleNo + bedNo + processName) 分组汇总
            Map<String, Map<String, Object>> summaryMap = new LinkedHashMap<>();
            for (WorkRecord wr : workerRecords) {
                Bundle bundle = bundleMap.get(wr.getBundleId());
                String styleNo = "-";
                String bedNo = "-";
                if (bundle != null) {
                    Style style = styleMap.get(bundle.getStyleId());
                    if (style != null) styleNo = style.getStyleNo();
                    if (bundle.getBedId() != null) {
                        Bed bed = bedMap.get(bundle.getBedId());
                        if (bed != null) bedNo = bed.getBedNo();
                    }
                }
                String processName = "-";
                if (wr.getBundleProcessId() != null) {
                    BundleProcess bp = bpMap.get(wr.getBundleProcessId());
                    if (bp != null) processName = bp.getProcessName();
                }

                final String fStyleNo = styleNo;
                final String fBedNo = bedNo;
                final String fProcessName = processName;
                String key = styleNo + "|" + bedNo + "|" + processName;
                summaryMap.computeIfAbsent(key, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("styleNo", fStyleNo);
                    m.put("bedNo", fBedNo);
                    m.put("processName", fProcessName);
                    m.put("quantity", 0);
                    m.put("price", wr.getPrice());
                    m.put("amount", BigDecimal.ZERO);
                    return m;
                });
                Map<String, Object> summary = summaryMap.get(key);
                summary.put("quantity", (int) summary.get("quantity") + wr.getQuantity());
                summary.put("amount", ((BigDecimal) summary.get("amount")).add(wr.getAmount()));
            }

            workerData.put("processSummary", new ArrayList<>(summaryMap.values()));
            workerList.add(workerData);
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> settlementInfo = new HashMap<>();
        settlementInfo.put("startDate", settlement.getStartDate());
        settlementInfo.put("endDate", settlement.getEndDate());
        settlementInfo.put("totalAmount", settlement.getTotalAmount());
        settlementInfo.put("workerCount", settlement.getWorkerCount());
        result.put("settlement", settlementInfo);
        result.put("workers", workerList);
        return result;
    }

    /**
     * 设置员工可见性
     */
    @Transactional
    public void setVisibility(Long settlementId, Long workerId, Integer visible) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        WageSettlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null || !settlement.getCompanyId().equals(companyId)) {
            throw new RuntimeException("结算单不存在");
        }

        LambdaQueryWrapper<WageSettlementDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(WageSettlementDetail::getSettlementId, settlementId)
                .eq(WageSettlementDetail::getWorkerId, workerId);
        WageSettlementDetail detail = detailMapper.selectOne(dw);
        if (detail == null) {
            throw new RuntimeException("该工人不在此结算单中");
        }

        detail.setVisibleToEmployee(visible);
        detailMapper.updateById(detail);
    }

    /**
     * 员工端：我的工资结算
     */
    public List<Map<String, Object>> getMySettlements() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 查询当前员工的结算明细
        LambdaQueryWrapper<WageSettlementDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(WageSettlementDetail::getCompanyId, companyId)
                .eq(WageSettlementDetail::getWorkerId, userId)
                .eq(WageSettlementDetail::getVisibleToEmployee, 1);
        List<WageSettlementDetail> details = detailMapper.selectList(detailWrapper);

        if (details.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询关联的结算单
        Set<Long> settlementIds = details.stream()
                .map(WageSettlementDetail::getSettlementId)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<WageSettlement> sw = new LambdaQueryWrapper<>();
        sw.in(WageSettlement::getId, settlementIds)
                .eq(WageSettlement::getCompanyId, companyId);
        List<WageSettlement> settlements = settlementMapper.selectList(sw);

        Map<Long, WageSettlement> settlementMap = settlements.stream()
                .collect(Collectors.toMap(WageSettlement::getId, s -> s));

        List<Map<String, Object>> result = new ArrayList<>();
        for (WageSettlementDetail d : details) {
            WageSettlement s = settlementMap.get(d.getSettlementId());
            if (s == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("settlementId", s.getId());
            item.put("startDate", s.getStartDate());
            item.put("endDate", s.getEndDate());
            item.put("detailId", d.getId());
            item.put("totalQuantity", d.getTotalQuantity());
            item.put("baseAmount", d.getBaseAmount());
            item.put("supplementAmount", d.getSupplementAmount());
            item.put("finalAmount", d.getFinalAmount());
            item.put("employeeConfirmed", d.getEmployeeConfirmed());
            item.put("employeeConfirmTime", d.getEmployeeConfirmTime());
            result.add(item);
        }

        return result;
    }

    /**
     * 员工端：确认查看工资
     */
    @Transactional
    public void confirmViewing(Long settlementId) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        LambdaQueryWrapper<WageSettlementDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(WageSettlementDetail::getSettlementId, settlementId)
                .eq(WageSettlementDetail::getWorkerId, userId);
        WageSettlementDetail detail = detailMapper.selectOne(dw);

        if (detail == null) {
            throw new RuntimeException("您不在该结算单中");
        }
        if (detail.getEmployeeConfirmed() == 1) {
            throw new RuntimeException("您已确认，请勿重复操作");
        }

        detail.setEmployeeConfirmed(1);
        detail.setEmployeeConfirmTime(LocalDateTime.now());
        detailMapper.updateById(detail);

        // 检查该结算单是否所有员工都已确认，如果是则更新状态为"已签名"
        LambdaQueryWrapper<WageSettlementDetail> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(WageSettlementDetail::getSettlementId, settlementId);
        long totalDetails = detailMapper.selectCount(countWrapper);
        countWrapper.eq(WageSettlementDetail::getEmployeeConfirmed, 1);
        long confirmedDetails = detailMapper.selectCount(countWrapper);

        if (totalDetails > 0 && totalDetails == confirmedDetails) {
            WageSettlement settlement = settlementMapper.selectById(settlementId);
            if (settlement != null && settlement.getStatus() != 2) {
                settlement.setStatus(2);
                settlementMapper.updateById(settlement);
            }
        }
    }

    /**
     * 工资统计（按月查看报工工资汇总，不需要创建结算单）
     */
    public Map<String, Object> getWageStatistics(String yearMonth) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 计算日期范围
        String startDate = yearMonth + "-01";
        int year = Integer.parseInt(yearMonth.split("-")[0]);
        int month = Integer.parseInt(yearMonth.split("-")[1]);
        String endDate = (month == 12) ? (year + 1) + "-01-01" : year + "-" + String.format("%02d", month + 1) + "-01";

        // 查询该月所有已通过的报工记录
        LambdaQueryWrapper<WorkRecord> wrWrapper = new LambdaQueryWrapper<>();
        wrWrapper.eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .ge(WorkRecord::getCreateTime, startDate + "T00:00:00")
                .lt(WorkRecord::getCreateTime, endDate + "T00:00:00");
        List<WorkRecord> allRecords = workRecordMapper.selectList(wrWrapper);

        // 按工人分组汇总
        Map<Long, List<WorkRecord>> grouped = allRecords.stream()
                .collect(Collectors.groupingBy(WorkRecord::getWorkerId));

        // 查询工人信息
        Set<Long> workerIds = grouped.keySet();
        Map<Long, User> userMap = new HashMap<>();
        if (!workerIds.isEmpty()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, workerIds));
            for (User u : users) userMap.put(u.getId(), u);
        }

        // 构建结果
        List<Map<String, Object>> workerStats = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (Map.Entry<Long, List<WorkRecord>> entry : grouped.entrySet()) {
            Long workerId = entry.getKey();
            List<WorkRecord> records = entry.getValue();

            int qty = records.stream().mapToInt(WorkRecord::getQuantity).sum();
            BigDecimal amount = records.stream().map(WorkRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> item = new HashMap<>();
            item.put("workerId", workerId);
            User user = userMap.get(workerId);
            item.put("workerName", user != null ? user.getRealName() : "未知");
            item.put("workerPhone", user != null ? user.getPhone() : null);
            item.put("totalQuantity", qty);
            item.put("totalAmount", amount);
            item.put("recordCount", records.size());
            workerStats.add(item);

            totalAmount = totalAmount.add(amount);
            totalQuantity += qty;
        }

        // 按工资降序排序
        workerStats.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));

        Map<String, Object> result = new HashMap<>();
        result.put("yearMonth", yearMonth);
        result.put("workerCount", grouped.size());
        result.put("totalQuantity", totalQuantity);
        result.put("totalAmount", totalAmount);
        result.put("workers", workerStats);
        return result;
    }

    // ===== 辅助方法 =====

    /** 获取结算单的工人明细VO列表 */
    private List<WageSettlementDetailVO> getDetailVOs(Long settlementId) {
        LambdaQueryWrapper<WageSettlementDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(WageSettlementDetail::getSettlementId, settlementId)
                .orderByAsc(WageSettlementDetail::getStatus)
                .orderByDesc(WageSettlementDetail::getBaseAmount);
        List<WageSettlementDetail> details = detailMapper.selectList(dw);

        Set<Long> workerIds = details.stream().map(WageSettlementDetail::getWorkerId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!workerIds.isEmpty()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, workerIds));
            for (User u : users) userMap.put(u.getId(), u);
        }

        List<WageSettlementDetailVO> vos = new ArrayList<>();
        for (WageSettlementDetail d : details) {
            vos.add(toDetailVO(d, userMap.get(d.getWorkerId())));
        }
        return vos;
    }

    private WageSettlementDetailVO toDetailVO(WageSettlementDetail d, User user) {
        WageSettlementDetailVO vo = new WageSettlementDetailVO();
        vo.setId(d.getId());
        vo.setWorkerId(d.getWorkerId());
        vo.setTotalQuantity(d.getTotalQuantity());
        vo.setBaseAmount(d.getBaseAmount());
        vo.setSupplementAmount(d.getSupplementAmount());
        vo.setFinalAmount(d.getFinalAmount());
        vo.setStatus(d.getStatus());
        vo.setVisibleToEmployee(d.getVisibleToEmployee());
        vo.setEmployeeConfirmed(d.getEmployeeConfirmed());
        vo.setEmployeeConfirmTime(d.getEmployeeConfirmTime());
        vo.setConfirmedTime(d.getConfirmedTime());
        vo.setRemark(d.getRemark());
        if (user != null) {
            vo.setWorkerName(user.getRealName());
            vo.setWorkerPhone(user.getPhone());
            vo.setBankCard(user.getBankCard());
        }
        return vo;
    }

    private WageSettlementVO toVO(WageSettlement s) {
        WageSettlementVO vo = new WageSettlementVO();
        vo.setId(s.getId());
        vo.setStartDate(s.getStartDate());
        vo.setEndDate(s.getEndDate());
        vo.setTotalAmount(s.getTotalAmount());
        vo.setWorkerCount(s.getWorkerCount());
        vo.setConfirmedCount(s.getConfirmedCount());
        vo.setStatus(s.getStatus());
        vo.setCreatorId(s.getCreatorId());
        vo.setCreateTime(s.getCreateTime());

        User creator = userMapper.selectById(s.getCreatorId());
        if (creator != null) {
            vo.setCreatorName(creator.getRealName());
        }
        return vo;
    }
}

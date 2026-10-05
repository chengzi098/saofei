package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.TenantContext;
import com.garment.dto.ReturnWorkRecordDTO;
import com.garment.dto.SubmitWorkRecordDTO;
import com.garment.entity.*;
import com.garment.mapper.*;
import com.garment.vo.WorkRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

@Service
public class WorkRecordService {

    private final WorkRecordMapper workRecordMapper;
    private final BundleMapper bundleMapper;
    private final BundleProcessMapper bundleProcessMapper;
    private final StyleMapper styleMapper;
    private final UserMapper userMapper;
    private final BedMapper bedMapper;
    private final UserCompanyMapper userCompanyMapper;

    public WorkRecordService(WorkRecordMapper workRecordMapper, BundleMapper bundleMapper,
                             BundleProcessMapper bundleProcessMapper, StyleMapper styleMapper,
                             UserMapper userMapper, BedMapper bedMapper,
                             UserCompanyMapper userCompanyMapper) {
        this.workRecordMapper = workRecordMapper;
        this.bundleMapper = bundleMapper;
        this.bundleProcessMapper = bundleProcessMapper;
        this.styleMapper = styleMapper;
        this.userMapper = userMapper;
        this.bedMapper = bedMapper;
        this.userCompanyMapper = userCompanyMapper;
    }

    /**
     * 提交报工（工人扫码后）—— 免审核直接生效
     */
    @Transactional
    public void submitWorkRecord(SubmitWorkRecordDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 确定实际报工的员工ID
        Long workerId = userId;
        if (dto.getWorkerId() != null) {
            // 老板指派模式：验证当前用户是老板
            LambdaQueryWrapper<UserCompany> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(UserCompany::getCompanyId, companyId)
                    .eq(UserCompany::getUserId, userId)
                    .eq(UserCompany::getStatus, 1);
            UserCompany userCompany = userCompanyMapper.selectOne(roleWrapper);
            if (userCompany == null
                    || (!"boss".equals(userCompany.getRole()) && !"supervisor".equals(userCompany.getRole()))) {
                throw new RuntimeException("只有老板或主管可以指派工序");
            }
            // 验证指定员工属于当前公司
            LambdaQueryWrapper<UserCompany> workerWrapper = new LambdaQueryWrapper<>();
            workerWrapper.eq(UserCompany::getCompanyId, companyId)
                    .eq(UserCompany::getUserId, dto.getWorkerId())
                    .eq(UserCompany::getStatus, 1);
            UserCompany workerCompany = userCompanyMapper.selectOne(workerWrapper);
            if (workerCompany == null) {
                throw new RuntimeException("该员工不属于当前公司");
            }
            workerId = dto.getWorkerId();
        }

        // 验证扎货
        Bundle bundle = bundleMapper.selectById(dto.getBundleId());
        if (bundle == null || !bundle.getCompanyId().equals(companyId) || bundle.getDeleted() == 1) {
            throw new RuntimeException("扎货不存在");
        }

        // 验证工序
        BundleProcess bp = bundleProcessMapper.selectById(dto.getBundleProcessId());
        if (bp == null || !bp.getCompanyId().equals(companyId) || !bp.getBundleId().equals(dto.getBundleId())) {
            throw new RuntimeException("工序不存在");
        }

        // 检查工序是否已报工（防止重复提交）
        if (bp.getStatus() == 2) {
            throw new RuntimeException("该工序已完成，请勿重复提交");
        }

        // 数量默认等于扎货件数（整扎完成）
        int quantity = bundle.getQuantity();

        // 计算金额
        BigDecimal amount = bp.getPrice().multiply(BigDecimal.valueOf(quantity));

        // 如果该工序之前有退回记录，标记为已处理（status=4）
        LambdaQueryWrapper<WorkRecord> oldReturnWrapper = new LambdaQueryWrapper<>();
        oldReturnWrapper.eq(WorkRecord::getBundleProcessId, dto.getBundleProcessId())
                .eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 3);
        WorkRecord oldReturned = workRecordMapper.selectOne(oldReturnWrapper);
        if (oldReturned != null) {
            oldReturned.setStatus(4); // 已重新报工
            workRecordMapper.updateById(oldReturned);
        }

        // 创建报工记录（status=1 直接通过）
        WorkRecord record = new WorkRecord();
        record.setCompanyId(companyId);
        record.setWorkerId(workerId);
        record.setBundleId(dto.getBundleId());
        record.setBundleProcessId(dto.getBundleProcessId());
        record.setQuantity(quantity);
        record.setPrice(bp.getPrice());
        record.setAmount(amount);
        record.setStatus(1); // 直接生效
        record.setWorkerRemark(dto.getRemark());
        workRecordMapper.insert(record);

        // 更新扎货状态为进行中
        if (bundle.getStatus() == 0) {
            bundle.setStatus(1);
            bundleMapper.updateById(bundle);
        }

        // 更新工序状态为已完成（直接完成，跳过进行中）
        bp.setStatus(2);
        bp.setOperatorId(workerId);
        // 获取员工姓名
        User user = userMapper.selectById(workerId);
        if (user != null) {
            bp.setOperatorName(user.getRealName());
        }
        bp.setCompleteTime(LocalDateTime.now());
        bundleProcessMapper.updateById(bp);

        // 检查该扎所有工序是否都已完成，如果是则扎货状态设为已完成
        List<BundleProcess> allProcesses = bundleProcessMapper.selectList(
                new LambdaQueryWrapper<BundleProcess>()
                        .eq(BundleProcess::getBundleId, dto.getBundleId())
                        .eq(BundleProcess::getCompanyId, companyId)
        );
        boolean allDone = allProcesses.stream().allMatch(p -> p.getStatus() == 2);
        if (allDone) {
            bundle.setStatus(2);
            bundleMapper.updateById(bundle);
        }
    }

    /**
     * 退回报工（主管发现问题后重新扫码退回）
     */
    @Transactional
    public void returnWorkRecord(Long bundleProcessId, ReturnWorkRecordDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 验证工序
        BundleProcess bp = bundleProcessMapper.selectById(bundleProcessId);
        if (bp == null || !bp.getCompanyId().equals(companyId)) {
            throw new RuntimeException("工序不存在");
        }

        // 验证扎货属于当前公司
        Bundle bundleCheck = bundleMapper.selectById(bp.getBundleId());
        if (bundleCheck == null || !bundleCheck.getCompanyId().equals(companyId)) {
            throw new RuntimeException("无权操作此工序");
        }

        // 验证工序是否已完成（只有已完成的才能退回）
        if (bp.getStatus() != 2) {
            throw new RuntimeException("该工序未完成，无法退回");
        }

        // 查找对应的报工记录（最新一条通过的）
        LambdaQueryWrapper<WorkRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkRecord::getBundleProcessId, bundleProcessId)
                .eq(WorkRecord::getCompanyId, companyId)
                .eq(WorkRecord::getStatus, 1)
                .orderByDesc(WorkRecord::getCreateTime)
                .last("LIMIT 1");
        WorkRecord record = workRecordMapper.selectOne(wrapper);

        if (record == null) {
            throw new RuntimeException("未找到该工序的报工记录");
        }

        // 更新报工记录为已退回
        record.setStatus(3);
        record.setReturnReason(dto.getReason());
        record.setReturnerId(userId);
        record.setReturnTime(LocalDateTime.now());
        workRecordMapper.updateById(record);

        // 工序状态恢复为未开始，清空操作人和完成时间
        bp.setStatus(0);
        bp.setOperatorId(null);
        bp.setOperatorName(null);
        bp.setCompleteTime(null);
        bundleProcessMapper.updateById(bp);

        // 扎货状态恢复为进行中（如果原来是已完成）
        Bundle bundle = bundleMapper.selectById(bp.getBundleId());
        if (bundle != null && bundle.getStatus() == 2) {
            bundle.setStatus(1);
            bundleMapper.updateById(bundle);
        }
    }

    /**
     * 报工列表（分页，支持按状态、工人、床号、扎号、日期范围筛选）
     */
    public Page<WorkRecordVO> listWorkRecords(int pageNum, int pageSize, Integer status, Long workerId,
                                               String workerName, String styleNo, String bedNo,
                                               String bundleNo, String processName,
                                               String startDate, String endDate) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        if ("worker".equals(TenantContext.getCompanyRole())) {
            workerId = TenantContext.getUserId();
            workerName = null;
        }

        // 如果有工人姓名筛选，先查询匹配的工人ID
        Set<Long> filterWorkerIds = null;
        if (workerName != null && !workerName.isEmpty()) {
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getRealName, workerName)
            );
            filterWorkerIds = new HashSet<>();
            for (User u : users) {
                filterWorkerIds.add(u.getId());
            }
            if (filterWorkerIds.isEmpty()) {
                // 没有匹配的工人，直接返回空结果
                Page<WorkRecordVO> emptyResult = new Page<>();
                emptyResult.setCurrent(pageNum);
                emptyResult.setSize(pageSize);
                emptyResult.setTotal(0);
                emptyResult.setRecords(new ArrayList<>());
                return emptyResult;
            }
        }

        // 如果有款号、床号或扎号筛选，先查询匹配的扎货ID
        Set<Long> filterBundleIds = null;
        if ((styleNo != null && !styleNo.isEmpty())
                || (bedNo != null && !bedNo.isEmpty())
                || (bundleNo != null && !bundleNo.isEmpty())) {
            LambdaQueryWrapper<Bundle> bundleWrapper = new LambdaQueryWrapper<>();
            bundleWrapper.eq(Bundle::getCompanyId, companyId).eq(Bundle::getDeleted, 0);
            if (styleNo != null && !styleNo.isEmpty()) {
                // 款号筛选：先匹配款式，再筛扎货
                List<Style> styles = styleMapper.selectList(
                        new LambdaQueryWrapper<Style>().like(Style::getStyleNo, styleNo)
                );
                Set<Long> styleIds = new HashSet<>();
                for (Style s : styles) {
                    styleIds.add(s.getId());
                }
                if (styleIds.isEmpty()) {
                    Page<WorkRecordVO> emptyResult = new Page<>();
                    emptyResult.setCurrent(pageNum);
                    emptyResult.setSize(pageSize);
                    emptyResult.setTotal(0);
                    emptyResult.setRecords(new ArrayList<>());
                    return emptyResult;
                }
                bundleWrapper.in(Bundle::getStyleId, styleIds);
            }
            if (bedNo != null && !bedNo.isEmpty()) {
                // 查询匹配的床号ID
                List<Bed> beds = bedMapper.selectList(
                        new LambdaQueryWrapper<Bed>().eq(Bed::getCompanyId, companyId).like(Bed::getBedNo, bedNo)
                );
                Set<Long> bedIds = new HashSet<>();
                for (Bed bed : beds) {
                    bedIds.add(bed.getId());
                }
                if (bedIds.isEmpty()) {
                    Page<WorkRecordVO> emptyResult = new Page<>();
                    emptyResult.setCurrent(pageNum);
                    emptyResult.setSize(pageSize);
                    emptyResult.setTotal(0);
                    emptyResult.setRecords(new ArrayList<>());
                    return emptyResult;
                }
                bundleWrapper.in(Bundle::getBedId, bedIds);
            }
            if (bundleNo != null && !bundleNo.isEmpty()) {
                bundleWrapper.like(Bundle::getBundleNo, bundleNo);
            }
            List<Bundle> bundles = bundleMapper.selectList(bundleWrapper);
            filterBundleIds = new HashSet<>();
            for (Bundle b : bundles) {
                filterBundleIds.add(b.getId());
            }
            if (filterBundleIds.isEmpty()) {
                Page<WorkRecordVO> emptyResult = new Page<>();
                emptyResult.setCurrent(pageNum);
                emptyResult.setSize(pageSize);
                emptyResult.setTotal(0);
                emptyResult.setRecords(new ArrayList<>());
                return emptyResult;
            }
        }

        // 如果有工序筛选，先查询匹配的工序记录ID
        Set<Long> filterBundleProcessIds = null;
        if (processName != null && !processName.isEmpty()) {
            List<BundleProcess> bps = bundleProcessMapper.selectList(
                    new LambdaQueryWrapper<BundleProcess>().like(BundleProcess::getProcessName, processName)
            );
            filterBundleProcessIds = new HashSet<>();
            for (BundleProcess bp : bps) {
                filterBundleProcessIds.add(bp.getId());
            }
            if (filterBundleProcessIds.isEmpty()) {
                Page<WorkRecordVO> emptyResult = new Page<>();
                emptyResult.setCurrent(pageNum);
                emptyResult.setSize(pageSize);
                emptyResult.setTotal(0);
                emptyResult.setRecords(new ArrayList<>());
                return emptyResult;
            }
        }

        LambdaQueryWrapper<WorkRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkRecord::getCompanyId, companyId);
        if (status != null) {
            wrapper.eq(WorkRecord::getStatus, status);
        }
        if (workerId != null) {
            wrapper.eq(WorkRecord::getWorkerId, workerId);
        }
        if (filterWorkerIds != null) {
            wrapper.in(WorkRecord::getWorkerId, filterWorkerIds);
        }
        if (filterBundleIds != null) {
            wrapper.in(WorkRecord::getBundleId, filterBundleIds);
        }
        if (filterBundleProcessIds != null) {
            wrapper.in(WorkRecord::getBundleProcessId, filterBundleProcessIds);
        }
        // 日期范围筛选
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(WorkRecord::getCreateTime, startDate + "T00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.lt(WorkRecord::getCreateTime, endDate + "T23:59:59");
        }
        wrapper.orderByDesc(WorkRecord::getCreateTime);

        Page<WorkRecord> page = workRecordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 批量查询关联信息
        Set<Long> bundleIds = new HashSet<>();
        Set<Long> workerIds = new HashSet<>();
        for (WorkRecord wr : page.getRecords()) {
            bundleIds.add(wr.getBundleId());
            workerIds.add(wr.getWorkerId());
        }

        // 查询扎货信息
        Map<Long, Bundle> bundleMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            List<Bundle> bundles = bundleMapper.selectList(
                    new LambdaQueryWrapper<Bundle>().in(Bundle::getId, bundleIds).eq(Bundle::getCompanyId, companyId)
            );
            for (Bundle b : bundles) {
                bundleMap.put(b.getId(), b);
            }
        }

        // 查询款式信息
        Set<Long> styleIds = new HashSet<>();
        Set<Long> bedIds = new HashSet<>();
        for (Bundle b : bundleMap.values()) {
            styleIds.add(b.getStyleId());
            if (b.getBedId() != null) {
                bedIds.add(b.getBedId());
            }
        }
        Map<Long, Style> styleMap = new HashMap<>();
        if (!styleIds.isEmpty()) {
            List<Style> styles = styleMapper.selectList(
                    new LambdaQueryWrapper<Style>().in(Style::getId, styleIds)
            );
            for (Style s : styles) {
                styleMap.put(s.getId(), s);
            }
        }

        // 查询床号信息
        Map<Long, Bed> bedMap = new HashMap<>();
        if (!bedIds.isEmpty()) {
            List<Bed> beds = bedMapper.selectList(
                    new LambdaQueryWrapper<Bed>().in(Bed::getId, bedIds).eq(Bed::getCompanyId, companyId)
            );
            for (Bed bed : beds) {
                bedMap.put(bed.getId(), bed);
            }
        }

        // 查询工人信息
        Map<Long, User> userMap = new HashMap<>();
        if (!workerIds.isEmpty()) {
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getId, workerIds)
            );
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        // 查询退回人信息
        Set<Long> returnerIds = new HashSet<>();
        for (WorkRecord wr : page.getRecords()) {
            if (wr.getReturnerId() != null) {
                returnerIds.add(wr.getReturnerId());
            }
        }
        Map<Long, User> returnerMap = new HashMap<>();
        if (!returnerIds.isEmpty()) {
            List<User> returners = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getId, returnerIds)
            );
            for (User u : returners) {
                returnerMap.put(u.getId(), u);
            }
        }

        // 查询每个扎货的工序进度
        Map<Long, int[]> bundleProgressMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            List<BundleProcess> allProcesses = bundleProcessMapper.selectList(
                    new LambdaQueryWrapper<BundleProcess>().in(BundleProcess::getBundleId, bundleIds).eq(BundleProcess::getCompanyId, companyId)
            );
            for (BundleProcess bp : allProcesses) {
                int[] progress = bundleProgressMap.computeIfAbsent(bp.getBundleId(), k -> new int[]{0, 0});
                progress[0]++; // 总工序数
                if (bp.getStatus() == 2) progress[1]++; // 已完成数
            }
        }

        Page<WorkRecordVO> result = new Page<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());

        List<WorkRecordVO> records = new ArrayList<>();
        for (WorkRecord wr : page.getRecords()) {
            WorkRecordVO vo = new WorkRecordVO();
            vo.setId(wr.getId());
            vo.setWorkerId(wr.getWorkerId());
            vo.setBundleId(wr.getBundleId());
            vo.setBundleProcessId(wr.getBundleProcessId());
            vo.setQuantity(wr.getQuantity());
            vo.setPrice(wr.getPrice());
            vo.setAmount(wr.getAmount());
            vo.setStatus(wr.getStatus());
            vo.setWorkerRemark(wr.getWorkerRemark());
            vo.setReviewRemark(wr.getReviewRemark());
            vo.setReviewTime(wr.getReviewTime());
            vo.setReturnReason(wr.getReturnReason());
            vo.setReturnTime(wr.getReturnTime());
            vo.setCreateTime(wr.getCreateTime());

            User user = userMap.get(wr.getWorkerId());
            if (user != null) {
                vo.setWorkerName(user.getRealName());
                vo.setWorkerPhone(user.getPhone());
            }

            User returner = returnerMap.get(wr.getReturnerId());
            if (returner != null) {
                vo.setReturnerName(returner.getRealName());
            }

            Bundle bundle = bundleMap.get(wr.getBundleId());
            if (bundle != null) {
                vo.setBundleNo(bundle.getBundleNo());
                vo.setBundleQuantity(bundle.getQuantity());
                Style style = styleMap.get(bundle.getStyleId());
                if (style != null) {
                    vo.setStyleNo(style.getStyleNo());
                }
                Bed bed = bedMap.get(bundle.getBedId());
                if (bed != null) {
                    vo.setBedNo(bed.getBedNo());
                }
            }

            // 设置工序进度
            int[] progress = bundleProgressMap.get(wr.getBundleId());
            if (progress != null) {
                vo.setTotalProcesses(progress[0]);
                vo.setCompletedProcesses(progress[1]);
            }

            // 查询工序名称
            BundleProcess bp = bundleProcessMapper.selectById(wr.getBundleProcessId());
            if (bp != null && bp.getCompanyId().equals(companyId)) {
                vo.setProcessName(bp.getProcessName());
            }

            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    /**
     * 报工筛选候选项：款号/床号/扎号/工序（按层级联动过滤，去重，id 倒序=新增的在上）
     * 层级：款号 → 床号 → 扎号 → 工序。任一上层选定后，下层只返回该范围候选项。
     */
    public Map<String, List<String>> getFilterOptions(Long companyId, String styleNo, String bedNo, String bundleNo) {
        Map<String, List<String>> options = new HashMap<>();
        boolean hasStyle = styleNo != null && !styleNo.isEmpty();
        boolean hasBed = bedNo != null && !bedNo.isEmpty();
        boolean hasBundle = bundleNo != null && !bundleNo.isEmpty();

        // 款号：始终全部（顶层），按 id 倒序
        options.put("styleNos", distinctDesc(styleMapper.selectList(
                new LambdaQueryWrapper<Style>().eq(Style::getCompanyId, companyId).orderByDesc(Style::getId)
        ), Style::getStyleNo));

        // 选定款号 → styleIds
        List<Long> styleIds = null;
        if (hasStyle) {
            styleIds = styleMapper.selectList(new LambdaQueryWrapper<Style>()
                    .eq(Style::getCompanyId, companyId).eq(Style::getStyleNo, styleNo)
            ).stream().map(Style::getId).toList();
            if (styleIds.isEmpty()) {
                options.put("bedNos", List.of());
                options.put("bundleNos", List.of());
                options.put("processNames", List.of());
                return options;
            }
        }

        // 床号选项：受 styleIds 约束，按 id 倒序
        LambdaQueryWrapper<Bed> bedOpt = new LambdaQueryWrapper<Bed>()
                .eq(Bed::getCompanyId, companyId).orderByDesc(Bed::getId);
        if (styleIds != null) {
            bedOpt.in(Bed::getStyleId, styleIds);
        }
        List<Bed> beds = bedMapper.selectList(bedOpt);
        options.put("bedNos", beds.stream().map(Bed::getBedNo)
                .filter(s -> s != null && !s.isEmpty()).distinct().toList());

        // 选定床号 → bedIds（在 styleIds 约束范围内锁定）
        List<Long> bedIds = null;
        if (hasBed) {
            bedIds = beds.stream().filter(b -> bedNo.equals(b.getBedNo()))
                    .map(Bed::getId).toList();
            if (bedIds.isEmpty()) {
                options.put("bundleNos", List.of());
                options.put("processNames", List.of());
                return options;
            }
        }

        // 扎号选项：受 styleIds/bedIds 约束，按 id 倒序
        LambdaQueryWrapper<Bundle> bundleOpt = new LambdaQueryWrapper<Bundle>()
                .eq(Bundle::getCompanyId, companyId).eq(Bundle::getDeleted, 0).orderByDesc(Bundle::getId);
        if (styleIds != null) {
            bundleOpt.in(Bundle::getStyleId, styleIds);
        }
        if (bedIds != null) {
            bundleOpt.in(Bundle::getBedId, bedIds);
        }
        List<Bundle> bundles = bundleMapper.selectList(bundleOpt);
        options.put("bundleNos", bundles.stream().map(Bundle::getBundleNo)
                .filter(s -> s != null && !s.isEmpty()).distinct().toList());

        // 工序选项：受 bundleIds 约束（选定扎号则锁定，否则用上层范围内的全部扎），按 id 倒序
        LambdaQueryWrapper<BundleProcess> procOpt = new LambdaQueryWrapper<BundleProcess>()
                .eq(BundleProcess::getCompanyId, companyId).orderByDesc(BundleProcess::getId);
        if (hasStyle || hasBed || hasBundle) {
            List<Long> bundleIds;
            if (hasBundle) {
                bundleIds = bundles.stream().filter(b -> bundleNo.equals(b.getBundleNo()))
                        .map(Bundle::getId).toList();
            } else {
                bundleIds = bundles.stream().map(Bundle::getId).toList();
            }
            if (bundleIds.isEmpty()) {
                options.put("processNames", List.of());
                return options;
            }
            procOpt.in(BundleProcess::getBundleId, bundleIds);
        }
        options.put("processNames", bundleProcessMapper.selectList(procOpt).stream()
                .map(BundleProcess::getProcessName).filter(s -> s != null && !s.isEmpty()).distinct().toList());

        return options;
    }

    /** 按 id 倒序查出的实体列表 → 取目标字符串列并去重（保留最新） */
    private <T> List<String> distinctDesc(List<T> list, Function<T, String> mapper) {
        return list.stream().map(mapper).filter(s -> s != null && !s.isEmpty()).distinct().toList();
    }
}

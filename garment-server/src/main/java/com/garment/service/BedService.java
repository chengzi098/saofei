package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.common.TenantContext;
import com.garment.dto.SaveBedDTO;
import com.garment.entity.Bed;
import com.garment.entity.Bundle;
import com.garment.entity.BundleProcess;
import com.garment.entity.Process;
import com.garment.entity.Style;
import com.garment.entity.WorkRecord;
import com.garment.mapper.BedMapper;
import com.garment.mapper.BundleMapper;
import com.garment.mapper.BundleProcessMapper;
import com.garment.mapper.ProcessMapper;
import com.garment.mapper.StyleMapper;
import com.garment.mapper.WorkRecordMapper;
import com.garment.vo.BedVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BedService {

    private final BedMapper bedMapper;
    private final StyleMapper styleMapper;
    private final BundleMapper bundleMapper;
    private final ProcessMapper processMapper;
    private final BundleProcessMapper bundleProcessMapper;
    private final WorkRecordMapper workRecordMapper;

    public BedService(BedMapper bedMapper, StyleMapper styleMapper,
                      BundleMapper bundleMapper, ProcessMapper processMapper,
                      BundleProcessMapper bundleProcessMapper, WorkRecordMapper workRecordMapper) {
        this.bedMapper = bedMapper;
        this.styleMapper = styleMapper;
        this.bundleMapper = bundleMapper;
        this.processMapper = processMapper;
        this.bundleProcessMapper = bundleProcessMapper;
        this.workRecordMapper = workRecordMapper;
    }

    /**
     * 获取款式下的所有床号
     */
    public List<BedVO> listBeds(Long styleId) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 校验款式归属
        Style style = styleMapper.selectById(styleId);
        if (style == null || !style.getCompanyId().equals(companyId)) {
            throw new RuntimeException("款式不存在");
        }

        List<Bed> beds = bedMapper.selectList(
                new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getStyleId, styleId)
                        .eq(Bed::getCompanyId, companyId)
                        .eq(Bed::getDeleted, 0)
                        .orderByAsc(Bed::getCreateTime)
        );

        List<BedVO> result = new ArrayList<>();
        for (Bed bed : beds) {
            BedVO vo = toBedVO(bed);
            // 查询该床号下已分扎数量
            List<Bundle> bundles = bundleMapper.selectList(
                    new LambdaQueryWrapper<Bundle>()
                            .eq(Bundle::getBedId, bed.getId())
                            .eq(Bundle::getCompanyId, companyId)
                            .eq(Bundle::getDeleted, 0)
                            .select(Bundle::getId)
            );
            vo.setBundleCount(bundles.size());

            // 检查是否有报工记录
            boolean hasWorkRecords = false;
            if (!bundles.isEmpty()) {
                List<Long> bundleIds = new ArrayList<>();
                for (Bundle b : bundles) {
                    bundleIds.add(b.getId());
                }
                // 查询这些扎货的工序快照ID
                List<BundleProcess> bps = bundleProcessMapper.selectList(
                        new LambdaQueryWrapper<BundleProcess>()
                                .in(BundleProcess::getBundleId, bundleIds)
                                .eq(BundleProcess::getCompanyId, companyId)
                                .select(BundleProcess::getId)
                );
                if (!bps.isEmpty()) {
                    List<Long> bpIds = new ArrayList<>();
                    for (BundleProcess bp : bps) {
                        bpIds.add(bp.getId());
                    }
                    Long wrCount = workRecordMapper.selectCount(
                            new LambdaQueryWrapper<WorkRecord>()
                                    .in(WorkRecord::getBundleProcessId, bpIds)
                                    .eq(WorkRecord::getCompanyId, companyId)
                    );
                    hasWorkRecords = wrCount > 0;
                }
            }
            vo.setHasWorkRecords(hasWorkRecords);

            result.add(vo);
        }
        return result;
    }

    /**
     * 新增床号（含工序）
     */
    @Transactional
    public BedVO addBed(Long styleId, SaveBedDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 校验款式归属
        Style style = styleMapper.selectById(styleId);
        if (style == null || !style.getCompanyId().equals(companyId)) {
            throw new RuntimeException("款式不存在");
        }

        if (dto.getBedNo() == null || dto.getBedNo().isEmpty()) {
            throw new RuntimeException("床号编号不能为空");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("名称不能为空");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new RuntimeException("件数必须大于0");
        }

        String bedName = dto.getName().trim();
        String bedNo = dto.getBedNo().trim();

        // 检查同款式下床号+名称组合是否重复（床号相同则名称不能相同）
        Long count = bedMapper.selectCount(
                new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getStyleId, styleId)
                        .eq(Bed::getCompanyId, companyId)
                        .eq(Bed::getBedNo, bedNo)
                        .eq(Bed::getName, bedName)
                        .eq(Bed::getDeleted, 0)
        );
        if (count > 0) {
            throw new RuntimeException("该款式下已存在相同的床号和名称");
        }

        Bed bed = new Bed();
        bed.setCompanyId(companyId);
        bed.setStyleId(styleId);
        bed.setBedNo(dto.getBedNo().trim());
        bed.setName(bedName);
        bed.setColors(dto.getColors());
        bed.setQuantity(dto.getQuantity());
        bed.setRemark(dto.getRemark());
        bedMapper.insert(bed);

        // 保存工序
        if (dto.getProcesses() != null && !dto.getProcesses().isEmpty()) {
            for (SaveBedDTO.ProcessItem item : dto.getProcesses()) {
                if (item.getName() == null || item.getName().isEmpty()) {
                    throw new RuntimeException("工序名称不能为空");
                }
                if (item.getPrice() == null) {
                    throw new RuntimeException("计件单价不能为空");
                }
                Process p = new Process();
                p.setStyleId(styleId);
                p.setBedId(bed.getId());
                p.setCompanyId(companyId);
                p.setName(item.getName());
                p.setSeq(item.getSeq() != null ? item.getSeq() : 0);
                p.setPrice(item.getPrice());
                // 如果价格大于0，则锁定价格；否则不锁定（允许后续修改一次）
                p.setPriceLocked(item.getPrice().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
                processMapper.insert(p);
            }
        }

        BedVO vo = toBedVO(bed);
        vo.setBundleCount(0);
        return vo;
    }

    /**
     * 编辑床号（含工序）
     */
    @Transactional
    public void updateBed(Long id, SaveBedDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Bed bed = bedMapper.selectById(id);
        if (bed == null || !bed.getCompanyId().equals(companyId) || bed.getDeleted() == 1) {
            throw new RuntimeException("床号不存在");
        }

        if (dto.getBedNo() != null && !dto.getBedNo().isEmpty()) {
            bed.setBedNo(dto.getBedNo().trim());
        }

        if (dto.getName() != null) {
            if (dto.getName().trim().isEmpty()) {
                throw new RuntimeException("名称不能为空");
            }
            bed.setName(dto.getName().trim());
        }

        // 检查同款式下床号+名称组合是否重复（床号相同则名称不能相同）
        Long count = bedMapper.selectCount(
                new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getStyleId, bed.getStyleId())
                        .eq(Bed::getCompanyId, companyId)
                        .eq(Bed::getBedNo, bed.getBedNo())
                        .eq(Bed::getName, bed.getName())
                        .eq(Bed::getDeleted, 0)
                        .ne(Bed::getId, id)
        );
        if (count > 0) {
            throw new RuntimeException("该款式下已存在相同的床号和名称");
        }
        if (dto.getColors() != null) {
            bed.setColors(dto.getColors());
        }

        if (dto.getQuantity() != null) {
            if (dto.getQuantity() <= 0) {
                throw new RuntimeException("件数必须大于0");
            }
            // 检查不能小于已有扎货总数量
            Long bundleTotal = bundleMapper.selectCount(
                    new LambdaQueryWrapper<Bundle>()
                            .eq(Bundle::getBedId, id)
                            .eq(Bundle::getCompanyId, companyId)
                            .eq(Bundle::getDeleted, 0)
            );
            if (bundleTotal > 0) {
                List<Bundle> bundles = bundleMapper.selectList(
                        new LambdaQueryWrapper<Bundle>()
                                .eq(Bundle::getBedId, id)
                                .eq(Bundle::getCompanyId, companyId)
                                .eq(Bundle::getDeleted, 0)
                                .select(Bundle::getQuantity)
                );
                int totalQty = bundles.stream().mapToInt(Bundle::getQuantity).sum();
                if (dto.getQuantity() < totalQty) {
                    throw new RuntimeException("件数不能小于已有扎货总件数(" + totalQty + ")");
                }
            }
            bed.setQuantity(dto.getQuantity());
        }

        if (dto.getRemark() != null) {
            bed.setRemark(dto.getRemark());
        }
        bedMapper.updateById(bed);

        // 保存工序（先删后插）
        if (dto.getProcesses() != null) {
            // 查询旧工序，用于检查价格锁定状态
            List<Process> oldProcesses = processMapper.selectList(
                    new LambdaQueryWrapper<Process>()
                            .eq(Process::getBedId, id)
                            .eq(Process::getCompanyId, companyId)
                            .eq(Process::getDeleted, 0)
            );

            List<Long> retainedProcessIds = dto.getProcesses().stream()
                    .map(SaveBedDTO.ProcessItem::getId)
                    .filter(processId -> processId != null)
                    .toList();
            for (Process oldProcess : oldProcesses) {
                if (!retainedProcessIds.contains(oldProcess.getId())
                        && hasWorkRecords(id, companyId, oldProcess.getName())) {
                    throw new RuntimeException("工序「" + oldProcess.getName() + "」已有员工报工，不能删除");
                }
            }

            // 检查是否有已锁定价格的工序被修改
            for (SaveBedDTO.ProcessItem item : dto.getProcesses()) {
                if (item.getId() != null) {
                    // 查找对应的旧工序
                    Process oldProcess = oldProcesses.stream()
                            .filter(p -> p.getId().equals(item.getId()))
                            .findFirst()
                            .orElse(null);

                    if (oldProcess != null) {
                        // 价格不为 0，不允许修改（不管 priceLocked 字段）
                        if (oldProcess.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                            if (item.getPrice() == null || item.getPrice().compareTo(oldProcess.getPrice()) != 0) {
                                throw new RuntimeException("工序「" + item.getName() + "」的定价已锁定，不能修改");
                            }
                            // 名称也不能修改
                            if (item.getName() != null && !item.getName().equals(oldProcess.getName())) {
                                throw new RuntimeException("工序「" + oldProcess.getName() + "」已锁定，不能修改名称");
                            }
                        }
                    }
                }
            }

            // 删除旧工序
            for (Process p : oldProcesses) {
                processMapper.deleteById(p.getId());
            }

            // 插入新工序
            for (SaveBedDTO.ProcessItem item : dto.getProcesses()) {
                if (item.getName() == null || item.getName().isEmpty()) {
                    throw new RuntimeException("工序名称不能为空");
                }
                if (item.getPrice() == null) {
                    throw new RuntimeException("计件单价不能为空");
                }
                Process p = new Process();
                p.setStyleId(bed.getStyleId());
                p.setBedId(id);
                p.setCompanyId(companyId);
                p.setName(item.getName());
                p.setSeq(item.getSeq() != null ? item.getSeq() : 0);
                p.setPrice(item.getPrice());

                // 检查是否需要锁定价格
                if (item.getId() != null) {
                    Process oldProcess = oldProcesses.stream()
                            .filter(op -> op.getId().equals(item.getId()))
                            .findFirst()
                            .orElse(null);

                    if (oldProcess != null) {
                        // 原工序价格为0且新价格大于0，锁定价格
                        if (oldProcess.getPrice().compareTo(BigDecimal.ZERO) == 0
                                && item.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                            p.setPriceLocked(1);
                        } else {
                            // 保持原锁定状态
                            p.setPriceLocked(oldProcess.getPriceLocked());
                        }
                    } else {
                        // 新工序，价格大于0则锁定
                        p.setPriceLocked(item.getPrice().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
                    }
                } else {
                    // 新工序，价格大于0则锁定
                    p.setPriceLocked(item.getPrice().compareTo(BigDecimal.ZERO) > 0 ? 1 : 0);
                }

                processMapper.insert(p);
            }

            // 更新"待定价"记录：同步更新 BundleProcess 和 WorkRecord 中单价为0的记录
            updatePendingPriceRecords(id, companyId, dto.getProcesses(), oldProcesses);
        }
    }

    private boolean hasWorkRecords(Long bedId, Long companyId, String processName) {
        List<Bundle> bundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getBedId, bedId)
                        .eq(Bundle::getCompanyId, companyId)
                        .eq(Bundle::getDeleted, 0)
        );
        if (bundles.isEmpty()) {
            return false;
        }

        List<Long> bundleIds = bundles.stream().map(Bundle::getId).toList();
        List<BundleProcess> bundleProcesses = bundleProcessMapper.selectList(
                new LambdaQueryWrapper<BundleProcess>()
                        .in(BundleProcess::getBundleId, bundleIds)
                        .eq(BundleProcess::getCompanyId, companyId)
                        .eq(BundleProcess::getProcessName, processName)
        );
        if (bundleProcesses.isEmpty()) {
            return false;
        }

        List<Long> bundleProcessIds = bundleProcesses.stream().map(BundleProcess::getId).toList();
        return workRecordMapper.selectCount(
                new LambdaQueryWrapper<WorkRecord>()
                        .in(WorkRecord::getBundleProcessId, bundleProcessIds)
                        .eq(WorkRecord::getCompanyId, companyId)
        ) > 0;
    }

    /**
     * 更新待定价记录：同步零价工序的名称，并在定价后更新报工金额
     */
    private void updatePendingPriceRecords(Long bedId, Long companyId,
                                           List<SaveBedDTO.ProcessItem> processes,
                                           List<Process> oldProcesses) {
        // 查询该床号下的所有扎货
        List<Bundle> bundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getBedId, bedId)
                        .eq(Bundle::getCompanyId, companyId)
                        .eq(Bundle::getDeleted, 0)
        );
        if (bundles.isEmpty()) {
            return;
        }

        for (SaveBedDTO.ProcessItem item : processes) {
            Process oldProcess = item.getId() == null ? null : oldProcesses.stream()
                    .filter(process -> process.getId().equals(item.getId()))
                    .findFirst()
                    .orElse(null);
            String previousName = oldProcess != null ? oldProcess.getName() : item.getName();
            boolean becomesPriced = item.getPrice() != null
                    && item.getPrice().compareTo(BigDecimal.ZERO) > 0;

            for (Bundle bundle : bundles) {
                List<BundleProcess> bundleProcesses = bundleProcessMapper.selectList(
                        new LambdaQueryWrapper<BundleProcess>()
                                .eq(BundleProcess::getBundleId, bundle.getId())
                                .eq(BundleProcess::getCompanyId, companyId)
                                .eq(BundleProcess::getProcessName, previousName)
                                .eq(BundleProcess::getPrice, BigDecimal.ZERO)
                );

                for (BundleProcess bp : bundleProcesses) {
                    bp.setProcessName(item.getName());
                    if (becomesPriced) {
                        bp.setPrice(item.getPrice());
                    }
                    bundleProcessMapper.updateById(bp);

                    if (!becomesPriced) {
                        continue;
                    }

                    List<WorkRecord> workRecords = workRecordMapper.selectList(
                            new LambdaQueryWrapper<WorkRecord>()
                                    .eq(WorkRecord::getBundleProcessId, bp.getId())
                                    .eq(WorkRecord::getCompanyId, companyId)
                                    .eq(WorkRecord::getPrice, BigDecimal.ZERO)
                    );
                    for (WorkRecord wr : workRecords) {
                        wr.setPrice(item.getPrice());
                        wr.setAmount(item.getPrice().multiply(BigDecimal.valueOf(wr.getQuantity())));
                        workRecordMapper.updateById(wr);
                    }
                }
            }
        }
    }

    /**
     * 删除床号（有报工记录时不允许删除，无报工记录时级联删除扎货+工序快照+工序模板）
     */
    @Transactional
    public void deleteBed(Long id) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Bed bed = bedMapper.selectById(id);
        if (bed == null || !bed.getCompanyId().equals(companyId) || bed.getDeleted() == 1) {
            throw new RuntimeException("床号不存在");
        }

        // 查询该床号下所有扎货
        List<Bundle> bundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getBedId, id)
                        .eq(Bundle::getCompanyId, companyId)
                        .eq(Bundle::getDeleted, 0)
        );

        // 检查是否有报工记录
        if (!bundles.isEmpty()) {
            List<Long> bundleIds = new ArrayList<>();
            for (Bundle b : bundles) {
                bundleIds.add(b.getId());
            }
            List<BundleProcess> bps = bundleProcessMapper.selectList(
                    new LambdaQueryWrapper<BundleProcess>()
                            .in(BundleProcess::getBundleId, bundleIds)
                            .eq(BundleProcess::getCompanyId, companyId)
                            .select(BundleProcess::getId)
            );
            if (!bps.isEmpty()) {
                List<Long> bpIds = new ArrayList<>();
                for (BundleProcess bp : bps) {
                    bpIds.add(bp.getId());
                }
                Long wrCount = workRecordMapper.selectCount(
                        new LambdaQueryWrapper<WorkRecord>()
                                .in(WorkRecord::getBundleProcessId, bpIds)
                                .eq(WorkRecord::getCompanyId, companyId)
                );
                if (wrCount > 0) {
                    throw new RuntimeException("该床号下已有员工报工，不能删除");
                }
            }

            // 无报工记录，级联删除扎货 + 工序快照
            for (Bundle bundle : bundles) {
                bundleProcessMapper.delete(
                        new LambdaQueryWrapper<BundleProcess>()
                                .eq(BundleProcess::getBundleId, bundle.getId())
                                .eq(BundleProcess::getCompanyId, companyId)
                );
                bundleMapper.deleteById(bundle.getId());
            }
        }

        // 删除床号
        bedMapper.deleteById(id);

        // 删除关联工序模板
        List<Process> processes = processMapper.selectList(
                new LambdaQueryWrapper<Process>()
                        .eq(Process::getBedId, id)
                        .eq(Process::getCompanyId, companyId)
                        .eq(Process::getDeleted, 0)
        );
        for (Process p : processes) {
            processMapper.deleteById(p.getId());
        }
    }

    /**
     * Bed -> BedVO 转换
     */
    private BedVO toBedVO(Bed bed) {
        BedVO vo = new BedVO();
        vo.setId(bed.getId());
        vo.setStyleId(bed.getStyleId());
        vo.setBedNo(bed.getBedNo());
        vo.setName(bed.getName());
        vo.setColors(bed.getColors());
        vo.setQuantity(bed.getQuantity());
        vo.setRemark(bed.getRemark());
        vo.setCreateTime(bed.getCreateTime());

        // 查询该床号下的扎货，计算总件数
        List<Bundle> bundles = bundleMapper.selectList(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getBedId, bed.getId())
                        .eq(Bundle::getCompanyId, bed.getCompanyId())
                        .eq(Bundle::getDeleted, 0)
        );
        int totalBundleQuantity = bundles.stream().mapToInt(Bundle::getQuantity).sum();

        // 查询工序列表
        List<Process> processes = processMapper.selectList(
                new LambdaQueryWrapper<Process>()
                        .eq(Process::getBedId, bed.getId())
                        .eq(Process::getCompanyId, bed.getCompanyId())
                        .eq(Process::getDeleted, 0)
                        .orderByAsc(Process::getSeq)
        );
        vo.setProcessCount(processes.size());
        List<BedVO.ProcessItem> processItems = new ArrayList<>();
        for (Process p : processes) {
            BedVO.ProcessItem item = new BedVO.ProcessItem();
            item.setId(p.getId());
            item.setName(p.getName());
            item.setPrice(p.getPrice());
            item.setSeq(p.getSeq());
            // 价格不为 0 就锁定，或者 priceLocked 字段为 1
            boolean isLocked = p.getPrice().compareTo(BigDecimal.ZERO) > 0
                    || (p.getPriceLocked() != null && p.getPriceLocked() == 1);
            item.setPriceLocked(isLocked);
            // 设置总数量（扎货总件数）
            item.setTotalQuantity(totalBundleQuantity);

            // 查询该工序的报工数量
            int completedQty = 0;
            if (!bundles.isEmpty()) {
                List<Long> bundleIds = bundles.stream().map(Bundle::getId).toList();
                // 查询该工序名称对应的 BundleProcess
                List<BundleProcess> bundleProcesses = bundleProcessMapper.selectList(
                        new LambdaQueryWrapper<BundleProcess>()
                                .in(BundleProcess::getBundleId, bundleIds)
                                .eq(BundleProcess::getCompanyId, bed.getCompanyId())
                                .eq(BundleProcess::getProcessName, p.getName())
                );
                if (!bundleProcesses.isEmpty()) {
                    List<Long> bpIds = bundleProcesses.stream().map(BundleProcess::getId).toList();
                    // 查询这些工序的报工记录总数
                    List<WorkRecord> workRecords = workRecordMapper.selectList(
                            new LambdaQueryWrapper<WorkRecord>()
                                    .in(WorkRecord::getBundleProcessId, bpIds)
                                    .eq(WorkRecord::getCompanyId, bed.getCompanyId())
                    );
                    completedQty = workRecords.stream().mapToInt(WorkRecord::getQuantity).sum();

                    // 计算效率：找出报工数量最多的一天
                    if (!workRecords.isEmpty()) {
                        // 按日期分组
                        Map<LocalDate, List<WorkRecord>> dayRecords = workRecords.stream()
                                .filter(wr -> wr.getCreateTime() != null)
                                .collect(Collectors.groupingBy(wr -> wr.getCreateTime().toLocalDate()));

                        // 找出报工数量最多的一天
                        LocalDate bestDay = null;
                        int bestDayQty = 0;
                        for (Map.Entry<LocalDate, List<WorkRecord>> entry : dayRecords.entrySet()) {
                            int dayQty = entry.getValue().stream().mapToInt(WorkRecord::getQuantity).sum();
                            if (dayQty > bestDayQty) {
                                bestDayQty = dayQty;
                                bestDay = entry.getKey();
                            }
                        }

                        // 计算该天的工作时长和平均速度
                        if (bestDay != null && bestDayQty > 0) {
                            List<WorkRecord> bestDayRecords = dayRecords.get(bestDay);
                            if (bestDayRecords.size() >= 2) {
                                // 找出最早和最晚的报工时间
                                LocalDateTime earliest = bestDayRecords.stream()
                                        .map(WorkRecord::getCreateTime)
                                        .min(LocalDateTime::compareTo)
                                        .orElse(null);
                                LocalDateTime latest = bestDayRecords.stream()
                                        .map(WorkRecord::getCreateTime)
                                        .max(LocalDateTime::compareTo)
                                        .orElse(null);

                                if (earliest != null && latest != null) {
                                    long minutes = java.time.Duration.between(earliest, latest).toMinutes();
                                    double hours = minutes / 60.0;

                                    // 如果时间差太小（小于10分钟），认为数据不足
                                    if (minutes >= 10) {
                                        item.setDayQuantity(bestDayQty);
                                        item.setDayHours(Math.round(hours * 10.0) / 10.0);
                                        item.setSpeedPerHour(Math.round(bestDayQty / hours * 10.0) / 10.0);
                                    }
                                }
                            }
                            // 只有一条记录，不设置效率数据
                        }
                    }
                }
            }
            item.setCompletedQuantity(completedQty);
            processItems.add(item);
        }
        vo.setProcesses(processItems);

        return vo;
    }
}

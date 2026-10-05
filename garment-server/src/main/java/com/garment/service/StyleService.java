package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.TenantContext;
import com.garment.dto.SaveStyleDTO;
import com.garment.entity.Bed;
import com.garment.entity.Bundle;
import com.garment.entity.Process;
import com.garment.entity.Style;
import com.garment.mapper.*;
import com.garment.vo.*;
import com.garment.entity.BundleProcess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StyleService {

    private final StyleMapper styleMapper;
    private final BedMapper bedMapper;
    private final ProcessMapper processMapper;
    private final BundleMapper bundleMapper;
    private final BundleProcessMapper bundleProcessMapper;

    public StyleService(StyleMapper styleMapper, BedMapper bedMapper,
                        ProcessMapper processMapper, BundleMapper bundleMapper,
                        BundleProcessMapper bundleProcessMapper) {
        this.styleMapper = styleMapper;
        this.bedMapper = bedMapper;
        this.processMapper = processMapper;
        this.bundleMapper = bundleMapper;
        this.bundleProcessMapper = bundleProcessMapper;
    }

    /**
     * 分页查询款式列表（含床号数量）
     */
    public Page<StyleVO> listStyles(int pageNum, int pageSize, String keyword, String startDate, String endDate) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        LambdaQueryWrapper<Style> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Style::getCompanyId, companyId);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Style::getStyleNo, keyword);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(Style::getCreateTime, startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(Style::getCreateTime, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(Style::getCreateTime);

        Page<Style> page = styleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 批量查询床号数量
        List<Long> styleIds = page.getRecords().stream().map(Style::getId).toList();
        Map<Long, Integer> bedCountMap = new HashMap<>();
        Map<Long, Boolean> zeroPriceMap = new HashMap<>();
        if (!styleIds.isEmpty()) {
            List<Bed> beds = bedMapper.selectList(
                    new LambdaQueryWrapper<Bed>()
                            .in(Bed::getStyleId, styleIds)
                            .eq(Bed::getCompanyId, companyId)
                            .eq(Bed::getDeleted, 0)
            );
            for (Bed b : beds) {
                bedCountMap.merge(b.getStyleId(), 1, Integer::sum);
            }

            // 批量查询工序，检查是否有未定价的工序
            List<Long> bedIds = beds.stream().map(Bed::getId).toList();
            if (!bedIds.isEmpty()) {
                List<Process> processes = processMapper.selectList(
                        new LambdaQueryWrapper<Process>()
                                .in(Process::getBedId, bedIds)
                                .eq(Process::getCompanyId, companyId)
                                .eq(Process::getDeleted, 0)
                );

                // 按 bedId 分组工序
                Map<Long, List<Process>> bedProcessMap = processes.stream()
                        .collect(Collectors.groupingBy(Process::getBedId));

                // 检查每个款式是否有未定价工序
                for (Bed b : beds) {
                    List<Process> bedProcesses = bedProcessMap.getOrDefault(b.getId(), Collections.emptyList());
                    boolean hasZero = bedProcesses.stream()
                            .anyMatch(p -> p.getPrice() == null || p.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0);
                    if (hasZero) {
                        zeroPriceMap.put(b.getStyleId(), true);
                    }
                }
            }
        }

        Page<StyleVO> result = new Page<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());

        List<StyleVO> records = new ArrayList<>();
        for (Style s : page.getRecords()) {
            StyleVO vo = new StyleVO();
            vo.setId(s.getId());
            vo.setStyleNo(s.getStyleNo());
            vo.setName(s.getName());
            vo.setBedCount(bedCountMap.getOrDefault(s.getId(), 0));
            vo.setCreateTime(s.getCreateTime());
            vo.setHasZeroPriceProcess(zeroPriceMap.getOrDefault(s.getId(), false));
            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    /**
     * 新增款式
     */
    @Transactional
    public Long addStyle(SaveStyleDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        if (dto.getStyleNo() == null || dto.getStyleNo().isEmpty()) {
            throw new RuntimeException("款号不能为空");
        }

        // 检查同公司下款号是否重复
        Long count = styleMapper.selectCount(
                new LambdaQueryWrapper<Style>()
                        .eq(Style::getCompanyId, companyId)
                        .eq(Style::getStyleNo, dto.getStyleNo())
        );
        if (count > 0) {
            throw new RuntimeException("款号已存在");
        }

        Style style = new Style();
        style.setCompanyId(companyId);
        style.setStyleNo(dto.getStyleNo());
        style.setName(dto.getStyleNo()); // 使用款号作为名称的默认值
        styleMapper.insert(style);

        return style.getId();
    }

    /**
     * 编辑款式
     */
    @Transactional
    public void updateStyle(Long id, SaveStyleDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Style style = styleMapper.selectById(id);
        if (style == null || !style.getCompanyId().equals(companyId)) {
            throw new RuntimeException("款式不存在");
        }

        if (dto.getStyleNo() != null && !dto.getStyleNo().isEmpty()) {
            // 检查款号是否与其他款式重复
            if (!dto.getStyleNo().equals(style.getStyleNo())) {
                Long count = styleMapper.selectCount(
                        new LambdaQueryWrapper<Style>()
                                .eq(Style::getCompanyId, companyId)
                                .eq(Style::getStyleNo, dto.getStyleNo())
                );
                if (count > 0) {
                    throw new RuntimeException("款号已存在");
                }
            }
            style.setStyleNo(dto.getStyleNo());
        }
        styleMapper.updateById(style);
    }

    /**
     * 删除款式（级联删除床号、工序、扎货）
     */
    @Transactional
    public void deleteStyle(Long id) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Style style = styleMapper.selectById(id);
        if (style == null || !style.getCompanyId().equals(companyId)) {
            throw new RuntimeException("款式不存在");
        }

        // 删除款式
        styleMapper.deleteById(id);

        // 删除关联床号
        List<Bed> beds = bedMapper.selectList(
                new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getStyleId, id)
                        .eq(Bed::getCompanyId, companyId)
                        .eq(Bed::getDeleted, 0)
        );
        for (Bed b : beds) {
            bedMapper.deleteById(b.getId());
            // 删除床号下的工序
            List<Process> processes = processMapper.selectList(
                    new LambdaQueryWrapper<Process>()
                            .eq(Process::getBedId, b.getId())
                            .eq(Process::getCompanyId, companyId)
                            .eq(Process::getDeleted, 0)
            );
            for (Process p : processes) {
                processMapper.deleteById(p.getId());
            }
        }
    }

    /**
     * 归档款式
     */
    public void archiveStyle(Long id) {
        Long companyId = TenantContext.getCompanyId();
        Style style = styleMapper.selectById(id);
        if (style == null || !style.getCompanyId().equals(companyId)) {
            throw new RuntimeException("款式不存在");
        }
        style.setArchived(1);
        styleMapper.updateById(style);
    }

    /**
     * 取消归档
     */
    public void unarchiveStyle(Long id) {
        Long companyId = TenantContext.getCompanyId();
        Style style = styleMapper.selectById(id);
        if (style == null || !style.getCompanyId().equals(companyId)) {
            throw new RuntimeException("款式不存在");
        }
        style.setArchived(0);
        styleMapper.updateById(style);
    }

    /**
     * 查询已归档款式列表（含进度信息，复用看板数据结构）
     */
    public List<DashboardStyleVO> getArchivedStyles() {
        Long companyId = TenantContext.getCompanyId();

        // 查已归档的款式
        LambdaQueryWrapper<Style> styleWrapper = new LambdaQueryWrapper<>();
        styleWrapper.eq(Style::getCompanyId, companyId).eq(Style::getArchived, 1);
        List<Style> archivedStyles = styleMapper.selectList(styleWrapper);
        if (archivedStyles.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> styleIds = archivedStyles.stream().map(Style::getId).collect(Collectors.toSet());
        Map<Long, Style> styleMap = archivedStyles.stream()
                .collect(Collectors.toMap(Style::getId, s -> s));

        // 查关联 bundle
        LambdaQueryWrapper<Bundle> bundleWrapper = new LambdaQueryWrapper<>();
        bundleWrapper.eq(Bundle::getCompanyId, companyId).eq(Bundle::getDeleted, 0).in(Bundle::getStyleId, styleIds);
        List<Bundle> allBundles = bundleMapper.selectList(bundleWrapper);

        // 查关联 bed
        Set<Long> bedIds = allBundles.stream().map(Bundle::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Bed> bedMap = new HashMap<>();
        if (!bedIds.isEmpty()) {
            bedMap = bedMapper.selectBatchIds(bedIds).stream()
                    .collect(Collectors.toMap(Bed::getId, b -> b));
        }

        // 查所有工序快照
        Set<Long> bundleIds = allBundles.stream().map(Bundle::getId).collect(Collectors.toSet());
        Map<Long, List<BundleProcess>> processMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            LambdaQueryWrapper<BundleProcess> processWrapper = new LambdaQueryWrapper<>();
            processWrapper.in(BundleProcess::getBundleId, bundleIds);
            List<BundleProcess> allProcesses = bundleProcessMapper.selectList(processWrapper);
            processMap = allProcesses.stream()
                    .collect(Collectors.groupingBy(BundleProcess::getBundleId));
        }

        // 按 style 分组 bundle
        Map<Long, List<Bundle>> styleBundleMap = allBundles.stream()
                .collect(Collectors.groupingBy(Bundle::getStyleId));

        List<DashboardStyleVO> result = new ArrayList<>();
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

                int bedProgress = 0;
                List<DashboardBundleVO> bundleVOs = new ArrayList<>();

                for (Bundle bundle : bedBundles) {
                    List<BundleProcess> processes = processMap.getOrDefault(bundle.getId(), Collections.emptyList());
                    int completed = (int) processes.stream().filter(p -> p.getStatus() == 2).count();
                    int bundleProgress = processes.size() > 0 ? Math.round(completed * 100f / processes.size()) : 0;
                    bedProgress += bundleProgress;
                    totalBundleProgress += bundleProgress;
                    bundleCount++;

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
                bedVO.setProgressPercent(bedBundles.size() > 0 ? Math.round(bedProgress * 1f / bedBundles.size()) : 0);
                bedVO.setBundles(bundleVOs);
                bedVOs.add(bedVO);
            }

            // 处理没有 bed_id 的 bundle
            List<Bundle> noBedBundles = styleBundles.stream()
                    .filter(b -> b.getBedId() == null)
                    .collect(Collectors.toList());
            for (Bundle bundle : noBedBundles) {
                List<BundleProcess> processes = processMap.getOrDefault(bundle.getId(), Collections.emptyList());
                int completed = (int) processes.stream().filter(p -> p.getStatus() == 2).count();
                int bundleProgress = processes.size() > 0 ? Math.round(completed * 100f / processes.size()) : 0;
                totalBundleProgress += bundleProgress;
                bundleCount++;
            }

            styleVO.setProgressPercent(bundleCount > 0 ? Math.round(totalBundleProgress * 1f / bundleCount) : 0);
            styleVO.setBeds(bedVOs);
            result.add(styleVO);
        }

        // 按归档时间倒序（updateTime 倒序）
        result.sort((a, b) -> {
            Style sa = styleMap.get(a.getStyleId());
            Style sb = styleMap.get(b.getStyleId());
            if (sa == null || sb == null) return 0;
            if (sa.getUpdateTime() == null || sb.getUpdateTime() == null) return 0;
            return sb.getUpdateTime().compareTo(sa.getUpdateTime());
        });

        return result;
    }
}

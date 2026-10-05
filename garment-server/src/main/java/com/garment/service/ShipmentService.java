package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.TenantContext;
import com.garment.dto.ShipBundleDTO;
import com.garment.entity.Bed;
import com.garment.entity.Bundle;
import com.garment.entity.BundleProcess;
import com.garment.entity.Style;
import com.garment.entity.User;
import com.garment.mapper.BedMapper;
import com.garment.mapper.BundleMapper;
import com.garment.mapper.BundleProcessMapper;
import com.garment.mapper.StyleMapper;
import com.garment.mapper.UserMapper;
import com.garment.vo.ShipmentItemVO;
import com.garment.vo.ShipmentSummaryVO;
import com.garment.vo.UnshippedBundleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 出货业务（主管扫码标记出货 / 老板每日出货查看）
 */
@Service
public class ShipmentService {

    private final BundleMapper bundleMapper;
    private final StyleMapper styleMapper;
    private final BedMapper bedMapper;
    private final UserMapper userMapper;
    private final BundleProcessMapper bundleProcessMapper;

    public ShipmentService(BundleMapper bundleMapper, StyleMapper styleMapper,
                           BedMapper bedMapper, UserMapper userMapper,
                           BundleProcessMapper bundleProcessMapper) {
        this.bundleMapper = bundleMapper;
        this.styleMapper = styleMapper;
        this.bedMapper = bedMapper;
        this.userMapper = userMapper;
        this.bundleProcessMapper = bundleProcessMapper;
    }

    /**
     * 标记一扎为已出货（前置条件：扎货所有工序已完成）
     */
    @Transactional
    public void ship(ShipBundleDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Bundle bundle = bundleMapper.selectById(dto.getBundleId());
        if (bundle == null || !bundle.getCompanyId().equals(companyId) || bundle.getDeleted() == 1) {
            throw new RuntimeException("扎货不存在");
        }
        if (!Integer.valueOf(2).equals(bundle.getStatus())) {
            throw new RuntimeException("扎货未全部完成，无法出货");
        }
        if (Integer.valueOf(1).equals(bundle.getShipped())) {
            throw new RuntimeException("该扎已出货");
        }

        bundle.setShipped(1);
        bundle.setShippedTime(LocalDateTime.now());
        bundle.setShipperId(userId);
        bundleMapper.updateById(bundle);
    }

    /**
     * 撤销出货
     */
    @Transactional
    public void unship(ShipBundleDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Bundle bundle = bundleMapper.selectById(dto.getBundleId());
        if (bundle == null || !bundle.getCompanyId().equals(companyId) || bundle.getDeleted() == 1) {
            throw new RuntimeException("扎货不存在");
        }
        if (!Integer.valueOf(1).equals(bundle.getShipped())) {
            throw new RuntimeException("该扎未出货，无需撤销");
        }

        bundle.setShipped(0);
        bundle.setShippedTime(null);
        bundle.setShipperId(null);
        bundleMapper.updateById(bundle);
    }

    /**
     * 每日出货明细分页（按日期范围筛选）
     */
    public Page<ShipmentItemVO> listDaily(String startDate, String endDate, int pageNum, int pageSize) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        LambdaQueryWrapper<Bundle> wrapper = new LambdaQueryWrapper<Bundle>()
                .eq(Bundle::getCompanyId, companyId)
                .eq(Bundle::getDeleted, 0)
                .eq(Bundle::getShipped, 1);

        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        if (start != null) {
            wrapper.ge(Bundle::getShippedTime, start);
        }
        if (end != null) {
            wrapper.le(Bundle::getShippedTime, end);
        }
        wrapper.orderByDesc(Bundle::getShippedTime);

        Page<Bundle> page = bundleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 批量查关联数据，避免 N+1
        Set<Long> styleIds = new HashSet<>();
        Set<Long> bedIds = new HashSet<>();
        Set<Long> shipperIds = new HashSet<>();
        for (Bundle b : page.getRecords()) {
            if (b.getStyleId() != null) styleIds.add(b.getStyleId());
            if (b.getBedId() != null) bedIds.add(b.getBedId());
            if (b.getShipperId() != null) shipperIds.add(b.getShipperId());
        }

        Map<Long, Style> styleMap = batchQueryStyles(styleIds);
        Map<Long, Bed> bedMap = batchQueryBeds(bedIds);
        Map<Long, User> userMap = batchQueryUsers(shipperIds);

        Page<ShipmentItemVO> result = new Page<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());

        List<ShipmentItemVO> records = new ArrayList<>();
        for (Bundle b : page.getRecords()) {
            records.add(toItemVO(b, styleMap, bedMap, userMap));
        }
        result.setRecords(records);
        return result;
    }

    /**
     * 出货汇总（按日期范围）
     */
    public ShipmentSummaryVO summary(String startDate, String endDate) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        LambdaQueryWrapper<Bundle> wrapper = new LambdaQueryWrapper<Bundle>()
                .eq(Bundle::getCompanyId, companyId)
                .eq(Bundle::getDeleted, 0)
                .eq(Bundle::getShipped, 1);

        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        if (start != null) {
            wrapper.ge(Bundle::getShippedTime, start);
        }
        if (end != null) {
            wrapper.le(Bundle::getShippedTime, end);
        }

        List<Bundle> list = bundleMapper.selectList(wrapper);

        ShipmentSummaryVO vo = new ShipmentSummaryVO();
        vo.setTotalBundles(list.size());
        long qty = 0;
        Set<Long> styleSet = new HashSet<>();
        Set<Long> shipperSet = new HashSet<>();
        for (Bundle b : list) {
            if (b.getQuantity() != null) qty += b.getQuantity();
            if (b.getStyleId() != null) styleSet.add(b.getStyleId());
            if (b.getShipperId() != null) shipperSet.add(b.getShipperId());
        }
        vo.setTotalQuantity(qty);
        vo.setStyleCount(styleSet.size());
        vo.setShipperCount(shipperSet.size());
        return vo;
    }

    /**
     * 未出货扎货清单（含生产进度）
     * 排序：已完成的排前面（该出未出优先看到），其次按建扎时间倒序
     */
    public List<UnshippedBundleVO> listUnshipped(Long styleId, Long bedId) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        LambdaQueryWrapper<Bundle> wrapper = new LambdaQueryWrapper<Bundle>()
                .eq(Bundle::getCompanyId, companyId)
                .eq(Bundle::getDeleted, 0)
                .eq(Bundle::getShipped, 0);
        if (styleId != null) {
            wrapper.eq(Bundle::getStyleId, styleId);
        }
        if (bedId != null) {
            wrapper.eq(Bundle::getBedId, bedId);
        }
        // 已完成(status=2)在前，再按建扎时间倒序
        wrapper.orderByDesc(Bundle::getStatus).orderByDesc(Bundle::getCreateTime);

        List<Bundle> bundles = bundleMapper.selectList(wrapper);
        if (bundles.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查款式/床号
        Set<Long> styleIds = new HashSet<>();
        Set<Long> bedIds = new HashSet<>();
        Set<Long> bundleIds = new HashSet<>();
        for (Bundle b : bundles) {
            if (b.getStyleId() != null) styleIds.add(b.getStyleId());
            if (b.getBedId() != null) bedIds.add(b.getBedId());
            bundleIds.add(b.getId());
        }
        Map<Long, Style> styleMap = batchQueryStyles(styleIds);
        Map<Long, Bed> bedMap = batchQueryBeds(bedIds);

        // 批量查工序，按 bundleId 分组统计
        Map<Long, List<BundleProcess>> processMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            List<BundleProcess> all = bundleProcessMapper.selectList(
                    new LambdaQueryWrapper<BundleProcess>()
                            .eq(BundleProcess::getCompanyId, companyId)
                            .in(BundleProcess::getBundleId, bundleIds)
            );
            for (BundleProcess bp : all) {
                processMap.computeIfAbsent(bp.getBundleId(), k -> new ArrayList<>()).add(bp);
            }
        }

        List<UnshippedBundleVO> result = new ArrayList<>();
        for (Bundle b : bundles) {
            UnshippedBundleVO vo = new UnshippedBundleVO();
            vo.setId(b.getId());
            vo.setBundleNo(b.getBundleNo());
            vo.setSize(b.getSize());
            vo.setQuantity(b.getQuantity());
            vo.setStyleId(b.getStyleId());
            vo.setBedId(b.getBedId());
            vo.setStatus(b.getStatus());
            vo.setCreateTime(b.getCreateTime());

            Style style = styleMap.get(b.getStyleId());
            if (style != null) {
                vo.setStyleNo(style.getStyleNo());
                vo.setStyleName(style.getName());
            }
            if (b.getBedId() != null) {
                Bed bed = bedMap.get(b.getBedId());
                if (bed != null) {
                    vo.setBedNo(bed.getBedNo());
                }
            }

            List<BundleProcess> ps = processMap.getOrDefault(b.getId(), Collections.emptyList());
            vo.setTotalProcesses(ps.size());
            int done = 0;
            for (BundleProcess bp : ps) {
                if (bp.getStatus() != null && bp.getStatus() == 2) done++;
            }
            vo.setCompletedProcesses(done);

            result.add(vo);
        }
        return result;
    }

    private ShipmentItemVO toItemVO(Bundle b, Map<Long, Style> styleMap, Map<Long, Bed> bedMap, Map<Long, User> userMap) {
        ShipmentItemVO vo = new ShipmentItemVO();
        vo.setId(b.getId());
        vo.setBundleId(b.getId());
        vo.setBundleNo(b.getBundleNo());
        vo.setSize(b.getSize());
        vo.setQuantity(b.getQuantity());
        vo.setStyleId(b.getStyleId());
        vo.setBedId(b.getBedId());
        vo.setShippedTime(b.getShippedTime());

        Style style = styleMap.get(b.getStyleId());
        if (style != null) {
            vo.setStyleNo(style.getStyleNo());
            vo.setStyleName(style.getName());
        }
        if (b.getBedId() != null) {
            Bed bed = bedMap.get(b.getBedId());
            if (bed != null) {
                vo.setBedNo(bed.getBedNo());
            }
        }
        if (b.getShipperId() != null) {
            User shipper = userMap.get(b.getShipperId());
            if (shipper != null) {
                vo.setShipperName(shipper.getRealName());
            }
        }
        return vo;
    }

    private Map<Long, Style> batchQueryStyles(Set<Long> ids) {
        Map<Long, Style> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        for (Style s : styleMapper.selectList(new LambdaQueryWrapper<Style>().in(Style::getId, ids))) {
            map.put(s.getId(), s);
        }
        return map;
    }

    private Map<Long, Bed> batchQueryBeds(Set<Long> ids) {
        Map<Long, Bed> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        for (Bed bed : bedMapper.selectList(new LambdaQueryWrapper<Bed>().in(Bed::getId, ids))) {
            map.put(bed.getId(), bed);
        }
        return map;
    }

    private Map<Long, User> batchQueryUsers(Set<Long> ids) {
        Map<Long, User> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) return map;
        for (User u : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, ids))) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private LocalDateTime parseStart(String date) {
        if (date == null || date.trim().isEmpty()) return null;
        return LocalDate.parse(date.trim()).atStartOfDay();
    }

    private LocalDateTime parseEnd(String date) {
        if (date == null || date.trim().isEmpty()) return null;
        return LocalDate.parse(date.trim()).atTime(23, 59, 59);
    }
}

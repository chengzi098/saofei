package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.garment.common.TenantContext;
import com.garment.dto.SaveBundleItemDTO;
import com.garment.dto.UpdateBundleDTO;
import com.garment.entity.Bed;
import com.garment.entity.Bundle;
import com.garment.entity.BundleProcess;
import com.garment.entity.Style;
import com.garment.entity.User;
import com.garment.entity.UserCompany;
import com.garment.entity.WorkRecord;
import com.garment.mapper.BedMapper;
import com.garment.mapper.BundleMapper;
import com.garment.mapper.BundleProcessMapper;
import com.garment.mapper.ProcessMapper;
import com.garment.mapper.StyleMapper;
import com.garment.mapper.UserCompanyMapper;
import com.garment.mapper.UserMapper;
import com.garment.mapper.WorkRecordMapper;
import com.garment.vo.BundleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BundleService {

    private final BundleMapper bundleMapper;
    private final BundleProcessMapper bundleProcessMapper;
    private final StyleMapper styleMapper;
    private final ProcessMapper processMapper;
    private final UserMapper userMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final BedMapper bedMapper;
    private final WorkRecordMapper workRecordMapper;

    public BundleService(BundleMapper bundleMapper, BundleProcessMapper bundleProcessMapper,
                         StyleMapper styleMapper, ProcessMapper processMapper,
                         UserMapper userMapper, UserCompanyMapper userCompanyMapper,
                         BedMapper bedMapper, WorkRecordMapper workRecordMapper) {
        this.bundleMapper = bundleMapper;
        this.bundleProcessMapper = bundleProcessMapper;
        this.styleMapper = styleMapper;
        this.processMapper = processMapper;
        this.userMapper = userMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.bedMapper = bedMapper;
        this.workRecordMapper = workRecordMapper;
    }

    /**
     * 获取当前用户的扎号前缀
     */
    public String getMyPrefix() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null || userId == null) {
            return "A";
        }
        UserCompany uc = userCompanyMapper.selectOne(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getUserId, userId)
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (uc != null && uc.getBundlePrefix() != null) {
            return uc.getBundlePrefix();
        }
        return "A";
    }

    /**
     * 扎货列表（分页，按款式/床号筛选）
     */
    public Page<BundleVO> listBundles(int pageNum, int pageSize, Long styleId, Long bedId, String bundleNo, String keyword, Integer printed) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        LambdaQueryWrapper<Bundle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bundle::getCompanyId, companyId)
                .eq(Bundle::getDeleted, 0);
        if (bedId != null) {
            wrapper.eq(Bundle::getBedId, bedId);
        } else if (styleId != null) {
            wrapper.eq(Bundle::getStyleId, styleId);
        }
        if (bundleNo != null && !bundleNo.trim().isEmpty()) {
            wrapper.like(Bundle::getBundleNo, bundleNo.trim());
        }
        if (printed != null) {
            wrapper.eq(Bundle::getPrinted, printed);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            // 跨表模糊：款号 / 床号 / 名称 / 扎号
            Set<Long> matchStyleIds = new HashSet<>();
            List<Style> styles = styleMapper.selectList(
                    new LambdaQueryWrapper<Style>().like(Style::getStyleNo, kw)
            );
            for (Style s : styles) {
                matchStyleIds.add(s.getId());
            }
            Set<Long> matchBedIds = new HashSet<>();
            List<Bed> beds = bedMapper.selectList(
                    new LambdaQueryWrapper<Bed>()
                            .like(Bed::getBedNo, kw)
                            .or().like(Bed::getName, kw)
            );
            for (Bed b : beds) {
                matchBedIds.add(b.getId());
            }
            final Set<Long> finalStyleIds = matchStyleIds;
            final Set<Long> finalBedIds = matchBedIds;
            wrapper.and(w -> {
                w.like(Bundle::getBundleNo, kw);
                if (!finalStyleIds.isEmpty()) {
                    w.or().in(Bundle::getStyleId, finalStyleIds);
                }
                if (!finalBedIds.isEmpty()) {
                    w.or().in(Bundle::getBedId, finalBedIds);
                }
            });
        }
        wrapper.orderByDesc(Bundle::getCreateTime);

        Page<Bundle> page = bundleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 查询款式信息
        Set<Long> styleIds = page.getRecords().stream().map(Bundle::getStyleId).collect(java.util.stream.Collectors.toSet());
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
        Set<Long> bedIds = new HashSet<>();
        for (Bundle b : page.getRecords()) {
            if (b.getBedId() != null) {
                bedIds.add(b.getBedId());
            }
        }
        Map<Long, Bed> bedMap = new HashMap<>();
        if (!bedIds.isEmpty()) {
            List<Bed> beds = bedMapper.selectList(
                    new LambdaQueryWrapper<Bed>().in(Bed::getId, bedIds)
            );
            for (Bed bed : beds) {
                bedMap.put(bed.getId(), bed);
            }
        }

        Page<BundleVO> result = new Page<>();
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());

        List<BundleVO> records = new ArrayList<>();
        for (Bundle b : page.getRecords()) {
            BundleVO vo = toBundleVO(b, styleMap, bedMap);

            // 查询工序快照
            List<BundleProcess> processes = bundleProcessMapper.selectList(
                    new LambdaQueryWrapper<BundleProcess>()
                            .eq(BundleProcess::getBundleId, b.getId())
                            .eq(BundleProcess::getCompanyId, b.getCompanyId())
                            .orderByAsc(BundleProcess::getSeq)
            );
            vo.setProcesses(buildProcessItems(processes));

            records.add(vo);
        }
        result.setRecords(records);
        return result;
    }

    /**
     * 获取扎货详情（含工序快照）
     */
    public BundleVO getBundleDetail(Long id) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Bundle bundle = bundleMapper.selectById(id);
        if (bundle == null || !bundle.getCompanyId().equals(companyId) || bundle.getDeleted() == 1) {
            throw new RuntimeException("扎货不存在");
        }

        Map<Long, Style> styleMap = new HashMap<>();
        Style style = styleMapper.selectById(bundle.getStyleId());
        if (style != null) {
            styleMap.put(style.getId(), style);
        }

        Map<Long, Bed> bedMap = new HashMap<>();
        if (bundle.getBedId() != null) {
            Bed bed = bedMapper.selectById(bundle.getBedId());
            if (bed != null) {
                bedMap.put(bed.getId(), bed);
            }
        }

        BundleVO vo = toBundleVO(bundle, styleMap, bedMap);

        List<BundleProcess> processes = bundleProcessMapper.selectList(
                new LambdaQueryWrapper<BundleProcess>()
                        .eq(BundleProcess::getBundleId, bundle.getId())
                        .orderByAsc(BundleProcess::getSeq)
        );
        vo.setProcesses(buildProcessItems(processes));

        return vo;
    }

    /**
     * 扫码查询扎货信息（工人/主管扫码用）
     */
    public BundleVO scanBundle(String qrCode) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Bundle bundle = bundleMapper.selectOne(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getQrCode, qrCode)
                        .eq(Bundle::getCompanyId, companyId)
                        .eq(Bundle::getDeleted, 0)
        );
        if (bundle == null) {
            throw new RuntimeException("二维码无效");
        }

        Map<Long, Style> styleMap = new HashMap<>();
        Style style = styleMapper.selectById(bundle.getStyleId());
        if (style != null) {
            styleMap.put(style.getId(), style);
        }

        Map<Long, Bed> bedMap = new HashMap<>();
        if (bundle.getBedId() != null) {
            Bed bed = bedMapper.selectById(bundle.getBedId());
            if (bed != null) {
                bedMap.put(bed.getId(), bed);
            }
        }

        BundleVO vo = toBundleVO(bundle, styleMap, bedMap);

        List<BundleProcess> processes = bundleProcessMapper.selectList(
                new LambdaQueryWrapper<BundleProcess>()
                        .eq(BundleProcess::getBundleId, bundle.getId())
                        .orderByAsc(BundleProcess::getSeq)
        );
        vo.setProcesses(buildProcessItems(processes));

        return vo;
    }

    /**
     * 构建工序列表，对已完成的工序附带报工记录信息
     */
    private List<BundleVO.ProcessItem> buildProcessItems(List<BundleProcess> processes) {
        if (processes == null || processes.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集已完成的工序ID
        List<Long> completedProcessIds = new ArrayList<>();
        for (BundleProcess bp : processes) {
            if (bp.getStatus() == 2) {
                completedProcessIds.add(bp.getId());
            }
        }

        // 批量查询已完成的报工记录
        Map<Long, WorkRecord> workRecordMap = new HashMap<>();
        Map<Long, User> workerMap = new HashMap<>();
        Long companyId = TenantContext.getCompanyId();
        if (!completedProcessIds.isEmpty()) {
            LambdaQueryWrapper<WorkRecord> wrWrapper = new LambdaQueryWrapper<WorkRecord>()
                    .in(WorkRecord::getBundleProcessId, completedProcessIds)
                    .eq(WorkRecord::getStatus, 1);
            if (companyId != null) {
                wrWrapper.eq(WorkRecord::getCompanyId, companyId);
            }
            List<WorkRecord> records = workRecordMapper.selectList(wrWrapper);
            for (WorkRecord wr : records) {
                workRecordMap.put(wr.getBundleProcessId(), wr);
            }

            // 查询工人信息
            Set<Long> workerIds = new HashSet<>();
            for (WorkRecord wr : records) {
                workerIds.add(wr.getWorkerId());
            }
            if (!workerIds.isEmpty()) {
                List<User> users = userMapper.selectList(
                        new LambdaQueryWrapper<User>().in(User::getId, workerIds)
                );
                for (User u : users) {
                    workerMap.put(u.getId(), u);
                }
            }
        }

        List<BundleVO.ProcessItem> processItems = new ArrayList<>();
        for (BundleProcess bp : processes) {
            BundleVO.ProcessItem item = new BundleVO.ProcessItem();
            item.setId(bp.getId());
            item.setProcessName(bp.getProcessName());
            item.setPrice(bp.getPrice());
            item.setSeq(bp.getSeq());
            item.setStatus(bp.getStatus());

            // 已完成的工序附带报工信息
            if (bp.getStatus() == 2) {
                WorkRecord wr = workRecordMap.get(bp.getId());
                if (wr != null) {
                    item.setWorkRecordId(wr.getId());
                    item.setWorkerRemark(wr.getWorkerRemark());
                    item.setWorkTime(wr.getCreateTime());
                    User worker = workerMap.get(wr.getWorkerId());
                    if (worker != null) {
                        item.setWorkerName(worker.getRealName());
                    }
                }
            }

            processItems.add(item);
        }
        return processItems;
    }

    /**
     * 批量保存扎货（主管分扎，按床号操作）
     */
    @Transactional
    public List<BundleVO> batchSave(Long bedId, List<SaveBundleItemDTO> items) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("至少需要一扎");
        }

        // 验证床号存在
        Bed bed = bedMapper.selectById(bedId);
        if (bed == null || !bed.getCompanyId().equals(companyId) || bed.getDeleted() == 1) {
            throw new RuntimeException("床号不存在");
        }
        Long styleId = bed.getStyleId();

        // 验证款式存在
        Style style = styleMapper.selectById(styleId);
        if (style == null || !style.getCompanyId().equals(companyId) || style.getDeleted() == 1) {
            throw new RuntimeException("款式不存在");
        }

        // 查询床号工序模板
        List<com.garment.entity.Process> processTemplates = processMapper.selectList(
                new LambdaQueryWrapper<com.garment.entity.Process>()
                        .eq(com.garment.entity.Process::getBedId, bedId)
                        .eq(com.garment.entity.Process::getDeleted, 0)
                        .orderByAsc(com.garment.entity.Process::getSeq)
        );

        // 获取当前主管的扎号前缀
        String prefix = "A"; // 默认前缀（boss 分扎时）
        UserCompany creatorUc = userCompanyMapper.selectOne(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getUserId, userId)
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (creatorUc != null && creatorUc.getBundlePrefix() != null) {
            prefix = creatorUc.getBundlePrefix();
        }

        // 查询当前床号下该前缀的最大扎号数字
        Bundle maxBundle = bundleMapper.selectOne(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getBedId, bedId)
                        .eq(Bundle::getCompanyId, companyId)
                        .eq(Bundle::getDeleted, 0)
                        .likeRight(Bundle::getBundleNo, prefix)
                        .orderByDesc(Bundle::getBundleNo)
                        .last("LIMIT 1")
        );
        int nextNo = 1;
        if (maxBundle != null && maxBundle.getBundleNo() != null) {
            try {
                String noStr = maxBundle.getBundleNo().substring(prefix.length());
                nextNo = Integer.parseInt(noStr) + 1;
            } catch (NumberFormatException ignored) {}
        }

        List<BundleVO> results = new ArrayList<>();

        for (SaveBundleItemDTO item : items) {
            // 创建扎货
            Bundle bundle = new Bundle();
            bundle.setCompanyId(companyId);
            bundle.setStyleId(styleId);
            bundle.setBedId(bedId);
            String noStr = nextNo <= 999 ? String.format("%03d", nextNo) : String.valueOf(nextNo);
            bundle.setBundleNo(prefix + noStr);
            nextNo++;
            bundle.setQuantity(item.getQuantity());
            bundle.setSize(item.getSize());
            bundle.setColor(item.getColor());
            bundle.setQrCode(UUID.randomUUID().toString().replace("-", ""));
            bundle.setStatus(0);
            bundle.setPrinted(0);
            bundle.setCreatorId(userId);
            bundleMapper.insert(bundle);

            // 创建工序快照
            for (com.garment.entity.Process pt : processTemplates) {
                BundleProcess bp = new BundleProcess();
                bp.setBundleId(bundle.getId());
                bp.setCompanyId(companyId);
                bp.setProcessName(pt.getName());
                bp.setPrice(pt.getPrice());
                bp.setSeq(pt.getSeq());
                bp.setStatus(0);
                bundleProcessMapper.insert(bp);
            }

            // 构建返回VO
            BundleVO vo = new BundleVO();
            vo.setId(bundle.getId());
            vo.setBundleNo(bundle.getBundleNo());
            vo.setQuantity(bundle.getQuantity());
            vo.setSize(bundle.getSize());
            vo.setQrCode(bundle.getQrCode());
            vo.setStatus(bundle.getStatus());
            vo.setPrinted(bundle.getPrinted());
            vo.setShipped(0);
            vo.setStyleId(styleId);
            vo.setStyleNo(style.getStyleNo());
            vo.setBedId(bedId);
            vo.setBedNo(bed.getBedNo());
            vo.setColors(bed.getColors());
            vo.setColor(bundle.getColor());
            vo.setCreateTime(bundle.getCreateTime());

            List<BundleVO.ProcessItem> processItems = new ArrayList<>();
            for (com.garment.entity.Process pt : processTemplates) {
                BundleVO.ProcessItem pi = new BundleVO.ProcessItem();
                pi.setProcessName(pt.getName());
                pi.setPrice(pt.getPrice());
                pi.setSeq(pt.getSeq());
                pi.setStatus(0);
                processItems.add(pi);
            }
            vo.setProcesses(processItems);

            results.add(vo);
        }

        return results;
    }

    /**
     * 更新扎货信息
     */
    public void updateBundle(Long id, UpdateBundleDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Bundle bundle = bundleMapper.selectById(id);
        if (bundle == null || !bundle.getCompanyId().equals(companyId)) {
            throw new RuntimeException("扎货不存在");
        }
        if (Integer.valueOf(1).equals(bundle.getPrinted())) {
            throw new RuntimeException("已打印的扎货不能修改");
        }

        if (dto.getBundleNo() != null) {
            bundle.setBundleNo(dto.getBundleNo());
        }
        if (dto.getQuantity() != null) {
            bundle.setQuantity(dto.getQuantity());
        }
        if (dto.getSize() != null) {
            bundle.setSize(dto.getSize());
        }
        if (dto.getColor() != null) {
            bundle.setColor(dto.getColor());
        }

        bundleMapper.updateById(bundle);
    }

    /**
     * 删除扎货（仅待开始的可删）
     */
    @Transactional
    public void deleteBundle(Long id) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        Bundle bundle = bundleMapper.selectById(id);
        if (bundle == null || !bundle.getCompanyId().equals(companyId)) {
            throw new RuntimeException("扎货不存在");
        }
        if (Integer.valueOf(1).equals(bundle.getPrinted())) {
            throw new RuntimeException("已打印的扎货不能删除");
        }

        // 使用 deleteById，由 @TableLogic 自动转为 UPDATE SET deleted=1
        bundleMapper.deleteById(id);

        // 同时删除关联的工序快照
        bundleProcessMapper.delete(
                new LambdaQueryWrapper<BundleProcess>()
                        .eq(BundleProcess::getBundleId, id)
        );
    }

    /**
     * 批量标记扎货为已打印
     */
    public void markPrinted(List<Long> ids) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Bundle bundle = bundleMapper.selectById(id);
            if (bundle != null && bundle.getCompanyId().equals(companyId)) {
                bundle.setPrinted(1);
                bundleMapper.updateById(bundle);
            }
        }
    }

    /**
     * Bundle -> BundleVO 转换（含款式和床号信息）
     */
    private BundleVO toBundleVO(Bundle b, Map<Long, Style> styleMap, Map<Long, Bed> bedMap) {
        BundleVO vo = new BundleVO();
        vo.setId(b.getId());
        vo.setBundleNo(b.getBundleNo());
        vo.setQuantity(b.getQuantity());
        vo.setSize(b.getSize());
        vo.setQrCode(b.getQrCode());
        vo.setStatus(b.getStatus());
        vo.setPrinted(b.getPrinted());
        vo.setShipped(b.getShipped());
        vo.setShippedTime(b.getShippedTime());
        vo.setStyleId(b.getStyleId());
        vo.setBedId(b.getBedId());
        vo.setColor(b.getColor());
        vo.setCreateTime(b.getCreateTime());

        // 出货人姓名（已出货时）
        if (b.getShipperId() != null) {
            User shipper = userMapper.selectById(b.getShipperId());
            if (shipper != null) {
                vo.setShipperName(shipper.getRealName());
            }
        }

        Style style = styleMap.get(b.getStyleId());
        if (style != null) {
            vo.setStyleNo(style.getStyleNo());
        }

        if (b.getBedId() != null) {
            Bed bed = bedMap.get(b.getBedId());
            if (bed != null) {
                vo.setBedNo(bed.getBedNo());
                vo.setBedName(bed.getName());
                vo.setColors(bed.getColors());
            }
        }

        return vo;
    }
}

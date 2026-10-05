package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.common.TenantContext;
import com.garment.dto.QuickImportDTO;
import com.garment.entity.Bed;
import com.garment.entity.Bundle;
import com.garment.entity.BundleProcess;
import com.garment.entity.Style;
import com.garment.entity.UserCompany;
import com.garment.mapper.*;
import com.garment.vo.QuickImportVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 一键录入服务
 */
@Service
public class QuickImportService {

    private final StyleMapper styleMapper;
    private final BedMapper bedMapper;
    private final ProcessMapper processMapper;
    private final BundleMapper bundleMapper;
    private final BundleProcessMapper bundleProcessMapper;
    private final UserCompanyMapper userCompanyMapper;

    public QuickImportService(StyleMapper styleMapper, BedMapper bedMapper,
                              ProcessMapper processMapper, BundleMapper bundleMapper,
                              BundleProcessMapper bundleProcessMapper,
                              UserCompanyMapper userCompanyMapper) {
        this.styleMapper = styleMapper;
        this.bedMapper = bedMapper;
        this.processMapper = processMapper;
        this.bundleMapper = bundleMapper;
        this.bundleProcessMapper = bundleProcessMapper;
        this.userCompanyMapper = userCompanyMapper;
    }

    /**
     * 一键录入：自动查找或创建款号、床号，批量创建扎货
     */
    @Transactional
    public QuickImportVO quickImport(QuickImportDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        // 参数校验
        if (dto.getStyleNo() == null || dto.getStyleNo().trim().isEmpty()) {
            throw new RuntimeException("款号不能为空");
        }
        if (dto.getStyleName() == null || dto.getStyleName().trim().isEmpty()) {
            throw new RuntimeException("款式名称不能为空");
        }
        if (dto.getBedNo() == null || dto.getBedNo().trim().isEmpty()) {
            throw new RuntimeException("床号不能为空");
        }
        if (dto.getBedName() == null || dto.getBedName().trim().isEmpty()) {
            throw new RuntimeException("床号名称不能为空");
        }
        if (dto.getBundles() == null || dto.getBundles().isEmpty()) {
            throw new RuntimeException("至少需要一扎");
        }

        // 1. 查找或创建款号
        boolean isNewStyle = false;
        Style style = styleMapper.selectOne(
                new LambdaQueryWrapper<Style>()
                        .eq(Style::getCompanyId, companyId)
                        .eq(Style::getStyleNo, dto.getStyleNo().trim())
                        .eq(Style::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (style == null) {
            // 创建新款式
            style = new Style();
            style.setCompanyId(companyId);
            style.setStyleNo(dto.getStyleNo().trim());
            style.setName(dto.getStyleName().trim());
            styleMapper.insert(style);
            isNewStyle = true;
        }

        // 2. 查找或创建床号
        boolean isNewBed = false;
        Bed bed = bedMapper.selectOne(
                new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getCompanyId, companyId)
                        .eq(Bed::getStyleId, style.getId())
                        .eq(Bed::getBedNo, dto.getBedNo().trim())
                        .eq(Bed::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (bed == null) {
            // 创建新床号
            bed = new Bed();
            bed.setCompanyId(companyId);
            bed.setStyleId(style.getId());
            bed.setBedNo(dto.getBedNo().trim());
            bed.setName(dto.getBedName().trim());
            if (dto.getColors() != null && !dto.getColors().trim().isEmpty()) {
                bed.setColors(dto.getColors().trim());
            }
            // 计算总件数
            int totalQty = dto.getBundles().stream().mapToInt(QuickImportDTO.BundleItem::getQuantity).sum();
            bed.setQuantity(totalQty);
            bedMapper.insert(bed);
            isNewBed = true;

            // 保存工序（如果有）
            if (dto.getProcesses() != null && !dto.getProcesses().isEmpty()) {
                int seq = 1;
                for (QuickImportDTO.ProcessItem pi : dto.getProcesses()) {
                    com.garment.entity.Process process = new com.garment.entity.Process();
                    process.setCompanyId(companyId);
                    process.setStyleId(style.getId());
                    process.setBedId(bed.getId());
                    process.setName(pi.getName());
                    process.setSeq(seq++);
                    process.setPrice(pi.getPrice());
                    processMapper.insert(process);
                }
            }
        }

        // 3. 查询床号工序模板
        List<com.garment.entity.Process> processTemplates = processMapper.selectList(
                new LambdaQueryWrapper<com.garment.entity.Process>()
                        .eq(com.garment.entity.Process::getBedId, bed.getId())
                        .eq(com.garment.entity.Process::getDeleted, 0)
                        .orderByAsc(com.garment.entity.Process::getSeq)
        );

        // 4. 获取当前用户的扎号前缀
        String prefix = "A";
        UserCompany uc = userCompanyMapper.selectOne(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getUserId, userId)
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (uc != null && uc.getBundlePrefix() != null) {
            prefix = uc.getBundlePrefix();
        }

        // 5. 查询当前床号下该前缀的最大扎号数字
        Bundle maxBundle = bundleMapper.selectOne(
                new LambdaQueryWrapper<Bundle>()
                        .eq(Bundle::getBedId, bed.getId())
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

        // 6. 批量创建扎货
        List<QuickImportVO.BundleInfo> bundleInfos = new ArrayList<>();
        int totalQuantity = 0;

        for (QuickImportDTO.BundleItem item : dto.getBundles()) {
            Bundle bundle = new Bundle();
            bundle.setCompanyId(companyId);
            bundle.setStyleId(style.getId());
            bundle.setBedId(bed.getId());
            String noStr = nextNo <= 999 ? String.format("%03d", nextNo) : String.valueOf(nextNo);
            bundle.setBundleNo(prefix + noStr);
            nextNo++;
            bundle.setQuantity(item.getQuantity());
            bundle.setSize(item.getSize());
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

            // 构建返回信息
            QuickImportVO.BundleInfo info = new QuickImportVO.BundleInfo();
            info.setId(bundle.getId());
            info.setBundleNo(bundle.getBundleNo());
            info.setQuantity(bundle.getQuantity());
            info.setSize(bundle.getSize());
            info.setQrCode(bundle.getQrCode());
            bundleInfos.add(info);

            totalQuantity += item.getQuantity();
        }

        // 7. 构建响应
        QuickImportVO vo = new QuickImportVO();
        vo.setStyleId(style.getId());
        vo.setStyleNo(style.getStyleNo());
        vo.setNewStyle(isNewStyle);
        vo.setBedId(bed.getId());
        vo.setBedNo(bed.getBedNo());
        vo.setNewBed(isNewBed);
        vo.setBundleCount(bundleInfos.size());
        vo.setTotalQuantity(totalQuantity);
        vo.setBundles(bundleInfos);

        return vo;
    }
}

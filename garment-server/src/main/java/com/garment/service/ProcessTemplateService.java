package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.common.TenantContext;
import com.garment.dto.SaveProcessTemplateDTO;
import com.garment.entity.ProcessTemplate;
import com.garment.entity.ProcessTemplateItem;
import com.garment.mapper.ProcessTemplateItemMapper;
import com.garment.mapper.ProcessTemplateMapper;
import com.garment.vo.ProcessTemplateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProcessTemplateService {

    private final ProcessTemplateMapper templateMapper;
    private final ProcessTemplateItemMapper itemMapper;

    public ProcessTemplateService(ProcessTemplateMapper templateMapper,
                                  ProcessTemplateItemMapper itemMapper) {
        this.templateMapper = templateMapper;
        this.itemMapper = itemMapper;
    }

    /**
     * 模板列表（不含明细）
     */
    public List<ProcessTemplateVO> list() {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        List<ProcessTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<ProcessTemplate>()
                        .eq(ProcessTemplate::getCompanyId, companyId)
                        .orderByDesc(ProcessTemplate::getCreateTime)
        );

        List<ProcessTemplateVO> result = new ArrayList<>();
        for (ProcessTemplate t : templates) {
            ProcessTemplateVO vo = new ProcessTemplateVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setRemark(t.getRemark());
            vo.setCreateTime(t.getCreateTime());
            vo.setUpdateTime(t.getUpdateTime());

            // 查询明细数量
            Long count = itemMapper.selectCount(
                    new LambdaQueryWrapper<ProcessTemplateItem>()
                            .eq(ProcessTemplateItem::getTemplateId, t.getId())
                            .eq(ProcessTemplateItem::getDeleted, 0)
            );
            vo.setItemCount(count.intValue());
            result.add(vo);
        }
        return result;
    }

    /**
     * 模板详情（含明细）
     */
    public ProcessTemplateVO getDetail(Long id) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        ProcessTemplate t = templateMapper.selectById(id);
        if (t == null || !t.getCompanyId().equals(companyId)) {
            throw new RuntimeException("模板不存在");
        }

        ProcessTemplateVO vo = new ProcessTemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setRemark(t.getRemark());
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());

        List<ProcessTemplateItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ProcessTemplateItem>()
                        .eq(ProcessTemplateItem::getTemplateId, id)
                        .eq(ProcessTemplateItem::getDeleted, 0)
                        .orderByAsc(ProcessTemplateItem::getSeq)
        );
        vo.setItemCount(items.size());

        List<ProcessTemplateVO.TemplateItem> itemVOs = new ArrayList<>();
        for (ProcessTemplateItem item : items) {
            ProcessTemplateVO.TemplateItem itemVO = new ProcessTemplateVO.TemplateItem();
            itemVO.setId(item.getId());
            itemVO.setName(item.getName());
            itemVO.setSeq(item.getSeq());
            itemVO.setPrice(item.getPrice());
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    /**
     * 新增模板（含明细）
     */
    @Transactional
    public ProcessTemplateVO save(SaveProcessTemplateDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("模板名称不能为空");
        }

        ProcessTemplate t = new ProcessTemplate();
        t.setCompanyId(companyId);
        t.setName(dto.getName().trim());
        t.setRemark(dto.getRemark());
        templateMapper.insert(t);

        // 保存明细
        if (dto.getItems() != null) {
            for (SaveProcessTemplateDTO.TemplateItem item : dto.getItems()) {
                if (item.getName() == null || item.getName().trim().isEmpty()) {
                    continue;
                }
                ProcessTemplateItem entity = new ProcessTemplateItem();
                entity.setTemplateId(t.getId());
                entity.setCompanyId(companyId);
                entity.setName(item.getName().trim());
                entity.setSeq(item.getSeq() != null ? item.getSeq() : 0);
                entity.setPrice(item.getPrice());
                itemMapper.insert(entity);
            }
        }

        return getDetail(t.getId());
    }

    /**
     * 编辑模板（先删旧明细，再插新明细）
     */
    @Transactional
    public void update(Long id, SaveProcessTemplateDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        ProcessTemplate t = templateMapper.selectById(id);
        if (t == null || !t.getCompanyId().equals(companyId)) {
            throw new RuntimeException("模板不存在");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("模板名称不能为空");
        }

        t.setName(dto.getName().trim());
        t.setRemark(dto.getRemark());
        templateMapper.updateById(t);

        // 删除旧明细
        List<ProcessTemplateItem> oldItems = itemMapper.selectList(
                new LambdaQueryWrapper<ProcessTemplateItem>()
                        .eq(ProcessTemplateItem::getTemplateId, id)
                        .eq(ProcessTemplateItem::getDeleted, 0)
        );
        for (ProcessTemplateItem item : oldItems) {
            itemMapper.deleteById(item.getId());
        }

        // 插入新明细
        if (dto.getItems() != null) {
            for (SaveProcessTemplateDTO.TemplateItem item : dto.getItems()) {
                if (item.getName() == null || item.getName().trim().isEmpty()) {
                    continue;
                }
                ProcessTemplateItem entity = new ProcessTemplateItem();
                entity.setTemplateId(id);
                entity.setCompanyId(companyId);
                entity.setName(item.getName().trim());
                entity.setSeq(item.getSeq() != null ? item.getSeq() : 0);
                entity.setPrice(item.getPrice());
                itemMapper.insert(entity);
            }
        }
    }

    /**
     * 删除模板（级联删除明细）
     */
    @Transactional
    public void delete(Long id) {
        Long companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new RuntimeException("请先选择公司");
        }

        ProcessTemplate t = templateMapper.selectById(id);
        if (t == null || !t.getCompanyId().equals(companyId)) {
            throw new RuntimeException("模板不存在");
        }

        templateMapper.deleteById(id);

        // 删除关联明细
        List<ProcessTemplateItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<ProcessTemplateItem>()
                        .eq(ProcessTemplateItem::getTemplateId, id)
                        .eq(ProcessTemplateItem::getDeleted, 0)
        );
        for (ProcessTemplateItem item : items) {
            itemMapper.deleteById(item.getId());
        }
    }
}

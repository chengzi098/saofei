package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.common.TenantContext;
import com.garment.dto.SaveAdjustmentDTO;
import com.garment.entity.User;
import com.garment.entity.UserCompany;
import com.garment.entity.WageAdjustment;
import com.garment.mapper.UserCompanyMapper;
import com.garment.mapper.UserMapper;
import com.garment.mapper.WageAdjustmentMapper;
import com.garment.vo.WageAdjustmentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WageAdjustmentService {

    private final WageAdjustmentMapper adjustmentMapper;
    private final UserMapper userMapper;
    private final UserCompanyMapper userCompanyMapper;

    public WageAdjustmentService(WageAdjustmentMapper adjustmentMapper,
                                  UserMapper userMapper,
                                  UserCompanyMapper userCompanyMapper) {
        this.adjustmentMapper = adjustmentMapper;
        this.userMapper = userMapper;
        this.userCompanyMapper = userCompanyMapper;
    }

    /**
     * 添加调整记录（老板/主管）
     */
    @Transactional
    public WageAdjustmentVO create(SaveAdjustmentDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        Long operatorId = TenantContext.getUserId();

        // 校验操作人角色
        String operatorRole = getRole(operatorId, companyId);
        if (!"boss".equals(operatorRole) && !"supervisor".equals(operatorRole)) {
            throw new RuntimeException("只有老板和主管可以添加工资调整");
        }

        // 校验目标员工存在且属于本公司
        String workerRole = getRole(dto.getWorkerId(), companyId);
        if (workerRole == null) {
            throw new RuntimeException("该员工不属于当前公司");
        }

        // 主管只能给工人添加
        if ("supervisor".equals(operatorRole) && !"worker".equals(workerRole)) {
            throw new RuntimeException("主管只能给工人添加工资调整");
        }

        // 校验金额
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("金额必须大于0");
        }

        // 校验类型
        if (dto.getType() == null || (dto.getType() != 1 && dto.getType() != 2)) {
            throw new RuntimeException("类型必须为1（加钱）或2（扣钱）");
        }

        // 校验备注
        if (dto.getRemark() == null || dto.getRemark().trim().isEmpty()) {
            throw new RuntimeException("备注不能为空");
        }

        WageAdjustment adj = new WageAdjustment();
        adj.setCompanyId(companyId);
        adj.setWorkerId(dto.getWorkerId());
        adj.setOperatorId(operatorId);
        adj.setType(dto.getType());
        adj.setAmount(dto.getAmount());
        adj.setRemark(dto.getRemark().trim());
        adj.setStatus(0); // 待审批
        adjustmentMapper.insert(adj);

        return toVO(adj);
    }

    /**
     * 查询调整列表（老板/主管）
     */
    public List<WageAdjustmentVO> list(Integer status, Long workerId) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        String role = getRole(userId, companyId);

        LambdaQueryWrapper<WageAdjustment> wrapper = new LambdaQueryWrapper<WageAdjustment>()
                .eq(WageAdjustment::getCompanyId, companyId)
                .orderByDesc(WageAdjustment::getCreateTime);

        // 主管只能看到自己添加的记录
        if ("supervisor".equals(role)) {
            wrapper.eq(WageAdjustment::getOperatorId, userId);
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(WageAdjustment::getStatus, status);
        } else {
            // 默认不显示已删除
            wrapper.ne(WageAdjustment::getStatus, 2);
        }

        // 工人筛选
        if (workerId != null) {
            wrapper.eq(WageAdjustment::getWorkerId, workerId);
        }

        List<WageAdjustment> list = adjustmentMapper.selectList(wrapper);
        return toVOList(list);
    }

    /**
     * 编辑（老板/主管，仅待审批状态）
     */
    @Transactional
    public WageAdjustmentVO update(Long id, SaveAdjustmentDTO dto) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        String role = getRole(userId, companyId);

        WageAdjustment adj = getByIdAndCompany(id, companyId);

        // 只有待审批状态才能编辑
        if (adj.getStatus() != 0) {
            throw new RuntimeException("只有待审批状态的记录才能编辑");
        }

        // 主管只能编辑自己添加的记录
        if ("supervisor".equals(role) && !userId.equals(adj.getOperatorId())) {
            throw new RuntimeException("主管只能编辑自己添加的记录");
        }

        // 校验目标员工
        String workerRole = getRole(dto.getWorkerId(), companyId);
        if (workerRole == null) {
            throw new RuntimeException("该员工不属于当前公司");
        }

        // 校验金额
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("金额必须大于0");
        }

        // 校验类型
        if (dto.getType() == null || (dto.getType() != 1 && dto.getType() != 2)) {
            throw new RuntimeException("类型必须为1（加钱）或2（扣钱）");
        }

        // 校验备注
        if (dto.getRemark() == null || dto.getRemark().trim().isEmpty()) {
            throw new RuntimeException("备注不能为空");
        }

        adj.setWorkerId(dto.getWorkerId());
        adj.setType(dto.getType());
        adj.setAmount(dto.getAmount());
        adj.setRemark(dto.getRemark().trim());
        adjustmentMapper.updateById(adj);

        return toVO(adj);
    }

    /**
     * 审批（仅老板）
     */
    @Transactional
    public void approve(Long id) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        String role = getRole(userId, companyId);
        if (!"boss".equals(role)) {
            throw new RuntimeException("只有老板可以审批");
        }

        WageAdjustment adj = getByIdAndCompany(id, companyId);
        if (adj.getStatus() != 0) {
            throw new RuntimeException("该记录不是待审批状态");
        }

        adj.setStatus(1); // 已审批
        adj.setApproverId(userId);
        adj.setApproveTime(LocalDateTime.now());
        adjustmentMapper.updateById(adj);
    }

    /**
     * 拒绝审批（仅老板）
     */
    @Transactional
    public void reject(Long id) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        String role = getRole(userId, companyId);
        if (!"boss".equals(role)) {
            throw new RuntimeException("只有老板可以拒绝审批");
        }

        WageAdjustment adj = getByIdAndCompany(id, companyId);
        if (adj.getStatus() != 0) {
            throw new RuntimeException("该记录不是待审批状态");
        }

        adj.setStatus(3); // 已驳回
        adj.setApproverId(userId);
        adj.setApproveTime(LocalDateTime.now());
        adjustmentMapper.updateById(adj);
    }

    /**
     * 删除（老板/主管，留痕）
     */
    @Transactional
    public void delete(Long id) {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        String role = getRole(userId, companyId);

        WageAdjustment adj = getByIdAndCompany(id, companyId);

        if ("supervisor".equals(role)) {
            // 主管只能删除自己添加的待审批记录
            if (!userId.equals(adj.getOperatorId())) {
                throw new RuntimeException("主管只能删除自己添加的记录");
            }
            if (adj.getStatus() != 0) {
                throw new RuntimeException("只能删除待审批的记录");
            }
        } else if (!"boss".equals(role)) {
            throw new RuntimeException("没有权限删除");
        }

        // 老板可以删除待审批和已审批的记录
        adj.setStatus(2); // 已删除
        adj.setDeleteTime(LocalDateTime.now());
        adjustmentMapper.updateById(adj);
    }

    /**
     * 工人查看自己的记录
     */
    public List<WageAdjustmentVO> myRecords() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();

        List<WageAdjustment> list = adjustmentMapper.selectList(
                new LambdaQueryWrapper<WageAdjustment>()
                        .eq(WageAdjustment::getCompanyId, companyId)
                        .eq(WageAdjustment::getWorkerId, userId)
                        .orderByDesc(WageAdjustment::getCreateTime)
        );

        return toVOList(list);
    }

    // ========== 私有辅助方法 ==========

    private String getRole(Long userId, Long companyId) {
        UserCompany uc = userCompanyMapper.selectOne(
                new LambdaQueryWrapper<UserCompany>()
                        .eq(UserCompany::getUserId, userId)
                        .eq(UserCompany::getCompanyId, companyId)
                        .eq(UserCompany::getStatus, 1)
                        .last("LIMIT 1")
        );
        return uc != null ? uc.getRole() : null;
    }

    private WageAdjustment getByIdAndCompany(Long id, Long companyId) {
        WageAdjustment adj = adjustmentMapper.selectById(id);
        if (adj == null || !companyId.equals(adj.getCompanyId())) {
            throw new RuntimeException("记录不存在");
        }
        return adj;
    }

    private List<WageAdjustmentVO> toVOList(List<WageAdjustment> list) {
        if (list.isEmpty()) return new ArrayList<>();

        // 收集所有涉及的用户ID
        List<Long> userIds = new ArrayList<>();
        for (WageAdjustment adj : list) {
            userIds.add(adj.getWorkerId());
            userIds.add(adj.getOperatorId());
            if (adj.getApproverId() != null) {
                userIds.add(adj.getApproverId());
            }
        }

        // 批量查询用户信息
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<WageAdjustmentVO> voList = new ArrayList<>();
        for (WageAdjustment adj : list) {
            voList.add(toVO(adj, userMap));
        }
        return voList;
    }

    private WageAdjustmentVO toVO(WageAdjustment adj) {
        // 单条记录时，查用户信息
        List<Long> userIds = new ArrayList<>();
        userIds.add(adj.getWorkerId());
        userIds.add(adj.getOperatorId());
        if (adj.getApproverId() != null) {
            userIds.add(adj.getApproverId());
        }
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return toVO(adj, userMap);
    }

    private WageAdjustmentVO toVO(WageAdjustment adj, Map<Long, User> userMap) {
        WageAdjustmentVO vo = new WageAdjustmentVO();
        vo.setId(adj.getId());
        vo.setWorkerId(adj.getWorkerId());
        vo.setOperatorId(adj.getOperatorId());
        vo.setType(adj.getType());
        vo.setAmount(adj.getAmount());
        vo.setRemark(adj.getRemark());
        vo.setStatus(adj.getStatus());
        vo.setApproverId(adj.getApproverId());
        vo.setApproveTime(adj.getApproveTime());
        vo.setDeleteTime(adj.getDeleteTime());
        vo.setCreateTime(adj.getCreateTime());

        User worker = userMap.get(adj.getWorkerId());
        if (worker != null) vo.setWorkerName(worker.getRealName());

        User operator = userMap.get(adj.getOperatorId());
        if (operator != null) vo.setOperatorName(operator.getRealName());

        if (adj.getApproverId() != null) {
            User approver = userMap.get(adj.getApproverId());
            if (approver != null) vo.setApproverName(approver.getRealName());
        }

        return vo;
    }
}

package com.garment.service;

import com.garment.dto.EmployeeWorkDetailDto;
import com.garment.dto.EmployeeWorkSummaryDto;
import com.garment.dto.WorkerSpeedDto;
import com.garment.mapper.EmployeeWorkDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 员工工作详情服务
 */
@Service
public class EmployeeWorkDetailService {

    @Autowired
    private EmployeeWorkDetailMapper employeeWorkDetailMapper;

    /**
     * 获取员工工作详情列表
     *
     * @param companyId 公司ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param workerId  员工ID（可选）
     * @return 工作详情列表
     */
    public List<EmployeeWorkDetailDto> getEmployeeWorkDetails(Long companyId, String startDate,
                                                               String endDate, Long workerId) {
        // 构建员工筛选条件
        String workerCondition = "";
        if (workerId != null) {
            workerCondition = "AND bn.worker_id = " + workerId;
        }

        return employeeWorkDetailMapper.getEmployeeWorkDetails(
            companyId, startDate, endDate, workerCondition
        );
    }

    /**
     * 获取员工工作汇总（按员工聚合，status=1 已通过）
     *
     * @param companyId 公司ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param workerId  员工ID（可选，为空则返回每个员工一行）
     * @return 汇总列表
     */
    public List<EmployeeWorkSummaryDto> getEmployeeWorkSummary(Long companyId, String startDate,
                                                               String endDate, Long workerId) {
        return employeeWorkDetailMapper.getEmployeeWorkSummary(
            companyId, startDate, endDate, workerId
        );
    }

    /**
     * 获取员工每扎作业耗时（相邻报工时间差法，用于作业速度柱状图）
     */
    public List<WorkerSpeedDto> getWorkerSpeed(Long companyId, Long workerId,
                                               String startDate, String endDate) {
        return employeeWorkDetailMapper.getWorkerSpeed(
            companyId, workerId, startDate, endDate
        );
    }
}

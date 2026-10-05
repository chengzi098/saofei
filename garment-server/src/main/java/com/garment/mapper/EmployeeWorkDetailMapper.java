package com.garment.mapper;

import com.garment.dto.EmployeeWorkDetailDto;
import com.garment.dto.EmployeeWorkSummaryDto;
import com.garment.dto.WorkerSpeedDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工工作详情 Mapper
 */
@Mapper
public interface EmployeeWorkDetailMapper {

    @Select("""
        SELECT
            u.id as workerId,
            u.real_name as workerName,
            bed.bed_no as bedNo,
            bn.bundle_no as bundleNo,
            bp.process_name as processName,
            wr.quantity as quantity,
            wr.amount as amount,
            DATE_FORMAT(wr.create_time, '%%Y-%%m-%%d') as date
        FROM work_record wr
        LEFT JOIN user u ON wr.worker_id = u.id
        LEFT JOIN bundle bn ON wr.bundle_id = bn.id
        LEFT JOIN bed bed ON bn.bed_id = bed.id
        LEFT JOIN bundle_process bp ON wr.bundle_process_id = bp.id
        WHERE wr.company_id = #{companyId}
          AND wr.status = 1
          AND wr.create_time >= #{startDate}
          AND wr.create_time <= CONCAT(#{endDate}, ' 23:59:59')
          ${workerCondition}
        ORDER BY wr.create_time DESC
    """)
    List<EmployeeWorkDetailDto> getEmployeeWorkDetails(
        @Param("companyId") Long companyId,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("workerCondition") String workerCondition
    );

    /**
     * 员工工作汇总（按员工聚合，status=1 已通过）
     * 不传 workerId 时返回该时段每个员工一行，前端可再 reduce 成全员合计
     */
    @Select("""
        <script>
        SELECT
            u.id as workerId,
            u.real_name as workerName,
            COALESCE(SUM(wr.quantity), 0) as totalQuantity,
            COALESCE(SUM(wr.amount), 0) as totalAmount,
            COUNT(DISTINCT DATE(wr.create_time)) as workDays,
            COUNT(wr.id) as recordCount
        FROM work_record wr
        LEFT JOIN user u ON wr.worker_id = u.id
        WHERE wr.company_id = #{companyId}
          AND wr.status = 1
          AND wr.create_time &gt;= #{startDate}
          AND wr.create_time &lt;= CONCAT(#{endDate}, ' 23:59:59')
          <if test="workerId != null">AND wr.worker_id = #{workerId}</if>
        GROUP BY u.id, u.real_name
        </script>
    """)
    List<EmployeeWorkSummaryDto> getEmployeeWorkSummary(
        @Param("companyId") Long companyId,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate,
        @Param("workerId") Long workerId
    );

    /**
     * 员工每扎作业耗时（相邻报工时间差法）
     * 按 (worker_id, bed_id, process_name) 分组，组内按报工时间升序，
     * 相邻两次报工 create_time 之差 = 后一扎作业耗时；首扎 prev 为 NULL 被过滤（"第一扎不算"）
     */
    @Select("""
        <script>
        SELECT * FROM (
            SELECT
                bn.bundle_no as bundleNo,
                bed.bed_no as bedNo,
                bp.process_name as processName,
                wr.quantity as quantity,
                TIMESTAMPDIFF(MINUTE,
                    LAG(wr.create_time) OVER (
                        PARTITION BY wr.worker_id, bn.bed_id, bp.process_name
                        ORDER BY wr.create_time
                    ),
                    wr.create_time
                ) as durationMinutes,
                wr.create_time as completeTime
            FROM work_record wr
            JOIN bundle_process bp ON wr.bundle_process_id = bp.id
            JOIN bundle bn ON wr.bundle_id = bn.id
            LEFT JOIN bed bed ON bn.bed_id = bed.id
            WHERE wr.company_id = #{companyId}
              AND wr.status = 1
              AND wr.worker_id = #{workerId}
              AND wr.create_time &gt;= #{startDate}
              AND wr.create_time &lt;= CONCAT(#{endDate}, ' 23:59:59')
              AND bn.deleted = 0
        ) t
        WHERE t.durationMinutes IS NOT NULL
        ORDER BY t.completeTime DESC
        LIMIT 100
        </script>
    """)
    List<WorkerSpeedDto> getWorkerSpeed(
        @Param("companyId") Long companyId,
        @Param("workerId") Long workerId,
        @Param("startDate") String startDate,
        @Param("endDate") String endDate
    );
}

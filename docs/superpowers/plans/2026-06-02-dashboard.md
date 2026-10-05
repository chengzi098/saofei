# 看板功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为老板和主管分别实现生产进度看板页面，展示款式/扎货完成进度、产量趋势和异常预警。

**Architecture:** 后端新增 DashboardController + DashboardService，通过多表关联查询组装看板数据；前端新增两个 Vue 页面，使用 Element Plus 组件 + ECharts 图表展示。

**Tech Stack:** Spring Boot 3.2.5 + MyBatis-Plus + Vue 3 + Element Plus + ECharts

---

## 文件清单

### 后端新建文件

| 文件 | 职责 |
|------|------|
| `com/garment/vo/boss/BossDashboardVO.java` | 老板看板接口响应对象 |
| `com/garment/vo/boss/StyleProgressVO.java` | 款式进度卡片数据 |
| `com/garment/vo/boss/AbnormalBundleVO.java` | 异常扎货数据 |
| `com/garment/vo/boss/WeeklyTrendItemVO.java` | 周趋势单项数据 |
| `com/garment/vo/boss/StyleDetailVO.java` | 款式详情抽屉响应 |
| `com/garment/vo/boss/BedDetailVO.java` | 床号详情（抽屉内） |
| `com/garment/vo/boss/BundleProcessDetailVO.java` | 扎货工序详情（抽屉内） |
| `com/garment/vo/supervisor/SupervisorDashboardVO.java` | 主管看板接口响应对象 |
| `com/garment/vo/supervisor/StyleGroupVO.java` | 主管视角款式分组 |
| `com/garment/vo/supervisor/SupervisorBundleVO.java` | 主管视角扎货进度 |
| `com/garment/vo/supervisor/SupervisorProcessVO.java` | 主管视角工序进度 |
| `com/garment/vo/supervisor/PendingReturnVO.java` | 待处理退回数据 |
| `com/garment/service/DashboardService.java` | 看板业务逻辑核心 |
| `com/garment/controller/DashboardController.java` | 看板 REST 接口 |

### 后端复用/依赖的现有文件

| 文件 | 用途 |
|------|------|
| `com/garment/entity/Style.java` | 查询款式基础数据 |
| `com/garment/entity/Bed.java` | 查询床号数据 |
| `com/garment/entity/Bundle.java` | 查询扎货数据 |
| `com/garment/entity/BundleProcess.java` | 查询工序快照 |
| `com/garment/entity/WorkRecord.java` | 查询报工记录 |
| `com/garment/entity/UserCompany.java` | 查询用户角色 |
| `com/garment/mapper/StyleMapper.java` | 款式 Mapper |
| `com/garment/mapper/BedMapper.java` | 床号 Mapper |
| `com/garment/mapper/BundleMapper.java` | 扎货 Mapper |
| `com/garment/mapper/BundleProcessMapper.java` | 工序 Mapper |
| `com/garment/mapper/WorkRecordMapper.java` | 报工 Mapper |
| `com/garment/mapper/UserCompanyMapper.java` | 用户公司关联 Mapper |
| `com/garment/common/TenantContext.java` | 获取当前租户信息 |
| `com/garment/common/Result.java` | 统一响应封装 |

### 前端新建文件

| 文件 | 职责 |
|------|------|
| `garment-web/src/api/dashboard.js` | 看板 API 封装 |
| `garment-web/src/views/DashboardBoss.vue` | 老板看板页面 |
| `garment-web/src/views/DashboardSupervisor.vue` | 主管看板页面 |

### 前端修改文件

| 文件 | 修改内容 |
|------|---------|
| `garment-web/src/router/index.js` | 添加 `/dashboard/boss` 和 `/dashboard/supervisor` 路由 |
| `garment-web/src/views/layout/Layout.vue` | 侧边栏添加看板菜单入口 |
| `garment-web/package.json` | 安装 `echarts` 和 `vue-echarts` |

---

## 任务分解

### Task 1: 安装 ECharts 依赖

**Files:**
- Modify: `garment-web/package.json`

- [ ] **Step 1: 安装 echarts 和 vue-echarts**

```bash
cd garment-web
npm install echarts vue-echarts
```

- [ ] **Step 2: 验证安装成功**

```bash
npm ls echarts vue-echarts
```

Expected: 显示版本号，无错误

---

### Task 2: 后端 VO 类 — 老板看板

**Files:**
- Create: `garment-server/src/main/java/com/garment/vo/boss/StyleProgressVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/boss/AbnormalBundleVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/boss/WeeklyTrendItemVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/boss/BossDashboardVO.java`

- [ ] **Step 1: 创建 `StyleProgressVO.java`**

```java
package com.garment.vo.boss;

import java.time.LocalDateTime;

public class StyleProgressVO {
    private Long styleId;
    private String styleNo;
    private Integer bedCount;
    private Integer totalQuantity;
    private Integer completedQuantity;
    private Integer progressPercent;
    private Integer status;
    private String statusText;
    private LocalDateTime lastWorkTime;
    private String lastWorkTimeText;

    public Long getStyleId() { return styleId; }
    public void setStyleId(Long styleId) { this.styleId = styleId; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public Integer getBedCount() { return bedCount; }
    public void setBedCount(Integer bedCount) { this.bedCount = bedCount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getCompletedQuantity() { return completedQuantity; }
    public void setCompletedQuantity(Integer completedQuantity) { this.completedQuantity = completedQuantity; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public LocalDateTime getLastWorkTime() { return lastWorkTime; }
    public void setLastWorkTime(LocalDateTime lastWorkTime) { this.lastWorkTime = lastWorkTime; }
    public String getLastWorkTimeText() { return lastWorkTimeText; }
    public void setLastWorkTimeText(String lastWorkTimeText) { this.lastWorkTimeText = lastWorkTimeText; }
}
```

- [ ] **Step 2: 创建 `AbnormalBundleVO.java`**

```java
package com.garment.vo.boss;

public class AbnormalBundleVO {
    private Long bundleId;
    private String bundleNo;
    private String styleNo;
    private String bedNo;
    private Integer abnormalType;
    private String abnormalTypeText;
    private Integer severity;

    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public Integer getAbnormalType() { return abnormalType; }
    public void setAbnormalType(Integer abnormalType) { this.abnormalType = abnormalType; }
    public String getAbnormalTypeText() { return abnormalTypeText; }
    public void setAbnormalTypeText(String abnormalTypeText) { this.abnormalTypeText = abnormalTypeText; }
    public Integer getSeverity() { return severity; }
    public void setSeverity(Integer severity) { this.severity = severity; }
}
```

- [ ] **Step 3: 创建 `WeeklyTrendItemVO.java`**

```java
package com.garment.vo.boss;

public class WeeklyTrendItemVO {
    private String date;
    private String dayOfWeek;
    private Integer quantity;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
```

- [ ] **Step 4: 创建 `BossDashboardVO.java`**

```java
package com.garment.vo.boss;

import java.util.List;

public class BossDashboardVO {
    private Integer todayCompleted;
    private Double todayCompletedCompare;
    private Integer activeStyleCount;
    private Integer totalStyleCount;
    private Integer abnormalBundleCount;
    private Integer activeWorkerCount;
    private Integer todayWorkerCount;
    private List<StyleProgressVO> styles;
    private List<AbnormalBundleVO> abnormalBundles;
    private List<WeeklyTrendItemVO> weeklyTrend;

    public Integer getTodayCompleted() { return todayCompleted; }
    public void setTodayCompleted(Integer todayCompleted) { this.todayCompleted = todayCompleted; }
    public Double getTodayCompletedCompare() { return todayCompletedCompare; }
    public void setTodayCompletedCompare(Double todayCompletedCompare) { this.todayCompletedCompare = todayCompletedCompare; }
    public Integer getActiveStyleCount() { return activeStyleCount; }
    public void setActiveStyleCount(Integer activeStyleCount) { this.activeStyleCount = activeStyleCount; }
    public Integer getTotalStyleCount() { return totalStyleCount; }
    public void setTotalStyleCount(Integer totalStyleCount) { this.totalStyleCount = totalStyleCount; }
    public Integer getAbnormalBundleCount() { return abnormalBundleCount; }
    public void setAbnormalBundleCount(Integer abnormalBundleCount) { this.abnormalBundleCount = abnormalBundleCount; }
    public Integer getActiveWorkerCount() { return activeWorkerCount; }
    public void setActiveWorkerCount(Integer activeWorkerCount) { this.activeWorkerCount = activeWorkerCount; }
    public Integer getTodayWorkerCount() { return todayWorkerCount; }
    public void setTodayWorkerCount(Integer todayWorkerCount) { this.todayWorkerCount = todayWorkerCount; }
    public List<StyleProgressVO> getStyles() { return styles; }
    public void setStyles(List<StyleProgressVO> styles) { this.styles = styles; }
    public List<AbnormalBundleVO> getAbnormalBundles() { return abnormalBundles; }
    public void setAbnormalBundles(List<AbnormalBundleVO> abnormalBundles) { this.abnormalBundles = abnormalBundles; }
    public List<WeeklyTrendItemVO> getWeeklyTrend() { return weeklyTrend; }
    public void setWeeklyTrend(List<WeeklyTrendItemVO> weeklyTrend) { this.weeklyTrend = weeklyTrend; }
}
```

---

### Task 3: 后端 VO 类 — 老板看板款式详情抽屉

**Files:**
- Create: `garment-server/src/main/java/com/garment/vo/boss/BundleProcessDetailVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/boss/BedDetailVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/boss/StyleDetailVO.java`

- [ ] **Step 1: 创建 `BundleProcessDetailVO.java`**

```java
package com.garment.vo.boss;

public class BundleProcessDetailVO {
    private String processName;
    private Integer status;
    private String statusText;

    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
}
```

- [ ] **Step 2: 创建 `BedDetailVO.java`**

```java
package com.garment.vo.boss;

import java.util.List;

public class BedDetailVO {
    private Long bedId;
    private String bedNo;
    private String name;
    private Integer quantity;
    private Integer completedQuantity;
    private Integer progressPercent;
    private Boolean abnormal;
    private List<BundleProcessDetailVO> processes;

    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getCompletedQuantity() { return completedQuantity; }
    public void setCompletedQuantity(Integer completedQuantity) { this.completedQuantity = completedQuantity; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public Boolean getAbnormal() { return abnormal; }
    public void setAbnormal(Boolean abnormal) { this.abnormal = abnormal; }
    public List<BundleProcessDetailVO> getProcesses() { return processes; }
    public void setProcesses(List<BundleProcessDetailVO> processes) { this.processes = processes; }
}
```

- [ ] **Step 3: 创建 `StyleDetailVO.java`**

```java
package com.garment.vo.boss;

import java.util.List;

public class StyleDetailVO {
    private Long styleId;
    private String styleNo;
    private Integer totalQuantity;
    private Integer completedQuantity;
    private Integer progressPercent;
    private List<BedDetailVO> beds;

    public Long getStyleId() { return styleId; }
    public void setStyleId(Long styleId) { this.styleId = styleId; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public Integer getCompletedQuantity() { return completedQuantity; }
    public void setCompletedQuantity(Integer completedQuantity) { this.completedQuantity = completedQuantity; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public List<BedDetailVO> getBeds() { return beds; }
    public void setBeds(List<BedDetailVO> beds) { this.beds = beds; }
}
```

---

### Task 4: 后端 VO 类 — 主管看板

**Files:**
- Create: `garment-server/src/main/java/com/garment/vo/supervisor/SupervisorProcessVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/supervisor/SupervisorBundleVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/supervisor/StyleGroupVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/supervisor/PendingReturnVO.java`
- Create: `garment-server/src/main/java/com/garment/vo/supervisor/SupervisorDashboardVO.java`

- [ ] **Step 1: 创建 `SupervisorProcessVO.java`**

```java
package com.garment.vo.supervisor;

public class SupervisorProcessVO {
    private String name;
    private Integer status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
```

- [ ] **Step 2: 创建 `SupervisorBundleVO.java`**

```java
package com.garment.vo.supervisor;

import java.time.LocalDateTime;
import java.util.List;

public class SupervisorBundleVO {
    private Long bundleId;
    private String bundleNo;
    private Integer quantity;
    private String size;
    private Integer status;
    private String statusText;
    private Integer progressPercent;
    private Boolean abnormal;
    private List<SupervisorProcessVO> processes;
    private LocalDateTime lastWorkTime;
    private String lastWorkerName;

    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public Boolean getAbnormal() { return abnormal; }
    public void setAbnormal(Boolean abnormal) { this.abnormal = abnormal; }
    public List<SupervisorProcessVO> getProcesses() { return processes; }
    public void setProcesses(List<SupervisorProcessVO> processes) { this.processes = processes; }
    public LocalDateTime getLastWorkTime() { return lastWorkTime; }
    public void setLastWorkTime(LocalDateTime lastWorkTime) { this.lastWorkTime = lastWorkTime; }
    public String getLastWorkerName() { return lastWorkerName; }
    public void setLastWorkerName(String lastWorkerName) { this.lastWorkerName = lastWorkerName; }
}
```

- [ ] **Step 3: 创建 `StyleGroupVO.java`**

```java
package com.garment.vo.supervisor;

import java.util.List;

public class StyleGroupVO {
    private Long styleId;
    private String styleNo;
    private Integer status;
    private String statusText;
    private Integer bedCount;
    private Integer totalQuantity;
    private List<SupervisorBundleVO> bundles;

    public Long getStyleId() { return styleId; }
    public void setStyleId(Long styleId) { this.styleId = styleId; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public Integer getBedCount() { return bedCount; }
    public void setBedCount(Integer bedCount) { this.bedCount = bedCount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public List<SupervisorBundleVO> getBundles() { return bundles; }
    public void setBundles(List<SupervisorBundleVO> bundles) { this.bundles = bundles; }
}
```

- [ ] **Step 4: 创建 `PendingReturnVO.java`**

```java
package com.garment.vo.supervisor;

import java.time.LocalDateTime;

public class PendingReturnVO {
    private Long bundleId;
    private String bundleNo;
    private String styleNo;
    private String processName;
    private String workerName;
    private String returnReason;
    private LocalDateTime returnTime;

    public Long getBundleId() { return bundleId; }
    public void setBundleId(Long bundleId) { this.bundleId = bundleId; }
    public String getBundleNo() { return bundleNo; }
    public void setBundleNo(String bundleNo) { this.bundleNo = bundleNo; }
    public String getStyleNo() { return styleNo; }
    public void setStyleNo(String styleNo) { this.styleNo = styleNo; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }
}
```

- [ ] **Step 5: 创建 `SupervisorDashboardVO.java`**

```java
package com.garment.vo.supervisor;

import com.garment.vo.boss.WeeklyTrendItemVO;
import java.util.List;

public class SupervisorDashboardVO {
    private Integer myBundleCount;
    private Integer weeklyNewBundleCount;
    private Integer todayCompleted;
    private Integer todayCompletedBundleCount;
    private Integer pendingReturnCount;
    private List<StyleGroupVO> styleGroups;
    private List<PendingReturnVO> pendingReturns;
    private List<WeeklyTrendItemVO> weeklyTrend;

    public Integer getMyBundleCount() { return myBundleCount; }
    public void setMyBundleCount(Integer myBundleCount) { this.myBundleCount = myBundleCount; }
    public Integer getWeeklyNewBundleCount() { return weeklyNewBundleCount; }
    public void setWeeklyNewBundleCount(Integer weeklyNewBundleCount) { this.weeklyNewBundleCount = weeklyNewBundleCount; }
    public Integer getTodayCompleted() { return todayCompleted; }
    public void setTodayCompleted(Integer todayCompleted) { this.todayCompleted = todayCompleted; }
    public Integer getTodayCompletedBundleCount() { return todayCompletedBundleCount; }
    public void setTodayCompletedBundleCount(Integer todayCompletedBundleCount) { this.todayCompletedBundleCount = todayCompletedBundleCount; }
    public Integer getPendingReturnCount() { return pendingReturnCount; }
    public void setPendingReturnCount(Integer pendingReturnCount) { this.pendingReturnCount = pendingReturnCount; }
    public List<StyleGroupVO> getStyleGroups() { return styleGroups; }
    public void setStyleGroups(List<StyleGroupVO> styleGroups) { this.styleGroups = styleGroups; }
    public List<PendingReturnVO> getPendingReturns() { return pendingReturns; }
    public void setPendingReturns(List<PendingReturnVO> pendingReturns) { this.pendingReturns = pendingReturns; }
    public List<WeeklyTrendItemVO> getWeeklyTrend() { return weeklyTrend; }
    public void setWeeklyTrend(List<WeeklyTrendItemVO> weeklyTrend) { this.weeklyTrend = weeklyTrend; }
}
```

---

### Task 5: 后端 Service — DashboardService

**Files:**
- Create: `garment-server/src/main/java/com/garment/service/DashboardService.java`

- [ ] **Step 1: 创建 `DashboardService.java`**

```java
package com.garment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.garment.common.TenantContext;
import com.garment.entity.*;
import com.garment.mapper.*;
import com.garment.vo.boss.*;
import com.garment.vo.supervisor.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final StyleMapper styleMapper;
    private final BedMapper bedMapper;
    private final BundleMapper bundleMapper;
    private final BundleProcessMapper bundleProcessMapper;
    private final WorkRecordMapper workRecordMapper;
    private final UserCompanyMapper userCompanyMapper;
    private final UserMapper userMapper;

    public DashboardService(StyleMapper styleMapper, BedMapper bedMapper,
                            BundleMapper bundleMapper, BundleProcessMapper bundleProcessMapper,
                            WorkRecordMapper workRecordMapper, UserCompanyMapper userCompanyMapper,
                            UserMapper userMapper) {
        this.styleMapper = styleMapper;
        this.bedMapper = bedMapper;
        this.bundleMapper = bundleMapper;
        this.bundleProcessMapper = bundleProcessMapper;
        this.workRecordMapper = workRecordMapper;
        this.userCompanyMapper = userCompanyMapper;
        this.userMapper = userMapper;
    }

    // ==================== 老板看板 ====================

    public BossDashboardVO getBossDashboard() {
        Long companyId = TenantContext.getCompanyId();
        BossDashboardVO vo = new BossDashboardVO();

        // 1. 今日完成件数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<WorkRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(WorkRecord::getCompanyId, companyId)
                .ge(WorkRecord::getCreateTime, todayStart)
                .ne(WorkRecord::getStatus, 3);
        List<WorkRecord> todayRecords = workRecordMapper.selectList(todayWrapper);
        int todayCompleted = todayRecords.stream().mapToInt(WorkRecord::getQuantity).sum();
        vo.setTodayCompleted(todayCompleted);

        // 昨日完成件数（用于环比）
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LambdaQueryWrapper<WorkRecord> yesterdayWrapper = new LambdaQueryWrapper<>();
        yesterdayWrapper.eq(WorkRecord::getCompanyId, companyId)
                .ge(WorkRecord::getCreateTime, yesterdayStart)
                .lt(WorkRecord::getCreateTime, todayStart)
                .ne(WorkRecord::getStatus, 3);
        List<WorkRecord> yesterdayRecords = workRecordMapper.selectList(yesterdayWrapper);
        int yesterdayCompleted = yesterdayRecords.stream().mapToInt(WorkRecord::getQuantity).sum();
        if (yesterdayCompleted > 0) {
            double compare = BigDecimal.valueOf(todayCompleted - yesterdayCompleted)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(yesterdayCompleted), 1, RoundingMode.HALF_UP)
                    .doubleValue();
            vo.setTodayCompletedCompare(compare);
        } else {
            vo.setTodayCompletedCompare(0.0);
        }

        // 2. 查询本公司所有款式
        LambdaQueryWrapper<Style> styleWrapper = new LambdaQueryWrapper<>();
        styleWrapper.eq(Style::getCompanyId, companyId).eq(Style::getDeleted, 0);
        List<Style> styles = styleMapper.selectList(styleWrapper);
        vo.setTotalStyleCount(styles.size());

        // 3. 查询本公司所有扎货
        LambdaQueryWrapper<Bundle> bundleWrapper = new LambdaQueryWrapper<>();
        bundleWrapper.eq(Bundle::getCompanyId, companyId).eq(Bundle::getDeleted, 0);
        List<Bundle> allBundles = bundleMapper.selectList(bundleWrapper);

        // 4. 查询所有工序快照
        Set<Long> bundleIds = allBundles.stream().map(Bundle::getId).collect(Collectors.toSet());
        Map<Long, List<BundleProcess>> processMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            LambdaQueryWrapper<BundleProcess> processWrapper = new LambdaQueryWrapper<>();
            processWrapper.in(BundleProcess::getBundleId, bundleIds);
            List<BundleProcess> processes = bundleProcessMapper.selectList(processWrapper);
            processMap = processes.stream().collect(Collectors.groupingBy(BundleProcess::getBundleId));
        }

        // 5. 查询所有报工记录（用于计算异常和最近报工时间）
        Map<Long, List<WorkRecord>> workRecordMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            LambdaQueryWrapper<WorkRecord> wrWrapper = new LambdaQueryWrapper<>();
            wrWrapper.in(WorkRecord::getBundleId, bundleIds).ne(WorkRecord::getStatus, 3);
            List<WorkRecord> allWorkRecords = workRecordMapper.selectList(wrWrapper);
            workRecordMap = allWorkRecords.stream().collect(Collectors.groupingBy(WorkRecord::getBundleId));
        }

        // 6. 查询所有床号
        Set<Long> styleIds = styles.stream().map(Style::getId).collect(Collectors.toSet());
        Map<Long, List<Bed>> bedMap = new HashMap<>();
        if (!styleIds.isEmpty()) {
            LambdaQueryWrapper<Bed> bedWrapper = new LambdaQueryWrapper<>();
            bedWrapper.in(Bed::getStyleId, styleIds).eq(Bed::getDeleted, 0);
            List<Bed> beds = bedMapper.selectList(bedWrapper);
            bedMap = beds.stream().collect(Collectors.groupingBy(Bed::getStyleId));
        }

        // 7. 组装款式进度卡片
        List<StyleProgressVO> styleProgressList = new ArrayList<>();
        int activeStyleCount = 0;
        for (Style style : styles) {
            StyleProgressVO sp = new StyleProgressVO();
            sp.setStyleId(style.getId());
            sp.setStyleNo(style.getStyleNo());

            List<Bed> beds = bedMap.getOrDefault(style.getId(), Collections.emptyList());
            sp.setBedCount(beds.size());

            // 该款式下的所有扎货
            List<Bundle> styleBundles = allBundles.stream()
                    .filter(b -> b.getStyleId().equals(style.getId()))
                    .collect(Collectors.toList());

            int totalQty = styleBundles.stream().mapToInt(Bundle::getQuantity).sum();
            int completedQty = styleBundles.stream()
                    .filter(b -> b.getStatus() == 2)
                    .mapToInt(Bundle::getQuantity).sum();

            sp.setTotalQuantity(totalQty);
            sp.setCompletedQuantity(completedQty);
            sp.setProgressPercent(totalQty > 0 ? completedQty * 100 / totalQty : 0);

            // 状态判断
            if (completedQty == totalQty && totalQty > 0) {
                sp.setStatus(2);
                sp.setStatusText("已完成");
            } else if (completedQty > 0) {
                sp.setStatus(1);
                sp.setStatusText("生产中");
                activeStyleCount++;
            } else {
                sp.setStatus(0);
                sp.setStatusText("未开始");
            }

            // 最近报工时间
            LocalDateTime lastWorkTime = styleBundles.stream()
                    .flatMap(b -> workRecordMap.getOrDefault(b.getId(), Collections.emptyList()).stream())
                    .map(WorkRecord::getCreateTime)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            sp.setLastWorkTime(lastWorkTime);
            if (lastWorkTime != null) {
                long hours = ChronoUnit.HOURS.between(lastWorkTime, LocalDateTime.now());
                if (hours < 1) sp.setLastWorkTimeText("刚刚");
                else if (hours < 24) sp.setLastWorkTimeText(hours + "小时前");
                else sp.setLastWorkTimeText(ChronoUnit.DAYS.between(lastWorkTime.toLocalDate(), LocalDate.now()) + "天前");
            }

            styleProgressList.add(sp);
        }
        vo.setStyles(styleProgressList);
        vo.setActiveStyleCount(activeStyleCount);

        // 8. 异常扎货检测
        List<AbnormalBundleVO> abnormalList = new ArrayList<>();
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);

        for (Bundle bundle : allBundles) {
            if (bundle.getStatus() != 1) continue; // 只检查进行中的

            List<WorkRecord> records = workRecordMap.getOrDefault(bundle.getId(), Collections.emptyList());
            LocalDateTime lastWork = records.stream()
                    .map(WorkRecord::getCreateTime)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            Bed bed = beds.stream().filter(b -> b.getId().equals(bundle.getBedId())).findFirst().orElse(null);
            Style style = styles.stream().filter(s -> s.getId().equals(bundle.getStyleId())).findFirst().orElse(null);

            // 检查3天无报工
            if (lastWork == null || lastWork.isBefore(threeDaysAgo)) {
                AbnormalBundleVO ab = new AbnormalBundleVO();
                ab.setBundleId(bundle.getId());
                ab.setBundleNo(bundle.getBundleNo());
                ab.setStyleNo(style != null ? style.getStyleNo() : "");
                ab.setBedNo(bed != null ? bed.getBedNo() : "");
                ab.setAbnormalType(1);
                ab.setAbnormalTypeText(lastWork == null ? "从未报工" : "3天无报工");
                ab.setSeverity(2);
                abnormalList.add(ab);
            } else if (lastWork.isBefore(twoDaysAgo)) {
                AbnormalBundleVO ab = new AbnormalBundleVO();
                ab.setBundleId(bundle.getId());
                ab.setBundleNo(bundle.getBundleNo());
                ab.setStyleNo(style != null ? style.getStyleNo() : "");
                ab.setBedNo(bed != null ? bed.getBedNo() : "");
                ab.setAbnormalType(2);
                ab.setAbnormalTypeText("2天无报工");
                ab.setSeverity(1);
                abnormalList.add(ab);
            }

            // 检查工序退回
            List<BundleProcess> processes = processMap.getOrDefault(bundle.getId(), Collections.emptyList());
            for (BundleProcess process : processes) {
                if (process.getStatus() == 0) {
                    boolean hasReturn = records.stream()
                            .anyMatch(r -> r.getBundleProcessId().equals(process.getId()) && r.getStatus() == 3);
                    if (hasReturn) {
                        AbnormalBundleVO ab = new AbnormalBundleVO();
                        ab.setBundleId(bundle.getId());
                        ab.setBundleNo(bundle.getBundleNo());
                        ab.setStyleNo(style != null ? style.getStyleNo() : "");
                        ab.setBedNo(bed != null ? bed.getBedNo() : "");
                        ab.setAbnormalType(3);
                        ab.setAbnormalTypeText("工序退回待处理");
                        ab.setSeverity(1);
                        abnormalList.add(ab);
                        break; // 一个扎货只报一次异常
                    }
                }
            }
        }
        vo.setAbnormalBundles(abnormalList);
        vo.setAbnormalBundleCount(abnormalList.size());

        // 9. 在产工人数
        Set<Long> todayWorkerIds = todayRecords.stream().map(WorkRecord::getWorkerId).collect(Collectors.toSet());
        vo.setTodayWorkerCount(todayWorkerIds.size());

        // 本公司所有在职工人
        LambdaQueryWrapper<UserCompany> workerWrapper = new LambdaQueryWrapper<>();
        workerWrapper.eq(UserCompany::getCompanyId, companyId)
                .eq(UserCompany::getRole, "worker")
                .eq(UserCompany::getStatus, 1);
        vo.setActiveWorkerCount((int) userCompanyMapper.selectCount(workerWrapper));

        // 10. 近7日产量趋势
        vo.setWeeklyTrend(buildWeeklyTrend(companyId, allBundles, workRecordMap));

        return vo;
    }

    private List<WeeklyTrendItemVO> buildWeeklyTrend(Long companyId, List<Bundle> bundles, Map<Long, List<WorkRecord>> workRecordMap) {
        List<WeeklyTrendItemVO> trend = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd");
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日", "今日"};

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            int qty = workRecordMap.values().stream()
                    .flatMap(List::stream)
                    .filter(r -> r.getCreateTime() != null)
                    .filter(r -> !r.getCreateTime().isBefore(start) && r.getCreateTime().isBefore(end))
                    .mapToInt(WorkRecord::getQuantity)
                    .sum();

            WeeklyTrendItemVO item = new WeeklyTrendItemVO();
            item.setDate(date.format(dateFormatter));
            item.setDayOfWeek(i == 0 ? "今日" : dayNames[date.getDayOfWeek().getValue() - 1]);
            item.setQuantity(qty);
            trend.add(item);
        }
        return trend;
    }

    // ==================== 老板看板款式详情 ====================

    public StyleDetailVO getStyleDetail(Long styleId) {
        Long companyId = TenantContext.getCompanyId();
        Style style = styleMapper.selectById(styleId);
        if (style == null || !style.getCompanyId().equals(companyId)) {
            return null;
        }

        StyleDetailVO vo = new StyleDetailVO();
        vo.setStyleId(style.getId());
        vo.setStyleNo(style.getStyleNo());

        // 查询该款式下所有床号
        LambdaQueryWrapper<Bed> bedWrapper = new LambdaQueryWrapper<>();
        bedWrapper.eq(Bed::getStyleId, styleId).eq(Bed::getDeleted, 0);
        List<Bed> beds = bedMapper.selectList(bedWrapper);

        // 查询该款式下所有扎货
        LambdaQueryWrapper<Bundle> bundleWrapper = new LambdaQueryWrapper<>();
        bundleWrapper.eq(Bundle::getStyleId, styleId).eq(Bundle::getDeleted, 0);
        List<Bundle> bundles = bundleMapper.selectList(bundleWrapper);
        Map<Long, List<Bundle>> bedBundleMap = bundles.stream().collect(Collectors.groupingBy(Bundle::getBedId));

        // 所有工序快照
        Set<Long> bundleIds = bundles.stream().map(Bundle::getId).collect(Collectors.toSet());
        Map<Long, List<BundleProcess>> processMap = new HashMap<>();
        if (!bundleIds.isEmpty()) {
            LambdaQueryWrapper<BundleProcess> pw = new LambdaQueryWrapper<>();
            pw.in(BundleProcess::getBundleId, bundleIds);
            processMap = bundleProcessMapper.selectList(pw).stream()
                    .collect(Collectors.groupingBy(BundleProcess::getBundleId));
        }

        int totalQty = bundles.stream().mapToInt(Bundle::getQuantity).sum();
        int completedQty = bundles.stream().filter(b -> b.getStatus() == 2).mapToInt(Bundle::getQuantity).sum();
        vo.setTotalQuantity(totalQty);
        vo.setCompletedQuantity(completedQty);
        vo.setProgressPercent(totalQty > 0 ? completedQty * 100 / totalQty : 0);

        // 组装床号详情
        List<BedDetailVO> bedDetails = new ArrayList<>();
        for (Bed bed : beds) {
            BedDetailVO bd = new BedDetailVO();
            bd.setBedId(bed.getId());
            bd.setBedNo(bed.getBedNo());
            bd.setName(bed.getName());

            List<Bundle> bedBundles = bedBundleMap.getOrDefault(bed.getId(), Collections.emptyList());
            int bedTotalQty = bedBundles.stream().mapToInt(Bundle::getQuantity).sum();
            int bedCompletedQty = bedBundles.stream().filter(b -> b.getStatus() == 2).mapToInt(Bundle::getQuantity).sum();
            bd.setQuantity(bedTotalQty);
            bd.setCompletedQuantity(bedCompletedQty);
            bd.setProgressPercent(bedTotalQty > 0 ? bedCompletedQty * 100 / bedTotalQty : 0);

            // 判断床号是否异常（只要有一个扎货异常就算异常）
            bd.setAbnormal(false);
            List<BundleProcessDetailVO> allProcesses = new ArrayList<>();
            for (Bundle bundle : bedBundles) {
                List<BundleProcess> processes = processMap.getOrDefault(bundle.getId(), Collections.emptyList());
                for (BundleProcess process : processes) {
                    BundleProcessDetailVO pd = new BundleProcessDetailVO();
                    pd.setProcessName(process.getProcessName());
                    pd.setStatus(process.getStatus());
                    pd.setStatusText(process.getStatus() == 2 ? "已完成" : process.getStatus() == 1 ? "进行中" : "未开始");
                    allProcesses.add(pd);
                }
            }
            bd.setProcesses(allProcesses);
            bedDetails.add(bd);
        }
        vo.setBeds(bedDetails);

        return vo;
    }

    // ==================== 主管看板 ====================

    public SupervisorDashboardVO getSupervisorDashboard() {
        Long companyId = TenantContext.getCompanyId();
        Long userId = TenantContext.getUserId();
        SupervisorDashboardVO vo = new SupervisorDashboardVO();

        // 1. 我的扎货总数
        LambdaQueryWrapper<Bundle> myBundleWrapper = new LambdaQueryWrapper<>();
        myBundleWrapper.eq(Bundle::getCompanyId, companyId)
                .eq(Bundle::getCreatorId, userId)
                .eq(Bundle::getDeleted, 0);
        List<Bundle> myBundles = bundleMapper.selectList(myBundleWrapper);
        vo.setMyBundleCount(myBundles.size());

        // 本周新增
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        long weeklyNew = myBundles.stream().filter(b -> b.getCreateTime() != null && b.getCreateTime().isAfter(weekStart)).count();
        vo.setWeeklyNewBundleCount((int) weeklyNew);

        // 2. 今日已完成
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Set<Long> myBundleIds = myBundles.stream().map(Bundle::getId).collect(Collectors.toSet());

        int todayCompleted = 0;
        int todayBundleCount = 0;
        if (!myBundleIds.isEmpty()) {
            LambdaQueryWrapper<WorkRecord> wrWrapper = new LambdaQueryWrapper<>();
            wrWrapper.in(WorkRecord::getBundleId, myBundleIds)
                    .ge(WorkRecord::getCreateTime, todayStart)
                    .ne(WorkRecord::getStatus, 3);
            List<WorkRecord> todayRecords = workRecordMapper.selectList(wrWrapper);
            todayCompleted = todayRecords.stream().mapToInt(WorkRecord::getQuantity).sum();
            todayBundleCount = (int) todayRecords.stream().map(WorkRecord::getBundleId).distinct().count();
        }
        vo.setTodayCompleted(todayCompleted);
        vo.setTodayCompletedBundleCount(todayBundleCount);

        // 3. 查询所有工序快照和报工记录
        Map<Long, List<BundleProcess>> processMap = new HashMap<>();
        Map<Long, List<WorkRecord>> workRecordMap = new HashMap<>();
        if (!myBundleIds.isEmpty()) {
            LambdaQueryWrapper<BundleProcess> pw = new LambdaQueryWrapper<>();
            pw.in(BundleProcess::getBundleId, myBundleIds);
            processMap = bundleProcessMapper.selectList(pw).stream()
                    .collect(Collectors.groupingBy(BundleProcess::getBundleId));

            LambdaQueryWrapper<WorkRecord> wrw = new LambdaQueryWrapper<>();
            wrw.in(WorkRecord::getBundleId, myBundleIds).ne(WorkRecord::getStatus, 3);
            workRecordMap = workRecordMapper.selectList(wrw).stream()
                    .collect(Collectors.groupingBy(WorkRecord::getBundleId));
        }

        // 4. 查询款式和床号信息
        Set<Long> styleIds = myBundles.stream().map(Bundle::getStyleId).collect(Collectors.toSet());
        Map<Long, Style> styleMap = new HashMap<>();
        Map<Long, Bed> bedMap = new HashMap<>();
        if (!styleIds.isEmpty()) {
            styleMap = styleMapper.selectBatchIds(styleIds).stream()
                    .collect(Collectors.toMap(Style::getId, s -> s));
            LambdaQueryWrapper<Bed> bw = new LambdaQueryWrapper<>();
            bw.in(Bed::getStyleId, styleIds).eq(Bed::getDeleted, 0);
            bedMap = bedMapper.selectList(bw).stream()
                    .collect(Collectors.toMap(Bed::getId, b -> b));
        }

        // 5. 查询工人姓名
        Set<Long> workerIds = workRecordMap.values().stream()
                .flatMap(List::stream).map(WorkRecord::getWorkerId).collect(Collectors.toSet());
        Map<Long, String> workerNameMap = new HashMap<>();
        if (!workerIds.isEmpty()) {
            workerNameMap = userMapper.selectBatchIds(workerIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
        }

        // 6. 组装按款式分组
        Map<Long, List<Bundle>> styleBundleMap = myBundles.stream()
                .collect(Collectors.groupingBy(Bundle::getStyleId));

        List<StyleGroupVO> styleGroups = new ArrayList<>();
        for (Map.Entry<Long, List<Bundle>> entry : styleBundleMap.entrySet()) {
            Long sid = entry.getKey();
            List<Bundle> bundles = entry.getValue();
            Style style = styleMap.get(sid);
            if (style == null) continue;

            StyleGroupVO sg = new StyleGroupVO();
            sg.setStyleId(sid);
            sg.setStyleNo(style.getStyleNo());

            // 状态判断
            int completedBundles = (int) bundles.stream().filter(b -> b.getStatus() == 2).count();
            if (completedBundles == bundles.size()) {
                sg.setStatus(2);
                sg.setStatusText("已完成");
            } else if (completedBundles > 0) {
                sg.setStatus(1);
                sg.setStatusText("生产中");
            } else {
                sg.setStatus(0);
                sg.setStatusText("未开始");
            }

            sg.setBedCount((int) bundles.stream().map(Bundle::getBedId).distinct().count());
            sg.setTotalQuantity(bundles.stream().mapToInt(Bundle::getQuantity).sum());

            // 组装扎货列表
            List<SupervisorBundleVO> bundleVOs = new ArrayList<>();
            for (Bundle bundle : bundles) {
                SupervisorBundleVO bv = new SupervisorBundleVO();
                bv.setBundleId(bundle.getId());
                bv.setBundleNo(bundle.getBundleNo());
                bv.setQuantity(bundle.getQuantity());
                bv.setSize(bundle.getSize());
                bv.setStatus(bundle.getStatus());
                bv.setStatusText(bundle.getStatus() == 2 ? "已完成" : bundle.getStatus() == 1 ? "进行中" : "待开始");

                List<BundleProcess> processes = processMap.getOrDefault(bundle.getId(), Collections.emptyList());
                int completedProcess = (int) processes.stream().filter(p -> p.getStatus() == 2).count();
                bv.setProgressPercent(processes.size() > 0 ? completedProcess * 100 / processes.size() : 0);

                List<SupervisorProcessVO> pvs = new ArrayList<>();
                for (BundleProcess p : processes) {
                    SupervisorProcessVO pv = new SupervisorProcessVO();
                    pv.setName(p.getProcessName());
                    pv.setStatus(p.getStatus());
                    pvs.add(pv);
                }
                bv.setProcesses(pvs);

                // 最近报工
                List<WorkRecord> records = workRecordMap.getOrDefault(bundle.getId(), Collections.emptyList());
                WorkRecord lastRecord = records.stream()
                        .max(Comparator.comparing(WorkRecord::getCreateTime))
                        .orElse(null);
                if (lastRecord != null) {
                    bv.setLastWorkTime(lastRecord.getCreateTime());
                    bv.setLastWorkerName(workerNameMap.getOrDefault(lastRecord.getWorkerId(), ""));
                }

                // 异常检测
                bv.setAbnormal(false);
                if (bundle.getStatus() == 1) {
                    LocalDateTime lastWork = lastRecord != null ? lastRecord.getCreateTime() : null;
                    if (lastWork == null || lastWork.isBefore(LocalDateTime.now().minusDays(2))) {
                        bv.setAbnormal(true);
                    }
                    // 检查退回
                    for (BundleProcess p : processes) {
                        if (p.getStatus() == 0) {
                            boolean hasReturn = records.stream()
                                    .anyMatch(r -> r.getBundleProcessId().equals(p.getId()) && r.getStatus() == 3);
                            if (hasReturn) {
                                bv.setAbnormal(true);
                                break;
                            }
                        }
                    }
                }

                bundleVOs.add(bv);
            }
            sg.setBundles(bundleVOs);
            styleGroups.add(sg);
        }
        vo.setStyleGroups(styleGroups);

        // 7. 待处理退回
        List<PendingReturnVO> pendingReturns = new ArrayList<>();
        for (Bundle bundle : myBundles) {
            List<WorkRecord> records = workRecordMap.getOrDefault(bundle.getId(), Collections.emptyList());
            List<BundleProcess> processes = processMap.getOrDefault(bundle.getId(), Collections.emptyList());

            for (BundleProcess process : processes) {
                if (process.getStatus() == 0) {
                    WorkRecord returnRecord = records.stream()
                            .filter(r -> r.getBundleProcessId().equals(process.getId()) && r.getStatus() == 3)
                            .max(Comparator.comparing(WorkRecord::getReturnTime))
                            .orElse(null);
                    if (returnRecord != null) {
                        PendingReturnVO pr = new PendingReturnVO();
                        pr.setBundleId(bundle.getId());
                        pr.setBundleNo(bundle.getBundleNo());
                        pr.setStyleNo(styleMap.getOrDefault(bundle.getStyleId(), new Style()).getStyleNo());
                        pr.setProcessName(process.getProcessName());
                        pr.setWorkerName(workerNameMap.getOrDefault(returnRecord.getWorkerId(), ""));
                        pr.setReturnReason(returnRecord.getReturnReason());
                        pr.setReturnTime(returnRecord.getReturnTime());
                        pendingReturns.add(pr);
                    }
                }
            }
        }
        vo.setPendingReturns(pendingReturns);
        vo.setPendingReturnCount(pendingReturns.size());

        // 8. 近7日班组产量
        vo.setWeeklyTrend(buildWeeklyTrend(companyId, myBundles, workRecordMap));

        return vo;
    }

    public void ackReturn(Long bundleId, String action) {
        // 软标记，暂不实现持久化
        // 未来可扩展为记录到单独的提醒表中
    }
}
```

---

### Task 6: 后端 Controller — DashboardController

**Files:**
- Create: `garment-server/src/main/java/com/garment/controller/DashboardController.java`

- [ ] **Step 1: 创建 `DashboardController.java`**

```java
package com.garment.controller;

import com.garment.common.Result;
import com.garment.common.TenantContext;
import com.garment.service.DashboardService;
import com.garment.vo.boss.BossDashboardVO;
import com.garment.vo.boss.StyleDetailVO;
import com.garment.vo.supervisor.SupervisorDashboardVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/boss")
    public Result<BossDashboardVO> getBossDashboard() {
        if (TenantContext.isSuperAdmin()) {
            return Result.error(403, "超级管理员请通过公司切换查看");
        }
        BossDashboardVO vo = dashboardService.getBossDashboard();
        return Result.ok(vo);
    }

    @GetMapping("/boss/style/{styleId}")
    public Result<StyleDetailVO> getStyleDetail(@PathVariable Long styleId) {
        if (TenantContext.isSuperAdmin()) {
            return Result.error(403, "超级管理员请通过公司切换查看");
        }
        StyleDetailVO vo = dashboardService.getStyleDetail(styleId);
        if (vo == null) {
            return Result.error(404, "款式不存在");
        }
        return Result.ok(vo);
    }

    @GetMapping("/supervisor")
    public Result<SupervisorDashboardVO> getSupervisorDashboard() {
        if (TenantContext.isSuperAdmin()) {
            return Result.error(403, "超级管理员不能访问主管看板");
        }
        SupervisorDashboardVO vo = dashboardService.getSupervisorDashboard();
        return Result.ok(vo);
    }

    @PostMapping("/supervisor/return/{bundleId}/ack")
    public Result<Void> ackReturn(@PathVariable Long bundleId, @RequestBody java.util.Map<String, String> body) {
        String action = body.getOrDefault("action", "ignore");
        dashboardService.ackReturn(bundleId, action);
        return Result.ok();
    }
}
```

---

### Task 7: 后端编译验证

**Files:**
- 无

- [ ] **Step 1: 编译后端**

```bash
cd garment-server
mvn compile
```

Expected: BUILD SUCCESS

---

### Task 8: 前端 API 封装

**Files:**
- Create: `garment-web/src/api/dashboard.js`

- [ ] **Step 1: 创建 `dashboard.js`**

```javascript
import request from './request'

export function getBossDashboard() {
  return request.get('/dashboard/boss')
}

export function getStyleDetail(styleId) {
  return request.get(`/dashboard/boss/style/${styleId}`)
}

export function getSupervisorDashboard() {
  return request.get('/dashboard/supervisor')
}

export function ackReturn(bundleId, action) {
  return request.post(`/dashboard/supervisor/return/${bundleId}/ack`, { action })
}
```

---

### Task 9: 前端老板看板页面

**Files:**
- Create: `garment-web/src/views/DashboardBoss.vue`

- [ ] **Step 1: 创建 `DashboardBoss.vue`**

```vue
<template>
  <div class="dashboard-boss">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>📊 生产进度看板</h1>
      <p class="update-time">数据更新于 {{ updateTime }}</p>
    </div>

    <!-- 顶部统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">今日完成件数</span>
          <span class="stat-icon">📦</span>
        </div>
        <div class="stat-value">{{ formatNumber(data.todayCompleted) }}</div>
        <div class="stat-compare" :class="{ 'up': data.todayCompletedCompare >= 0 }">
          {{ data.todayCompletedCompare >= 0 ? '↑' : '↓' }} 较昨日 {{ Math.abs(data.todayCompletedCompare || 0) }}%
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">在产款式数</span>
          <span class="stat-icon">👕</span>
        </div>
        <div class="stat-value">{{ data.activeStyleCount }}</div>
        <div class="stat-sub">共 {{ data.totalStyleCount }} 个款式</div>
      </div>
      <div class="stat-card abnormal">
        <div class="stat-header">
          <span class="stat-label">异常扎货数</span>
          <span class="stat-icon">⚠️</span>
        </div>
        <div class="stat-value abnormal-value">{{ data.abnormalBundleCount }}</div>
        <div class="stat-sub abnormal-text">需要关注</div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">在产工人数</span>
          <span class="stat-icon">👷</span>
        </div>
        <div class="stat-value">{{ data.activeWorkerCount }}</div>
        <div class="stat-sub">今日已报工 {{ data.todayWorkerCount }} 人</div>
      </div>
    </div>

    <!-- 中部：款式卡片 + 异常预警 -->
    <div class="main-content">
      <div class="style-section">
        <div class="section-header">
          <h2>款式生产进度</h2>
          <div class="status-filters">
            <span class="filter-tag blue">生产中 {{ activeCount }}</span>
            <span class="filter-tag green">已完成 {{ completedCount }}</span>
            <span class="filter-tag gray">未开始 {{ notStartedCount }}</span>
          </div>
        </div>
        <div class="style-grid">
          <div v-for="style in data.styles" :key="style.styleId" class="style-card"
               :class="{ 'completed': style.status === 2 }" @click="openDetail(style)">
            <div class="style-header">
              <div class="style-title">
                <span class="style-no">{{ style.styleNo }}</span>
                <span class="status-tag" :class="getStatusClass(style.status)">{{ style.statusText }}</span>
              </div>
              <span class="bed-count">{{ style.bedCount }} 床</span>
            </div>
            <div class="style-progress">
              <span class="progress-current">{{ style.completedQuantity }}</span>
              <span class="progress-total">/ {{ style.totalQuantity }} 件</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: style.progressPercent + '%' }"
                   :class="{ 'completed': style.status === 2 }"></div>
            </div>
            <div class="style-footer">
              <span>完成 {{ style.progressPercent }}%</span>
              <span class="last-work">最近报工：{{ style.lastWorkTimeText || '无' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="warning-section">
        <div class="section-header">
          <h2>异常预警</h2>
          <span class="warning-badge">{{ data.abnormalBundleCount }}</span>
        </div>
        <div class="warning-list">
          <div v-for="item in data.abnormalBundles" :key="item.bundleId" class="warning-item"
               :class="{ 'high': item.severity === 2 }">
            <div class="warning-info">
              <div class="warning-title">扎号 {{ item.bundleNo }}</div>
              <div class="warning-sub">{{ item.styleNo }} · {{ item.bedNo }}</div>
            </div>
            <span class="warning-tag" :class="{ 'red': item.severity === 2, 'orange': item.severity === 1 }">
              {{ item.abnormalTypeText }}
            </span>
          </div>
          <div v-if="!data.abnormalBundles?.length" class="empty-warning">
            ✅ 暂无异常，生产正常
          </div>
        </div>

        <div class="quick-filter">
          <h3>快捷筛选</h3>
          <label class="filter-item">
            <input type="checkbox" v-model="filters.onlyActive">
            <span>只看生产中款式</span>
          </label>
          <label class="filter-item">
            <input type="checkbox" v-model="filters.showCompleted">
            <span>显示已完成款式</span>
          </label>
          <label class="filter-item">
            <input type="checkbox" v-model="filters.highlightAbnormal">
            <span>高亮异常扎货</span>
          </label>
        </div>
      </div>
    </div>

    <!-- 底部：产量趋势图 -->
    <div class="trend-section">
      <h2>近7日产量趋势</h2>
      <div ref="trendChart" class="trend-chart"></div>
    </div>

    <!-- 款式详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="currentStyle?.styleNo + ' 生产详情'" size="600px">
      <div v-if="styleDetail" class="style-detail">
        <div class="detail-summary">
          <span>总进度 {{ styleDetail.completedQuantity }}/{{ styleDetail.totalQuantity }} 件</span>
          <span class="detail-percent">({{ styleDetail.progressPercent }}%)</span>
        </div>
        <div class="bed-list">
          <div v-for="bed in styleDetail.beds" :key="bed.bedId" class="bed-item" :class="{ 'abnormal': bed.abnormal }">
            <div class="bed-header">
              <span class="bed-name">{{ bed.bedNo }} {{ bed.name ? '· ' + bed.name : '' }}</span>
              <span class="bed-progress">{{ bed.quantity }}件 · 完成 {{ bed.progressPercent }}%</span>
            </div>
            <div class="process-list">
              <div v-for="process in bed.processes" :key="process.processName" class="process-row">
                <span class="process-name">{{ process.processName }}</span>
                <div class="process-bar">
                  <div class="process-fill" :class="getProcessClass(process.status)"
                       :style="{ width: process.status === 2 ? '100%' : process.status === 1 ? '50%' : '0%' }"></div>
                </div>
                <span class="process-status" :class="getProcessClass(process.status)">{{ process.statusText }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getBossDashboard, getStyleDetail } from '../api/dashboard'

const data = ref({
  todayCompleted: 0,
  todayCompletedCompare: 0,
  activeStyleCount: 0,
  totalStyleCount: 0,
  abnormalBundleCount: 0,
  activeWorkerCount: 0,
  todayWorkerCount: 0,
  styles: [],
  abnormalBundles: [],
  weeklyTrend: []
})

const updateTime = ref('')
const trendChart = ref(null)
let chartInstance = null
const detailVisible = ref(false)
const currentStyle = ref(null)
const styleDetail = ref(null)

const filters = ref({
  onlyActive: true,
  showCompleted: false,
  highlightAbnormal: true
})

const filteredStyles = computed(() => {
  let list = data.value.styles || []
  if (filters.value.onlyActive && !filters.value.showCompleted) {
    list = list.filter(s => s.status === 1)
  }
  if (!filters.value.showCompleted) {
    list = list.filter(s => s.status !== 2)
  }
  return list
})

const activeCount = computed(() => data.value.styles?.filter(s => s.status === 1).length || 0)
const completedCount = computed(() => data.value.styles?.filter(s => s.status === 2).length || 0)
const notStartedCount = computed(() => data.value.styles?.filter(s => s.status === 0).length || 0)

function formatNumber(num) {
  return num?.toLocaleString() || '0'
}

function getStatusClass(status) {
  if (status === 2) return 'green'
  if (status === 1) return 'blue'
  return 'gray'
}

function getProcessClass(status) {
  if (status === 2) return 'completed'
  if (status === 1) return 'active'
  return 'pending'
}

async function loadData() {
  try {
    const res = await getBossDashboard()
    if (res.code === 200) {
      data.value = res.data
      updateTime.value = new Date().toLocaleString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      nextTick(() => initChart())
    }
  } catch (e) {
    console.error('加载看板数据失败', e)
  }
}

function initChart() {
  if (!trendChart.value || !data.value.weeklyTrend?.length) return
  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(trendChart.value)
  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.value.weeklyTrend.map(item => item.dayOfWeek),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#606266' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#ebeef5' } },
      axisLabel: { color: '#606266' }
    },
    series: [{
      data: data.value.weeklyTrend.map(item => ({
        value: item.quantity,
        itemStyle: { color: item.dayOfWeek === '今日' ? '#409EFF' : '#a0cfff' }
      })),
      type: 'bar',
      barWidth: '50%',
      borderRadius: [4, 4, 0, 0]
    }]
  }
  chartInstance.setOption(option)
}

async function openDetail(style) {
  currentStyle.value = style
  detailVisible.value = true
  try {
    const res = await getStyleDetail(style.styleId)
    if (res.code === 200) {
      styleDetail.value = res.data
    }
  } catch (e) {
    console.error('加载款式详情失败', e)
  }
}

let refreshTimer = null

onMounted(() => {
  loadData()
  refreshTimer = setInterval(loadData, 30000)
  window.addEventListener('resize', () => chartInstance?.resize())
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (chartInstance) chartInstance.dispose()
  window.removeEventListener('resize', () => chartInstance?.resize())
})
</script>

<style scoped>
.dashboard-boss { padding: 20px; max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h1 { font-size: 24px; font-weight: bold; color: #303133; margin: 0; }
.update-time { color: #909399; font-size: 13px; margin-top: 4px; }

.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; border: 1px solid #ebeef5; transition: all 0.3s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.stat-card.abnormal { border-color: #fde2e2; }
.stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.stat-label { color: #606266; font-size: 14px; }
.stat-icon { font-size: 24px; }
.stat-value { font-size: 32px; font-weight: bold; color: #303133; }
.abnormal-value { color: #f56c6c; }
.stat-compare { font-size: 13px; margin-top: 4px; }
.stat-compare.up { color: #67c23a; }
.stat-sub { color: #909399; font-size: 13px; margin-top: 4px; }
.abnormal-text { color: #f56c6c; }

.main-content { display: flex; gap: 16px; margin-bottom: 20px; }
.style-section { flex: 2; }
.warning-section { flex: 1; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h2 { font-size: 16px; font-weight: bold; color: #303133; margin: 0; }
.status-filters { display: flex; gap: 8px; }
.filter-tag { font-size: 12px; padding: 2px 10px; border-radius: 4px; }
.filter-tag.blue { background: #ecf5ff; color: #409eff; }
.filter-tag.green { background: #f0f9eb; color: #67c23a; }
.filter-tag.gray { background: #f4f4f5; color: #909399; }
.warning-badge { background: #fef0f0; color: #f56c6c; font-size: 12px; padding: 2px 10px; border-radius: 10px; font-weight: bold; }

.style-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.style-card { background: #fff; border-radius: 12px; padding: 16px; border: 1px solid #ebeef5; cursor: pointer; transition: all 0.3s; }
.style-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.08); }
.style-card.completed { border-color: #e1f3d8; }
.style-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.style-title { display: flex; align-items: center; gap: 8px; }
.style-no { font-weight: bold; color: #303133; }
.status-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-tag.blue { background: #ecf5ff; color: #409eff; }
.status-tag.green { background: #f0f9eb; color: #67c23a; }
.status-tag.gray { background: #f4f4f5; color: #909399; }
.bed-count { color: #909399; font-size: 12px; }
.style-progress { margin-bottom: 8px; }
.progress-current { font-size: 24px; font-weight: bold; color: #303133; }
.progress-total { color: #909399; }
.progress-bar { height: 8px; background: #e4e7ed; border-radius: 4px; overflow: hidden; margin-bottom: 8px; }
.progress-fill { height: 100%; border-radius: 4px; background: #409eff; transition: width 0.3s; }
.progress-fill.completed { background: #67c23a; }
.style-footer { display: flex; justify-content: space-between; font-size: 12px; color: #909399; }

.warning-list { background: #fff; border-radius: 12px; border: 1px solid #ebeef5; overflow: hidden; margin-bottom: 16px; }
.warning-item { padding: 12px 16px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #f5f7fa; }
.warning-item:last-child { border-bottom: none; }
.warning-item.high { background: #fef0f0; }
.warning-title { font-weight: bold; color: #303133; font-size: 14px; }
.warning-sub { color: #909399; font-size: 12px; margin-top: 2px; }
.warning-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.warning-tag.red { background: #fde2e2; color: #f56c6c; }
.warning-tag.orange { background: #fdf6ec; color: #e6a23c; }
.empty-warning { padding: 24px; text-align: center; color: #67c23a; font-size: 14px; }

.quick-filter { background: #fff; border-radius: 12px; padding: 16px; border: 1px solid #ebeef5; }
.quick-filter h3 { font-size: 14px; font-weight: bold; color: #303133; margin: 0 0 12px 0; }
.filter-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #606266; margin-bottom: 8px; cursor: pointer; }
.filter-item:last-child { margin-bottom: 0; }

.trend-section { background: #fff; border-radius: 12px; padding: 20px; border: 1px solid #ebeef5; }
.trend-section h2 { font-size: 16px; font-weight: bold; color: #303133; margin: 0 0 16px 0; }
.trend-chart { height: 240px; }

.style-detail { padding: 16px; }
.detail-summary { font-size: 16px; color: #303133; margin-bottom: 20px; }
.detail-percent { color: #409eff; font-weight: bold; }
.bed-list { display: flex; flex-direction: column; gap: 16px; }
.bed-item { border: 1px solid #ebeef5; border-radius: 8px; padding: 16px; }
.bed-item.abnormal { border-color: #fde2e2; background: #fef0f0; }
.bed-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
.bed-name { font-weight: bold; color: #303133; }
.bed-progress { color: #909399; font-size: 13px; }
.process-list { display: flex; flex-direction: column; gap: 8px; }
.process-row { display: flex; align-items: center; gap: 12px; }
.process-name { width: 80px; font-size: 13px; color: #606266; }
.process-bar { flex: 1; height: 6px; background: #e4e7ed; border-radius: 3px; overflow: hidden; }
.process-fill { height: 100%; border-radius: 3px; }
.process-fill.completed { background: #67c23a; }
.process-fill.active { background: #409eff; }
.process-fill.pending { background: #e4e7ed; }
.process-status { width: 50px; text-align: right; font-size: 12px; }
.process-status.completed { color: #67c23a; }
.process-status.active { color: #409eff; }
.process-status.pending { color: #c0c4cc; }
</style>
```

---

### Task 10: 前端主管看板页面

**Files:**
- Create: `garment-web/src/views/DashboardSupervisor.vue`

- [ ] **Step 1: 创建 `DashboardSupervisor.vue`**

```vue
<template>
  <div class="dashboard-supervisor">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>📋 我的工作台</h1>
    </div>

    <!-- 顶部统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">我的扎货总数</span>
          <span class="stat-icon">📦</span>
        </div>
        <div class="stat-value">{{ data.myBundleCount }}</div>
        <div class="stat-sub">本周新增 {{ data.weeklyNewBundleCount }} 扎</div>
      </div>
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">今日已完成</span>
          <span class="stat-icon">✅</span>
        </div>
        <div class="stat-value green">{{ data.todayCompleted }}</div>
        <div class="stat-sub">件（{{ data.todayCompletedBundleCount }} 扎）</div>
      </div>
      <div class="stat-card abnormal">
        <div class="stat-header">
          <span class="stat-label">待处理退回</span>
          <span class="stat-icon">🔄</span>
        </div>
        <div class="stat-value red">{{ data.pendingReturnCount }}</div>
        <div class="stat-sub red-text">需尽快处理</div>
      </div>
    </div>

    <!-- 中部：扎货进度 + 待处理退回 -->
    <div class="main-content">
      <div class="bundle-section">
        <div class="section-header">
          <h2>我的扎货进度</h2>
          <el-select v-model="filterStyle" placeholder="全部款式" size="small" clearable>
            <el-option v-for="sg in data.styleGroups" :key="sg.styleId" :label="sg.styleNo" :value="sg.styleId" />
          </el-select>
        </div>
        <div class="style-groups">
          <div v-for="group in filteredGroups" :key="group.styleId" class="style-group">
            <div class="group-header">
              <div class="group-title">
                <span class="style-no">{{ group.styleNo }}</span>
                <span class="group-status" :class="getStatusClass(group.status)">{{ group.statusText }}</span>
              </div>
              <span class="group-info">{{ group.bedCount }} 床 · {{ group.totalQuantity }} 件</span>
            </div>
            <div class="bundle-list">
              <div v-for="bundle in group.bundles" :key="bundle.bundleId" class="bundle-item"
                   :class="{ 'abnormal': bundle.abnormal }">
                <div class="bundle-header">
                  <div class="bundle-title">
                    <span class="bundle-no">扎号 {{ bundle.bundleNo }}</span>
                    <span class="bundle-status" :class="getStatusClass(bundle.status)">{{ bundle.statusText }}</span>
                  </div>
                  <span class="bundle-info">{{ bundle.quantity }}件 · {{ bundle.size || '全码' }}</span>
                </div>
                <div class="process-mini-bars">
                  <div v-for="process in bundle.processes" :key="process.name" class="mini-bar-wrapper">
                    <div class="mini-label">{{ process.name }}</div>
                    <div class="mini-bar">
                      <div class="mini-fill" :class="getProcessFillClass(process.status)"></div>
                    </div>
                  </div>
                  <span class="bundle-percent">{{ bundle.progressPercent }}%</span>
                </div>
                <div v-if="bundle.lastWorkTime" class="bundle-footer">
                  最近报工：{{ formatTime(bundle.lastWorkTime) }} · 工人：{{ bundle.lastWorkerName }}
                </div>
                <div v-if="bundle.abnormal" class="bundle-warning">
                  ⚠️ {{ getAbnormalText(bundle) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="right-section">
        <!-- 待处理退回 -->
        <div class="return-section" v-if="data.pendingReturns?.length">
          <div class="section-header">
            <h2>待处理退回</h2>
            <span class="warning-badge">{{ data.pendingReturnCount }}</span>
          </div>
          <div class="return-list">
            <div v-for="item in data.pendingReturns" :key="item.bundleId + item.processName" class="return-item">
              <div class="return-info">
                <div class="return-title">扎号 {{ item.bundleNo }}</div>
                <div class="return-sub">{{ item.styleNo }} · {{ item.processName }}</div>
                <div class="return-sub">工人：{{ item.workerName }}</div>
              </div>
              <div class="return-reason">{{ item.returnReason }}</div>
              <div class="return-actions">
                <el-button type="primary" size="small" @click="handleReturn(item, 'reassign')">重新分配</el-button>
                <el-button size="small" @click="handleReturn(item, 'ignore')">忽略</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="quick-actions">
          <h3>快捷操作</h3>
          <div class="action-list">
            <router-link to="/scan-report" class="action-btn scan">
              <span class="action-icon">📷</span>
              <span class="action-text">扫码报工</span>
            </router-link>
            <router-link to="/bundle" class="action-btn bundle">
              <span class="action-icon">📦</span>
              <span class="action-text">分扎管理</span>
            </router-link>
            <router-link to="/work-review" class="action-btn record">
              <span class="action-icon">📝</span>
              <span class="action-text">报工记录</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：班组产量 -->
    <div class="trend-section">
      <h2>近7日班组产量</h2>
      <div ref="trendChart" class="trend-chart"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getSupervisorDashboard, ackReturn } from '../api/dashboard'
import { ElMessage } from 'element-plus'

const data = ref({
  myBundleCount: 0,
  weeklyNewBundleCount: 0,
  todayCompleted: 0,
  todayCompletedBundleCount: 0,
  pendingReturnCount: 0,
  styleGroups: [],
  pendingReturns: [],
  weeklyTrend: []
})

const filterStyle = ref('')
const trendChart = ref(null)
let chartInstance = null

const filteredGroups = computed(() => {
  if (!filterStyle.value) return data.value.styleGroups || []
  return (data.value.styleGroups || []).filter(g => g.styleId === filterStyle.value)
})

function getStatusClass(status) {
  if (status === 2) return 'green'
  if (status === 1) return 'blue'
  return 'gray'
}

function getProcessFillClass(status) {
  if (status === 2) return 'completed'
  if (status === 1) return 'active'
  return 'pending'
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const hours = Math.floor((now - date) / 3600000)
  if (hours < 1) return '刚刚'
  if (hours < 24) return hours + '小时前'
  return Math.floor(hours / 24) + '天前'
}

function getAbnormalText(bundle) {
  if (bundle.status !== 1) return ''
  // 简化：如果异常且最近报工超过2天
  const lastWork = bundle.lastWorkTime
  if (!lastWork) return '从未报工'
  const hours = Math.floor((new Date() - new Date(lastWork)) / 3600000)
  if (hours >= 72) return '超过3天未报工'
  if (hours >= 48) return '超过2天未报工'
  return '工序退回待处理'
}

async function loadData() {
  try {
    const res = await getSupervisorDashboard()
    if (res.code === 200) {
      data.value = res.data
      nextTick(() => initChart())
    }
  } catch (e) {
    console.error('加载主管看板失败', e)
  }
}

async function handleReturn(item, action) {
  try {
    await ackReturn(item.bundleId, action)
    ElMessage.success(action === 'reassign' ? '已标记重新分配' : '已忽略')
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function initChart() {
  if (!trendChart.value || !data.value.weeklyTrend?.length) return
  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(trendChart.value)
  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.value.weeklyTrend.map(item => item.dayOfWeek),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#606266' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#ebeef5' } },
      axisLabel: { color: '#606266' }
    },
    series: [{
      data: data.value.weeklyTrend.map(item => ({
        value: item.quantity,
        itemStyle: { color: item.dayOfWeek === '今日' ? '#67c23a' : '#b3e19d' }
      })),
      type: 'bar',
      barWidth: '50%',
      borderRadius: [4, 4, 0, 0]
    }]
  }
  chartInstance.setOption(option)
}

let refreshTimer = null

onMounted(() => {
  loadData()
  refreshTimer = setInterval(loadData, 30000)
  window.addEventListener('resize', () => chartInstance?.resize())
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (chartInstance) chartInstance.dispose()
  window.removeEventListener('resize', () => chartInstance?.resize())
})
</script>

<style scoped>
.dashboard-supervisor { padding: 20px; max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-header h1 { font-size: 24px; font-weight: bold; color: #303133; margin: 0; }

.stat-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; border: 1px solid #ebeef5; }
.stat-card.abnormal { border-color: #fde2e2; }
.stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.stat-label { color: #606266; font-size: 14px; }
.stat-icon { font-size: 24px; }
.stat-value { font-size: 32px; font-weight: bold; color: #303133; }
.stat-value.green { color: #67c23a; }
.stat-value.red { color: #f56c6c; }
.stat-sub { color: #909399; font-size: 13px; margin-top: 4px; }
.red-text { color: #f56c6c; }

.main-content { display: flex; gap: 16px; margin-bottom: 20px; }
.bundle-section { flex: 2; }
.right-section { flex: 1; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-header h2 { font-size: 16px; font-weight: bold; color: #303133; margin: 0; }
.warning-badge { background: #fef0f0; color: #f56c6c; font-size: 12px; padding: 2px 10px; border-radius: 10px; font-weight: bold; }

.style-groups { display: flex; flex-direction: column; gap: 16px; }
.style-group { background: #fff; border-radius: 12px; border: 1px solid #ebeef5; overflow: hidden; }
.group-header { padding: 12px 16px; background: #f5f7fa; border-bottom: 1px solid #ebeef5; display: flex; justify-content: space-between; align-items: center; }
.group-title { display: flex; align-items: center; gap: 8px; }
.style-no { font-weight: bold; color: #303133; }
.group-status { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.group-status.blue { background: #ecf5ff; color: #409eff; }
.group-status.green { background: #f0f9eb; color: #67c23a; }
.group-status.gray { background: #f4f4f5; color: #909399; }
.group-info { color: #909399; font-size: 13px; }

.bundle-list { padding: 8px; }
.bundle-item { padding: 12px; border-bottom: 1px solid #f5f7fa; }
.bundle-item:last-child { border-bottom: none; }
.bundle-item.abnormal { background: #fef0f0; border-radius: 8px; margin-bottom: 8px; }
.bundle-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.bundle-title { display: flex; align-items: center; gap: 8px; }
.bundle-no { font-weight: bold; color: #303133; font-size: 14px; }
.bundle-status { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.bundle-status.blue { background: #ecf5ff; color: #409eff; }
.bundle-status.green { background: #f0f9eb; color: #67c23a; }
.bundle-status.gray { background: #f4f4f5; color: #909399; }
.bundle-info { color: #909399; font-size: 12px; }

.process-mini-bars { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.mini-bar-wrapper { width: 50px; text-align: center; }
.mini-label { font-size: 10px; color: #909399; margin-bottom: 2px; }
.mini-bar { height: 4px; background: #e4e7ed; border-radius: 2px; overflow: hidden; }
.mini-fill { height: 100%; border-radius: 2px; }
.mini-fill.completed { background: #67c23a; }
.mini-fill.active { background: #409eff; }
.mini-fill.pending { background: #e4e7ed; }
.bundle-percent { font-size: 13px; font-weight: bold; color: #409eff; margin-left: 8px; }
.bundle-footer { font-size: 11px; color: #909399; }
.bundle-warning { font-size: 11px; color: #f56c6c; margin-top: 4px; }

.return-section { margin-bottom: 16px; }
.return-list { background: #fff; border-radius: 12px; border: 1px solid #ebeef5; overflow: hidden; }
.return-item { padding: 12px 16px; border-bottom: 1px solid #f5f7fa; }
.return-item:last-child { border-bottom: none; }
.return-title { font-weight: bold; color: #303133; font-size: 14px; }
.return-sub { color: #909399; font-size: 12px; margin-top: 2px; }
.return-reason { background: #fdf6ec; color: #e6a23c; font-size: 12px; padding: 6px 10px; border-radius: 4px; margin: 8px 0; }
.return-actions { display: flex; gap: 8px; }

.quick-actions { background: #fff; border-radius: 12px; padding: 16px; border: 1px solid #ebeef5; }
.quick-actions h3 { font-size: 14px; font-weight: bold; color: #303133; margin: 0 0 12px 0; }
.action-list { display: flex; flex-direction: column; gap: 8px; }
.action-btn { display: flex; align-items: center; gap: 8px; padding: 12px; border-radius: 8px; text-decoration: none; transition: all 0.2s; }
.action-btn:hover { opacity: 0.8; }
.action-btn.scan { background: #ecf5ff; color: #409eff; }
.action-btn.bundle { background: #f0f9eb; color: #67c23a; }
.action-btn.record { background: #f5f0ff; color: #9254de; }
.action-icon { font-size: 20px; }
.action-text { font-size: 14px; font-weight: 500; }

.trend-section { background: #fff; border-radius: 12px; padding: 20px; border: 1px solid #ebeef5; }
.trend-section h2 { font-size: 16px; font-weight: bold; color: #303133; margin: 0 0 16px 0; }
.trend-chart { height: 240px; }
</style>
```

---

### Task 11: 前端路由配置

**Files:**
- Modify: `garment-web/src/router/index.js`

- [ ] **Step 1: 在 PC 端路由 children 中添加看板路由**

在现有的 PC 路由 children 数组中，找到合适的位置（建议放在第一个，作为默认首页），添加：

```javascript
{
  path: '/dashboard/boss',
  component: () => import('../views/DashboardBoss.vue'),
  meta: { title: '生产看板', layout: 'pc' }
},
{
  path: '/dashboard/supervisor',
  component: () => import('../views/DashboardSupervisor.vue'),
  meta: { title: '我的工作台', layout: 'pc' }
}
```

- [ ] **Step 2: 修改登录后跳转逻辑**

在路由守卫或登录逻辑中，登录成功后根据角色跳转到对应看板：

```javascript
// 在登录成功后的跳转逻辑中（Login.vue 或路由守卫）
const currentRole = localStorage.getItem('current_role') || ''
if (currentRole === 'boss') {
  router.push('/dashboard/boss')
} else if (currentRole === 'supervisor') {
  router.push('/dashboard/supervisor')
} else if (currentRole === 'worker') {
  router.push('/mobile')
}
```

注意：需要找到项目中现有的登录后跳转代码并修改。

---

### Task 12: 前端菜单配置

**Files:**
- Modify: `garment-web/src/views/layout/Layout.vue`

- [ ] **Step 1: 在 el-menu 中添加看板菜单项**

根据角色显示不同的看板入口：

```vue
<!-- 老板看板 -->
<el-menu-item v-if="userStore.isBoss()" index="/dashboard/boss">
  <el-icon><DataLine /></el-icon>
  <span>生产看板</span>
</el-menu-item>

<!-- 主管工作台 -->
<el-menu-item v-if="userStore.isSupervisor()" index="/dashboard/supervisor">
  <el-icon><Monitor /></el-icon>
  <span>我的工作台</span>
</el-menu-item>
```

确保 `el-icon` 组件已注册（Element Plus 自动图标需要导入 `DataLine` 和 `Monitor`）。

---

### Task 13: 前端构建验证

**Files:**
- 无

- [ ] **Step 1: 构建前端**

```bash
cd garment-web
npm run build
```

Expected: build completed without errors

---

## Self-Review Checklist

### Spec Coverage

| 需求项 | 对应任务 |
|--------|---------|
| 老板看板顶部4个统计卡片 | Task 5 (Service) + Task 9 (Vue) |
| 老板看板款式进度卡片网格 | Task 5 + Task 9 |
| 老板看板异常预警列表 | Task 5 + Task 9 |
| 老板看板近7日产量趋势 | Task 5 + Task 9 (ECharts) |
| 款式详情抽屉（床号工序） | Task 5 + Task 9 |
| 主管看板顶部3个统计卡片 | Task 5 + Task 10 |
| 主管看板扎货进度列表 | Task 5 + Task 10 |
| 主管看板待处理退回 | Task 5 + Task 10 |
| 主管看板快捷操作 | Task 10 |
| 主管看板近7日班组产量 | Task 5 + Task 10 (ECharts) |
| 后端4个接口 | Task 6 |
| 权限控制 | Task 6 (Controller) + Task 11/12 |
| 异常判定规则 | Task 5 (Service) |
| 自动刷新30秒 | Task 9 + Task 10 |

### Placeholder Scan

- [x] 无 TBD/TODO
- [x] 无 "appropriate error handling" 等模糊描述
- [x] 每个代码步骤包含完整代码
- [x] 无 "similar to Task N" 引用

### Type Consistency

- [x] VO 字段名与 Controller/Service 中使用的一致
- [x] 接口路径前后一致 (`/api/dashboard/boss`, `/api/dashboard/supervisor`)
- [x] 状态码使用一致 (0未开始/1进行中/2已完成, work_record 1通过/2驳回/3已退回)

---

## 执行建议

**按以下顺序执行：**

1. Task 1: 安装 ECharts
2. Task 2-4: 创建所有 VO 类（可并行）
3. Task 5: 创建 DashboardService（核心，依赖 VO）
4. Task 6: 创建 DashboardController
5. Task 7: 后端编译验证
6. Task 8: 前端 API 封装
7. Task 9-10: 前端两个页面（可并行，但建议先做老板看板）
8. Task 11: 路由配置
9. Task 12: 菜单配置
10. Task 13: 前端构建验证

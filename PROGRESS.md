# 缝迹 - 开发进度追踪

> 最后更新：2026-07-08（补齐扎货颜色字段生产迁移）
> 当前阶段：第二阶段+第三阶段+手机端员工端 🟡 进行中（员工管理 ✅ → 款式管理 ✅ → 分扎管理重构 ✅ → 二维码+报工 ✅ → 报工审核优化 ✅ → 分扎主管前缀 ✅ → 床号功能 ✅ → 手机端员工端 ✅ → **工资结算 ✅** → **主管工作台 ✅** → **老板工作台 ✅** → **工序模板 ✅** → **工资调整 ✅** → **主管通知 ✅** → **群发公告 ✅** → **超管工作台 ✅** → **全局数据看板增强 ✅** → **工资结算重构 ✅** → **安全修复 ✅** → **首次部署 ✅** → **扫码流程完善 ✅** → **员工工作详情分析 ✅** → **随机密码生成 ✅** → **工序定价锁定 ✅** → **老板端员工工作查询页 ✅** → **颜色从床号移到扎号 ✅** → 下一个：公司数据穿透 / 物料管理）

---

## 工作日志

### 2026-07-08（补齐扎货颜色字段生产迁移）

**问题：** `bundle.color` 已加入实体类和 `schema.sql`，但未同步加入 `migration.sql`，导致已有生产数据库不会自动新增该字段，后端查询扎货时报 `Unknown column 'color'`。

**处理：**
- 在 `migration.sql` 中增加可重复执行的 `bundle.color` 字段迁移。
- 迁移前备份生产数据库，备份目录：`/opt/garment/backup/20260708-000340`。
- 生产数据库 `garment_db.bundle` 已新增 `color VARCHAR(50) NULL COMMENT '颜色'`，位置在 `size` 字段之后。

**验证：** 原失败查询 `SELECT color FROM bundle LIMIT 1` 已成功执行；后端服务状态正常，近期日志未再出现 `Unknown column 'color'`。

---

### 2026-07-06（颜色从床号移到扎号）

**背景：** 用户要求将颜色设置从床号移到扎号中，床号不再需要设置颜色，颜色在扎号中设置。

**完成内容：**
1. 后端修改：
   - Bundle.java 添加 color 字段
   - schema.sql 更新 bundle 表，添加 color 字段
   - SaveBundleItemDTO.java 添加 color 字段
   - UpdateBundleDTO.java 添加 color 字段
   - BundleVO.java 添加 color 字段
   - BundleService.java 更新 batchSave、updateBundle、toBundleVO 方法，支持 color 字段

2. 前端修改：
   - Bundle.vue 移除床号表单中的颜色输入框，在扎号表格中添加颜色列
   - BundlePrintDrawer.vue 显示扎号的颜色
   - BundleQrOverviewDialog.vue 显示扎号的颜色
   - bundleLabelPrint.js 显示扎号的颜色
   - QrPrint.vue 显示扎号的颜色
   - Style.vue 移除床号编辑中的颜色输入框

**数据库更新：**
```sql
ALTER TABLE bundle ADD COLUMN color VARCHAR(50) DEFAULT NULL COMMENT '颜色' AFTER size;
```

**验证：** 后端 `mvn compile` 通过；前端 `npm run build` 通过。

---

### 2026-06-30（移除「每扎耗时」整扎生产跨度图）

**背景：** 「每扎耗时」图用的是整扎首尾工序完成时间差（含跨工序等待），口径不准、参考价值低，被同页更贴近真实作业耗时的「作业速度」图取代。橙子决定删除该图。

**完成内容：** 删除员工工作页的「每扎耗时」柱状图（整扎生产跨度口径），保留「作业速度」图。

**改动文件：**
- `garment-server/.../dto/BundleDurationDto.java` — 删除
- `garment-server/.../mapper/EmployeeWorkDetailMapper.java` — 删除 getBundleDurations
- `garment-server/.../service/EmployeeWorkDetailService.java` — 删除 getBundleDurations
- `garment-server/.../controller/WorkRecordController.java` — 删除 /bundle-durations 端点
- `garment-web/src/api/analysis.js` — 删除 getBundleDurations
- `garment-web/src/views/EmployeeWork.vue` — 移除每扎耗时图表及相关加载/渲染逻辑（`formatDuration` 因被作业速度图复用而保留）

**验证：** 后端 `mvn compile` 通过；前端 `npm run build` 通过。

---

### 2026-06-30（员工真实作业速度图：相邻报工时间差法）

**完成内容：**
新增「作业速度」柱状图到员工工作页，看员工每扎的真实作业耗时与速度。

**算法（橙子提出，零操作负担）：**
工人被安排做"一个床号下的同一道工序"，一批扎一起做，每完成一扎就扫码报工。系统按 **(员工, 床号, 工序名)** 分组，组内按报工时间升序，**相邻两次报工时间差 = 后一扎的作业耗时**。第一扎无前驱、起点不准，不计（"第一闸不算"）。速度 = 件数 ÷ 耗时（件/小时）。
- 不改工人端操作（仍是单次扫码报工），纯用现有 work_record 数据。
- 用 MySQL 8 窗口函数 `LAG() OVER (PARTITION BY worker_id, bed_id, process_name ORDER BY create_time)` 取相邻报工差；首扎 prev 为 NULL 被 `WHERE durationMinutes IS NOT NULL` 过滤。

**改动文件：**
- `garment-server/.../dto/WorkerSpeedDto.java` — 新增
- `garment-server/.../mapper/EmployeeWorkDetailMapper.java` — 新增 getWorkerSpeed（LAG 窗口函数）
- `garment-server/.../service/EmployeeWorkDetailService.java` — 新增 getWorkerSpeed
- `garment-server/.../controller/WorkRecordController.java` — 新增 /worker-speed 端点
- `garment-web/src/api/analysis.js` — 新增 getWorkerSpeed
- `garment-web/src/views/EmployeeWork.vue` — 新增「作业速度」柱状图（绿色，tooltip 含速度）

**与已有图区别：** 现有「每扎耗时」图是整扎首尾工序跨度（含等待），新「作业速度」图是同工序相邻两扎报工差（接近真实作业耗时），数值通常远小于前者。

**验证：** 后端 `mvn compile` 通过；前端 `npm run build` 通过。

---

### 2026-06-30（员工工作页：菜单移入工作概览 + 每扎耗时柱状图）

**完成内容：**
1. 菜单调整：「员工工作」从「审核通知」分组移到「工作概览」分组（与工作台/工作看板/历史工作并列）。
2. 新增「每扎耗时」柱状图到员工工作页面：选中员工后，展示该员工参与的每扎耗时（ECharts 柱状图，横轴扎号、纵轴分钟）。

**耗时口径（重要数据约束）：**
系统只记录每道工序的报工完成时刻（`bundle_process.complete_time`），没有"开工时间"。采用妥协口径——**整扎生产跨度**：`max(complete_time) − min(complete_time)`（首道→末道工序完成时间差）。仅统计 ≥2 道已完成工序的扎，按员工筛选（`operator_id` 命中），取最近 50 扎。图中含跨工序等待，非员工纯作业时长。

**改动文件：**
- `garment-server/.../dto/BundleDurationDto.java` — 新增
- `garment-server/.../mapper/EmployeeWorkDetailMapper.java` — 新增 getBundleDurations（TIMESTAMPDIFF 聚合）
- `garment-server/.../service/EmployeeWorkDetailService.java` — 新增 getBundleDurations
- `garment-server/.../controller/WorkRecordController.java` — 新增 /bundle-durations 端点
- `garment-web/src/api/analysis.js` — 新增 getBundleDurations
- `garment-web/src/views/EmployeeWork.vue` — 加图表区（ECharts 柱状图，联动刷新，占位提示）
- `garment-web/src/views/layout/Layout.vue` — 员工工作菜单移入工作概览分组

**验证：** 后端 `mvn compile` 通过；前端 `npm run build` 通过。

---

### 2026-06-30（老板端「员工工作」查询页）

**完成内容：**
1. 新增老板/主管 PC 端页面：搜索某员工（或全员）在指定时间段做了什么 —— 顶部汇总卡片 + 报工明细表。
2. 数据口径：只看已通过（status=1）报工；员工下拉可清空=查全部该时段所有人。
3. 汇总卡片：总件数、总金额、工作天数（有报工的不同日期数）、记录条数。
4. 明细表：员工、款号、床号、扎号、工序、数量、单价、金额、时间，分页。

**实现要点（最大化复用）：**
- 明细表格**零后端改动**：复用 `GET /api/work-record`（已支持 workerId + status + 时间范围 + 分页，返回 WorkRecordVO 字段全）。
- 新增唯一后端接口 `GET /api/work-record/summary`：按员工聚合 SUM(件数)/SUM(金额)/COUNT(DISTINCT 日期)/COUNT(记录)，用 MyBatis `<script>` + `<if>` 参数化（规避旧 employee-work-details 的字符串拼接）。
- 路径放在 `/api/work-record/summary` 而非 `/api/analysis`，使主管角色也能访问（`/api/analysis/**` 对 supervisor 禁止）。
- 汇总接口返回按员工 GROUP BY 的 List，前端 reduce 成合计；查单员工时只有一行。

**改动文件：**
- `garment-server/.../dto/EmployeeWorkSummaryDto.java` — 新增
- `garment-server/.../mapper/EmployeeWorkDetailMapper.java` — 新增聚合查询方法
- `garment-server/.../service/EmployeeWorkDetailService.java` — 新增 getEmployeeWorkSummary
- `garment-server/.../controller/WorkRecordController.java` — 新增 /summary 端点，注入 EmployeeWorkDetailService
- `garment-web/src/api/analysis.js` — 新增 getEmployeeWorkSummary
- `garment-web/src/views/EmployeeWork.vue` — 新增页面（参考 WorkReview.vue 骨架，去掉退回/扫码，加汇总卡片）
- `garment-web/src/router/index.js` — 新增 /employee-work 路由（boss/supervisor/superadmin）
- `garment-web/src/views/layout/Layout.vue` — 报工审核后新增「员工工作」菜单

**验证：** 后端 `mvn compile` 通过；前端 `npm run build` 通过。

---

### 2026-06-17（工序定价锁定与效率分析）

**完成内容：**
1. 未定价工序提示功能：
   - 款式列表卡片显示"有工序未定价"警告
   - 床号卡片显示"有 X 道工序未定价"提示
   - 点击提示可直接打开编辑对话框
2. 定价锁定规则：
   - 价格为 0 的工序可以修改一次（名称和价格）
   - 价格 > 0 的工序完全锁定，禁止所有修改（改名、改价、移动、删除）
   - 锁图标 🔒 显示在序号前面
3. 工资预览功能（未锁定时显示）：
   - 已完成数量 × 单价 = 已完成工资
   - 预估总数量 × 单价 = 预估工资
4. 效率分析功能（未锁定时显示）：
   - 选择报工数量最多的一天计算
   - 显示：平均速度（件/小时）
   - 显示：当天完成数量 / 工作时长
   - 如果只有一条记录，显示"数据不足"
5. 数据库变更：
   - process 表新增 price_locked 字段
   - 创建 migration.sql 迁移脚本

**改动文件：**
- `garment-server/src/main/resources/db/schema.sql` — 添加 price_locked 字段
- `garment-server/src/main/resources/db/migration.sql` — 新增迁移脚本
- `garment-server/.../entity/Process.java` — 添加 priceLocked 字段
- `garment-server/.../vo/BedVO.java` — ProcessItem 添加效率相关字段
- `garment-server/.../dto/SaveBedDTO.java` — ProcessItem 添加 id 字段
- `garment-server/.../service/BedService.java` — 定价锁定逻辑、效率计算
- `garment-web/src/views/Style.vue` — UI 显示、交互逻辑

**业务规则：**
- 价格为 0 → 可以修改一次（名称和价格），修改后自动锁定
- 价格 > 0 → 完全锁定，禁止所有修改
- 效率信息和价格预览只在未锁定状态显示

---

### 2026-06-14（随机密码生成安全修复）

**完成内容：**
1. 新增 `PasswordGenerator` 工具类：
   - 生成8位随机密码（包含大小写字母和数字）
   - 使用 `SecureRandom` 确保安全性
2. 后端服务层改造：
   - `CompanyService.createCompany()` — 返回随机密码（替代默认123456）
   - `CompanyService.resetBossPassword()` — 返回随机密码
   - `EmployeeService.addEmployee()` — 返回随机密码（跨厂添加返回null）
   - `EmployeeService.resetEmployeePassword()` — 返回随机密码
3. 后端控制器层改造：
   - `CompanyController.create()` — 返回 `Result<String>` 包含临时密码
   - `CompanyController.resetBossPassword()` — 返回 `Result<String>` 包含临时密码
   - `EmployeeController.add()` — 返回 `Result<String>` 包含临时密码
   - `EmployeeController.resetPassword()` — 返回 `Result<String>` 包含临时密码
4. 配置文件清理：
   - `application.yml` + `application-prod.yml` — 去掉 DB 密码和 JWT secret 的默认值
   - 新建 `.env.example` + `.gitignore` — 环境变量示例和版本控制排除
5. 前端改造：
   - `CompanyManage.vue` — 展示随机密码弹窗 + 修改提示文案
   - `Employee.vue` — 展示随机密码弹窗 + 修改提示文案
6. 编译验证：`mvn compile` + `npm run build` 均通过

**安全改进：**
- 创建公司/员工时自动生成8位随机密码（替代默认123456）
- 重置密码时自动生成随机密码（替代默认123456）
- 数据库密码和JWT密钥改为环境变量配置（不再硬编码）
- 敏感信息通过 `.gitignore` 排除版本控制

**改动文件：**
- `garment-server/.../util/PasswordGenerator.java` — 新增
- `garment-server/.../service/CompanyService.java` — 返回随机密码
- `garment-server/.../service/EmployeeService.java` — 返回随机密码
- `garment-server/.../controller/CompanyController.java` — 返回临时密码
- `garment-server/.../controller/EmployeeController.java` — 返回临时密码
- `garment-server/src/main/resources/application.yml` — 环境变量配置
- `garment-server/src/main/resources/application-prod.yml` — 环境变量配置
- `.env.example` — 新增环境变量示例
- `.gitignore` — 新增排除规则
- `garment-web/src/views/CompanyManage.vue` — 密码弹窗
- `garment-web/src/views/Employee.vue` — 密码弹窗

---

### 2026-06-14（员工工作详情分析接口）

**完成内容：**
1. 后端新增员工工作详情分析接口 `GET /api/analysis/employee-work-details`：
   - 支持按时间范围筛选（startDate、endDate）
   - 支持按员工ID筛选（workerId，可选）
   - 返回：员工姓名、床号、扎号、工序名称、数量、金额、日期
2. 后端新增模块（dto → mapper → service → controller 完整四层）：
   - `EmployeeWorkDetailDto` — 数据传输对象
   - `EmployeeWorkDetailMapper` — 自定义 SQL 查询，关联 bundles、workers、beds、processes 表
   - `EmployeeWorkDetailService` — 业务逻辑，动态构建员工筛选条件
   - `EmployeeWorkDetailController` — REST API 接口，自动获取当前公司ID
3. 查询逻辑：
   - 从 `bundles` 表获取已完成的扎货记录（status=completed）
   - 关联 `workers` 表获取员工姓名
   - 关联 `beds` 表获取床号
   - 关联 `processes` 表获取工序名称
   - 按创建时间倒序排列
4. 权限控制：
   - 超管/老板：可查看所有员工数据
   - 工人：只能查看自己的数据（自动设置 workerId 为当前用户）
5. 前端集成：
   - 在「数据分析」页面（AnalysisApi.vue）新增接口说明卡片
   - 支持一键鉴权、复制 URL、复制 cURL 命令

**改动文件：**
- `garment-server/.../common/TenantContext.java` — 新增 userType 字段
- `garment-server/.../config/SecurityConfig.java` — 设置 userType 到 TenantContext
- `garment-server/.../dto/EmployeeWorkDetailDto.java` — 新增
- `garment-server/.../mapper/EmployeeWorkDetailMapper.java` — 新增
- `garment-server/.../service/EmployeeWorkDetailService.java` — 新增
- `garment-server/.../controller/EmployeeWorkDetailController.java` — 新增
- `garment-web/src/views/AnalysisApi.vue` — 新增接口说明卡片

---

### 2026-06-13（扫码登录后自动返回优化）

**完成内容：**
1. 未登录扫码拦截优化：
   - 访问 `/mobile/scan?code=xxx` 等需登录页面时，路由守卫自动跳 `/login?redirect=...`
   - 登录成功后按角色返回：超级管理员直接回原页面，普通用户先选公司再回原页面
   - 选择公司页支持读取 `redirect` 参数，单公司自动进入时也能正确返回
2. 安全处理：仅接受以 `/` 开头的站内相对路径作为 redirect，避免开放重定向漏洞

**改动文件：**
- `garment-web/src/router/index.js` — 未登录时携带 redirect 参数
- `garment-web/src/views/Login.vue` — 登录后按 redirect 跳转
- `garment-web/src/views/CompanySelect.vue` — 选公司后按 redirect 跳转

---

### 2026-06-04（首次部署到生产服务器）

**服务器信息：**
- IP：`101.34.209.192`
- 系统：Ubuntu 22.04
- 配置：4核 4G内存 40G硬盘
- 账号：ubuntu

**部署架构：**
- 后端：Spring Boot jar + systemd 服务（端口 8080）
- 前端：Nginx 托管静态文件（端口 80）+ 反向代理 `/api/` 到后端
- 数据库：MySQL 8（密码由环境变量提供）
- JDK：OpenJDK 21

**部署过程记录：**
1. 创建部署脚本：`deploy/setup-server.sh`（环境安装）、`deploy/deploy.sh`（项目部署）、`deploy/build.bat`（本地打包）
2. 本地打包：`mvn clean package` + `npm run build` → 生成 `garment-deploy.zip`（约 30MB）
3. 上传到服务器：`scp` 到 `/tmp/`
4. 服务器安装环境：JDK 17 → MySQL 8 → Nginx（后升级到 JDK 21）
5. 数据库初始化：执行 schema.sql 建表
6. 配置 systemd 服务：`garment.service`
7. 配置 Nginx 反向代理

**遇到的问题及解决：**
1. **schema.sql 开头有 `cdcd` 乱码** — 删除后解决
2. **MySQL 不支持 `ADD COLUMN IF NOT EXISTS` 语法** — 该语法仅 MariaDB 支持，MySQL 需要在 CREATE TABLE 中定义所有字段，或手动 ALTER TABLE
3. **Java 版本不匹配** — 项目用 Java 21 编译，服务器最初装的 JDK 17，升级到 JDK 21 后解决
4. **User 实体字段与数据库不同步** — schema.sql 缺少 `gender`、`bank_card`、`need_change_password`、`last_view_*` 等字段，手动 ALTER TABLE 添加后解决

**部署文件：**
- `deploy/setup-server.sh` — 服务器环境安装脚本
- `deploy/deploy.sh` — 项目部署脚本
- `deploy/build.bat` — Windows 本地打包脚本
- `deploy/README.md` — 部署文档
- `garment-server/src/main/resources/application-prod.yml` — 生产环境配置

**经验教训：**
- schema.sql 必须和代码保持同步，不能依赖 ALTER TABLE 补字段（MySQL 不支持 IF NOT EXISTS）
- 本地开发用的 Java 版本要和服务器一致
- 打包前检查所有实体类的字段是否在建表脚本中

---

### 2026-06-04（安全修复 + 强制改密）

**完成内容：**
1. 安全风险扫描与修复：
   - **X-Company-Id 越权漏洞修复** — SecurityConfig 中验证非超管用户是否属于目标公司（查 user_company 表）
   - **删除 /api/test/** 后门配置
   - **CORS 配置收紧** — 从 `*` 改为仅允许 localhost 和局域网 IP
   - **敏感信息改环境变量** — 数据库密码、JWT 密钥改为 `${ENV_VAR:default}` 格式
   - **User 实体加 @JsonIgnore** — 防止密码哈希通过 JSON 响应泄露
   - **公司详情接口加权限校验** — 非超管只能查看自己所在的公司
   - **前端路由权限校验** — 为 7 个管理路由添加 allowedRoles 白名单
   - **补充 @Transactional 注解** — 为 5 个多表写操作方法添加事务保护
2. 强制首次登录修改密码：
   - User 实体新增 `needChangePassword` 字段
   - 创建用户/重置密码时自动设置为 1
   - 登录时返回该字段，前端弹出强制改密对话框（不可关闭/跳过）
   - 修改密码成功后清除标记

**需要执行的SQL：**
```sql
-- user 表新增强制改密字段
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `need_change_password` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要修改密码：0不需要 1需要' AFTER `status`;

-- 可选：让现有用户下次登录强制改密
UPDATE `user` SET `need_change_password` = 1;
```

**改动文件：**
- `garment-server/.../config/SecurityConfig.java` — 修复越权 + 删除后门
- `garment-server/.../config/CorsConfig.java` — 收紧 CORS
- `garment-server/src/main/resources/application.yml` — 环境变量
- `garment-server/.../entity/User.java` — @JsonIgnore + needChangePassword
- `garment-server/.../controller/CompanyController.java` — 权限校验
- `garment-server/.../service/AuthService.java` — 返回 needChangePassword
- `garment-server/.../service/CompanyService.java` — 创建/重置时设置标记
- `garment-server/.../service/EmployeeService.java` — 创建/重置时设置标记
- `garment-server/.../service/NotificationService.java` — @Transactional
- `garment-server/.../config/DataInitializer.java` — admin 设置标记
- `garment-server/src/main/resources/db/schema.sql` — 新增字段
- `garment-web/src/router/index.js` — 路由权限白名单
- `garment-web/src/stores/user.js` — needChangePassword 状态管理
- `garment-web/src/views/Login.vue` — 强制改密对话框

---

### 2026-06-04（工资结算功能重构）

**完成内容：**
1. 数据库变更：
   - `wage_settlement` 表：删除 `year_month`、`signed_count`，新增 `start_date`、`end_date`
   - `wage_settlement_detail` 表：删除 `sign_time`，新增 `visible_to_employee`、`employee_confirmed`、`employee_confirm_time`
   - `work_record` 表新增 `settlement_id` 字段（标记已结算）
   - `wage_adjustment` 表新增 `settlement_id` 字段（标记已结算）
2. 后端重构：
   - `WageSettlementService` — 核心重构：
     - 新增 `getLastSettlement()` — 获取上次结算信息（自动填充下次起始日期）
     - 新增 `previewSettlement()` — 预览全员明细（不保存）
     - 新增 `confirmEmployees()` — 批量确认员工（生成结算记录）
     - 新增 `setVisibility()` — 设置员工可见性
     - 新增 `getMySettlements()` — 员工端：我的工资结算
     - 新增 `confirmViewing()` — 员工端：确认查看工资
     - 删除签名相关方法（initiateSigning、signSettlement、cancelSettlement）
   - `WageSettlementController` — 新增接口：
     - `GET /api/wage-settlement/last` — 获取上次结算信息
     - `POST /api/wage-settlement/preview` — 预览全员明细
     - `POST /api/wage-settlement/confirm` — 批量确认员工
     - `POST /api/wage-settlement/{id}/worker/{workerId}/visibility` — 设置员工可见性
     - `POST /api/wage-settlement/{id}/confirm-view` — 员工确认查看
     - 删除签名相关接口
   - `DashboardService` — 修改工资统计逻辑（适配新的 startDate/endDate 字段）
3. 前端重构：
   - `api/wageSettlement.js` — 新增 API：getLastSettlement、previewSettlement、confirmEmployees、setEmployeeVisibility、getMySettlements、confirmViewing
   - `WageSettlement.vue`（老板端）— 全面重构：
     - 新增「预览」功能：选择时间段 → 预览全员明细（含报工明细可展开）
     - 新增「批量确认」功能：全选/单选员工 → 批量确认生成结算记录
     - 新增「员工可见性」开关：控制员工是否可见历史工资
     - 结算详情页：显示员工列表、工资明细、确认状态
     - 删除签名相关功能
   - `MobileWageSign.vue`（员工端）— 重构：
     - 显示分项明细：计件工资、调整工资、应发工资
     - 新增「确认查看」按钮（只是"我已查看"，不能拒绝）
     - 删除签名功能
4. 编译验证：后端 `mvn compile` 通过

**业务流程（重构后）：**
```
老板选择时间段（默认从上次结算后一天开始）→ 预览全员明细
→ 逐个/批量确认员工 → 生成结算记录 → 标记已结算的报工和调整
→ 员工收到提醒 → 查看分项明细 → 确认查看
→ 发现错误 → 用工资调整功能补差
```

**关键设计：**
- 确认一个生成一个，不提前生成
- 不支持撤销，发现错误用工资调整补差
- 0元工资的员工不参与结算
- 老板可控制员工是否可见历史金额
- 员工确认只是"我已查看"，不能拒绝

**需要执行的SQL：**
```sql
-- 重建 wage_settlement 表（或修改现有表）
DROP TABLE IF EXISTS `wage_settlement`;
CREATE TABLE `wage_settlement` (...);  -- 见 schema.sql

-- 重建 wage_settlement_detail 表（或修改现有表）
DROP TABLE IF EXISTS `wage_settlement_detail`;
CREATE TABLE `wage_settlement_detail` (...);  -- 见 schema.sql

-- work_record 表新增 settlement_id
ALTER TABLE `work_record` ADD COLUMN IF NOT EXISTS `settlement_id` BIGINT DEFAULT NULL COMMENT '结算单ID(null=未结算)' AFTER `status`;

-- wage_adjustment 表新增 settlement_id
ALTER TABLE `wage_adjustment` ADD COLUMN IF NOT EXISTS `settlement_id` BIGINT DEFAULT NULL COMMENT '结算单ID(null=未结算)' AFTER `approver_id`;
```

**改动文件：**
- `garment-server/src/main/resources/db/schema.sql` — 新增/修改表结构
- `garment-server/.../entity/WageSettlement.java` — 重构字段
- `garment-server/.../entity/WageSettlementDetail.java` — 重构字段
- `garment-server/.../entity/WorkRecord.java` — 新增 settlementId
- `garment-server/.../entity/WageAdjustment.java` — 新增 settlementId
- `garment-server/.../dto/CreateSettlementDTO.java` — 重构字段
- `garment-server/.../vo/WageSettlementVO.java` — 重构字段
- `garment-server/.../vo/WageSettlementDetailVO.java` — 重构字段
- `garment-server/.../service/WageSettlementService.java` — 核心重构
- `garment-server/.../service/DashboardService.java` — 修改工资统计逻辑
- `garment-server/.../controller/WageSettlementController.java` — 重构接口
- `garment-web/src/api/wageSettlement.js` — 重构 API
- `garment-web/src/views/WageSettlement.vue` — 老板端全面重构
- `garment-web/src/views/mobile/MobileWageSign.vue` — 员工端重构

---

### 2026-06-03（全局数据看板增强）

**完成内容：**
1. 安装 ECharts 图表库（`npm install echarts`）
2. 后端增强 `AdminDashboardVO`：
   - 新增 `totalBundleCount`（总扎货数）
   - 新增 `totalWorkRecordCount`（本月报工条数）
   - 新增 `monthTrends`（近6个月产值趋势数据）
   - 新增 `companyEmployeeStats`（各公司员工分布 Top10）
   - 新增内部类 `MonthlyTrend` 和 `CompanyEmployeeStat`
3. 后端增强 `DashboardService.getAdminStats()`：
   - 查询总扎货数
   - 查询近6个月每月产值（循环查询6个月的 work_record）
   - 查询各公司员工分布统计
4. 前端增强 `Dashboard.vue` 超级管理员视图：
   - 统计卡片新增「总扎货数」（替换原来的「总款式数」）
   - 新增「近6个月产值趋势」折线图（ECharts，带面积图、渐变色）
   - 新增「公司员工分布」柱状图+折线图组合（员工数柱状图+本月产值折线图）
   - 图表窗口自适应（resize 事件监听）
5. 编译验证：后端 `mvn compile` + 前端 `npm run build` 均通过

**超管工作台最新布局：**
```
欢迎区 → 全局统计卡片（4个：公司/员工/产值/扎货）→ 快捷操作 → 活跃公司排名（Top5）
→ 即将到期提醒 → 近6个月产值趋势图 → 公司员工分布图
```

**改动文件：**
- `garment-server/.../vo/AdminDashboardVO.java` — 新增字段和内部类
- `garment-server/.../service/DashboardService.java` — 增强 getAdminStats()
- `garment-web/package.json` — 新增 echarts 依赖
- `garment-web/src/views/Dashboard.vue` — 新增图表+统计卡片调整

---

### 2026-06-03（超管工作台改造）

**完成内容：**
1. 后端新增：
   - `AdminDashboardVO` — 超管全局统计数据VO，含公司数、员工数、款式数、产值、到期提醒、活跃公司排名等
   - `DashboardService` 新增 `getAdminStats()` 方法：全局聚合统计（跨 company_id 查询）
   - `DashboardController` 新增 `GET /api/dashboard/admin-stats` 接口（仅超管可访问）
2. 前端新增：
   - `dashboard.js` 新增 `getAdminStats()` API
3. 前端修改 `Dashboard.vue`：
   - 超管统计卡片改为调用真实接口（入驻公司/总员工数/总款式数/本月产值）
   - 快捷操作调整为「公司管理」「续费记录」
   - 新增「活跃公司排名」区域：按本月产值排序 Top5，显示员工数、款式数
   - 新增「即将到期提醒」区域：30天内到期公司列表，7天内标红高亮
4. 编译验证：后端 `mvn compile` + 前端 `npm run build` 均通过

**超管工作台布局：**
```
欢迎区 → 全局统计卡片（4个）→ 快捷操作 → 活跃公司排名（Top5）→ 即将到期提醒
```

**改动文件：**
- `garment-server/.../vo/AdminDashboardVO.java` — 新增
- `garment-server/.../service/DashboardService.java` — 新增 getAdminStats()
- `garment-server/.../controller/DashboardController.java` — 新增 /admin-stats
- `garment-web/src/api/dashboard.js` — 新增 getAdminStats()
- `garment-web/src/views/Dashboard.vue` — 超管分支改造 + 新增区域 + 样式

---

### 2026-06-03（主管通知功能）

**完成内容：**
1. 后端新增：
   - `User` 实体新增3个字段：`lastViewReturnAt`、`lastViewReviewAt`、`lastViewSupervisorAdjAt`（主管通知查看时间戳）
   - `DashboardController` 新增2个接口：`GET /supervisor-notifications`（通知计数）、`POST /supervisor-notifications/viewed`（标记已读）
   - `DashboardService` 新增 `getSupervisorNotifications()` 和 `markSupervisorNotificationsViewed()` 方法
2. 前端新增：
   - `SupervisorNotifications.vue` — PC端主管通知页面，三种通知类型（退回/新报工/工资调整），筛选Tab，卡片列表
   - `dashboard.js` 新增 `getSupervisorNotifications()` 和 `markSupervisorNotificationsViewed()` API
3. 前端修改：
   - `router/index.js` — 新增 `/notifications` 路由
   - `Layout.vue` — 侧边栏新增「通知」菜单（仅主管可见），带红点数字徽标，离开通知页时刷新红点
   - `schema.sql` — 新增6个 ALTER TABLE 语句（user表扩展字段）

**通知类型：**
| 类型 | 说明 | 数据来源 | 点击跳转 |
|------|------|----------|----------|
| 退回 | 本公司被退回的报工记录 | work_record status=3 | 报工审核页 |
| 新报工 | 本公司新提交的报工记录 | work_record status=1 | 报工审核页 |
| 工资调整 | 主管创建的待审批调整记录 | wage_adjustment status=0 | 工资调整页 |

**改动文件：**
- `garment-server/.../entity/User.java` — 新增3个字段
- `garment-server/src/main/resources/db/schema.sql` — 新增ALTER TABLE
- `garment-server/.../service/DashboardService.java` — 新增2个方法
- `garment-server/.../controller/DashboardController.java` — 新增2个接口
- `garment-web/src/api/dashboard.js` — 新增2个API
- `garment-web/src/views/SupervisorNotifications.vue` — 新增
- `garment-web/src/router/index.js` — 新增路由
- `garment-web/src/views/layout/Layout.vue` — 新增通知菜单+红点逻辑

---

### 2026-06-03（群发公告功能）

**完成内容：**
1. 数据库新增 2 张表：
   - `notification`（通知公告表）— 公司、发送人、标题、内容、接收范围（全部/仅主管/仅工人）
   - `notification_read`（通知已读表）— 通知ID、用户ID、已读时间
2. 后端新增模块（entity → mapper → service → controller 完整四层）：
   - `NotificationService` — 6个方法：发送通知、我发送的通知列表、我的通知列表、标记已读、全部已读、未读计数、删除通知
   - `NotificationController` — 7个接口
   - DTO：SendNotificationDTO
   - VO：NotificationVO（含发送人姓名、已读状态）
3. 前端新增：
   - `api/notification.js` — API 封装（7个函数）
   - `NotificationManage.vue`（PC端）— 老板/主管发送通知页面，支持发送、查看已发通知、删除
   - `MobileNotifications.vue`（手机端）— 集成公告Tab，支持查看公告、标记已读、全部已读
4. 前端修改：
   - `router/index.js` — 新增 `/notification-manage` 路由
   - `Layout.vue` — 侧边栏新增「发通知」菜单（老板+主管可见）
   - `MobileLayout.vue` — 通知红点逻辑增加公告未读检测
5. 编译验证：后端 `mvn compile` + 前端 `npm run build` 均通过

**业务流程：**
```
老板/主管进入「发通知」页面 → 写标题+内容 → 选择接收范围（全部/仅主管/仅工人）
→ 发送 → 员工手机端通知页出现新公告（红点提醒）
→ 员工点击公告 → 标记已读
→ 员工可点「全部已读」一键标记
```

**需要执行的SQL：**
```sql
CREATE TABLE IF NOT EXISTS `notification` (...);
CREATE TABLE IF NOT EXISTS `notification_read` (...);
-- 完整建表语句见 schema.sql 末尾
```

**改动文件：**
- `garment-server/src/main/resources/db/schema.sql` — 新增2张表
- `garment-server/.../entity/Notification.java` — 新增
- `garment-server/.../entity/NotificationRead.java` — 新增
- `garment-server/.../mapper/NotificationMapper.java` — 新增
- `garment-server/.../mapper/NotificationReadMapper.java` — 新增
- `garment-server/.../dto/SendNotificationDTO.java` — 新增
- `garment-server/.../vo/NotificationVO.java` — 新增
- `garment-server/.../service/NotificationService.java` — 新增
- `garment-server/.../controller/NotificationController.java` — 新增
- `garment-web/src/api/notification.js` — 新增
- `garment-web/src/views/NotificationManage.vue` — 新增
- `garment-web/src/views/mobile/MobileNotifications.vue` — 集成公告Tab
- `garment-web/src/router/index.js` — 新增路由
- `garment-web/src/views/layout/Layout.vue` — 新增菜单+红点逻辑
- `garment-web/src/views/layout/MobileLayout.vue` — 红点逻辑增加公告检测

---

### 2026-06-03（工资调整记录功能）

**完成内容：**
1. 数据库新增 1 张表：
   - `wage_adjustment`（工资调整记录表）— 工人、操作人、类型(加钱/扣钱)、金额、备注、状态、审批人
2. 后端新增模块（entity → mapper → service → controller 完整四层）：
   - `WageAdjustmentService` — 5个方法：添加、列表、审批、删除、我的记录
   - `WageAdjustmentController` — 5个接口
   - DTO：SaveAdjustmentDTO
   - VO：WageAdjustmentVO（含工人姓名、操作人姓名、审批人姓名）
3. 前端新增：
   - `api/wageAdjustment.js` — API 封装（5个函数）
   - `WageAdjustment.vue`（PC端）— 老板/主管共用页面，统计卡片+筛选+表格+添加弹窗
   - `MobileWageRecords.vue`（手机端）— 工人查看自己的调整记录，统计+筛选+卡片列表
4. 前端修改：
   - `router/index.js` — 新增 PC 端和手机端路由
   - `Layout.vue` — 侧边栏新增「工资调整」菜单（老板+主管可见）
   - `MobileProfile.vue` — 个人中心新增「工资调整」入口
5. 工资结算集成：
   - `WageSettlementService.createSettlement()` — 创建结算单时汇总该月已审批的工资调整金额
   - `WageSettlementDetail` 实体和 VO 新增 `supplementAmount`（调整金额）和 `finalAmount`（最终工资）字段
6. 编译验证：后端 `mvn compile` + 前端 `npm run build` 均通过

**业务流程：**
```
老板/主管进入「工资调整」页面 → 添加记录（选工人、加钱/扣钱、金额、备注）
→ 状态：待审批 → 老板审批通过 → 状态：已审批 → 立即计入工资
→ 月底创建结算单时自动汇总已审批的调整金额
→ 员工手机端可查看自己的所有调整记录（待审批/已生效/已删除）
```

**权限设计：**
| 角色 | 可做什么 |
|------|---------|
| 老板 | 给所有人添加 + 审批 + 删除所有 |
| 主管 | 给工人添加 + 删除自己添加的待审批记录 |
| 工人 | 查看自己的记录 |

**需要执行的SQL：**
```sql
-- 新增工资调整记录表
CREATE TABLE IF NOT EXISTS `wage_adjustment` (...);
-- 完整建表语句见 schema.sql 末尾
```

**改动文件：**
- `garment-server/src/main/resources/db/schema.sql` — 新增1张表
- `garment-server/.../entity/WageAdjustment.java` — 新增
- `garment-server/.../mapper/WageAdjustmentMapper.java` — 新增
- `garment-server/.../dto/SaveAdjustmentDTO.java` — 新增
- `garment-server/.../vo/WageAdjustmentVO.java` — 新增
- `garment-server/.../service/WageAdjustmentService.java` — 新增
- `garment-server/.../controller/WageAdjustmentController.java` — 新增
- `garment-server/.../entity/WageSettlementDetail.java` — 新增 supplementAmount、finalAmount
- `garment-server/.../vo/WageSettlementDetailVO.java` — 新增 supplementAmount、finalAmount
- `garment-server/.../service/WageSettlementService.java` — 注入调整Mapper + 汇总调整金额
- `garment-web/src/api/wageAdjustment.js` — 新增
- `garment-web/src/views/WageAdjustment.vue` — 新增
- `garment-web/src/views/mobile/MobileWageRecords.vue` — 新增
- `garment-web/src/router/index.js` — 新增路由+工人跳转映射
- `garment-web/src/views/layout/Layout.vue` — 新增菜单+标题映射
- `garment-web/src/views/mobile/MobileProfile.vue` — 新增工资调整入口

---

### 2026-06-03（工序模板功能）

**完成内容：**
1. 数据库新增 2 张表：
   - `process_template`（工序模板主表）— 模板名称、备注、公司隔离
   - `process_template_item`（工序模板明细表）— 工序名称、序号、默认单价
2. 后端新增模块（entity → mapper → service → controller 完整四层）：
   - `ProcessTemplateService` — 5个方法：列表、详情、新增、编辑、删除
   - `ProcessTemplateController` — 5个接口，超级管理员拦截
   - DTO：SaveProcessTemplateDTO（含内部类 TemplateItem）
   - VO：ProcessTemplateVO（含内部类 TemplateItem）
3. 前端新增：
   - `api/processTemplate.js` — API 封装（5个函数）
   - `ProcessTemplate.vue` — 老板工序模板管理页面（卡片网格 + 搜索 + 新增/编辑弹窗 + 工序行编辑）
4. 前端修改：
   - `Style.vue` — 床号编辑弹窗新增「导入模板」按钮，选择模板后自动填充工序列表
   - `router/index.js` — 新增 `/process-template` 路由
   - `Layout.vue` — 侧边栏新增「工序模板」菜单（仅老板可见）
5. 编译验证：后端 `mvn compile` + 前端 `npm run build` 均通过

**业务流程：**
```
老板进入「工序模板」页面 → 新建模板 → 添加工序（名称、序号、单价）
→ 主管编辑床号时 → 点击「导入模板」→ 选择模板 → 工序自动填充
→ 主管可微调（修改单价/增删/排序）→ 保存床号 → 工序与模板脱钩
```

**需要执行的SQL：**
```sql
-- 新增工序模板表
CREATE TABLE IF NOT EXISTS `process_template` (...);
CREATE TABLE IF NOT EXISTS `process_template_item` (...);
-- 完整建表语句见 schema.sql 末尾
```

**改动文件：**
- `garment-server/src/main/resources/db/schema.sql` — 新增2张表
- `garment-server/.../entity/ProcessTemplate.java` — 新增
- `garment-server/.../entity/ProcessTemplateItem.java` — 新增
- `garment-server/.../mapper/ProcessTemplateMapper.java` — 新增
- `garment-server/.../mapper/ProcessTemplateItemMapper.java` — 新增
- `garment-server/.../dto/SaveProcessTemplateDTO.java` — 新增
- `garment-server/.../vo/ProcessTemplateVO.java` — 新增
- `garment-server/.../service/ProcessTemplateService.java` — 新增
- `garment-server/.../controller/ProcessTemplateController.java` — 新增
- `garment-web/src/api/processTemplate.js` — 新增
- `garment-web/src/views/ProcessTemplate.vue` — 新增
- `garment-web/src/views/Style.vue` — 新增导入模板按钮+选择弹窗
- `garment-web/src/router/index.js` — 新增路由
- `garment-web/src/views/layout/Layout.vue` — 新增菜单

---

### 2026-06-02（老板工作台功能）

**完成内容：**
1. 后端新增老板统计接口 `GET /api/dashboard/boss-stats`：
   - 今日报工产值和条数
   - 本月报工金额
   - 今日活跃员工数 / 在职员工总数
   - 进行中款式数
   - 近7天退回记录数
2. 后端新增 `BossDashboardVO` VO 类
3. `DashboardService` 新增 `getBossStats()` 方法
4. 前端 `Dashboard.vue` 老板工作台改造：
   - 统计卡片接入真实数据：今日产值 / 本月工资 / 在职员工 / 进行中款式
   - 快捷操作配置路由：员工管理 / 款式管理 / 工资结算 / 分扎管理
   - 新增「全公司款式进度」区域：显示所有款式的生产进度条
5. 前端 `dashboard.js` 新增 `getBossStats()` API
6. 后端编译 + 前端构建验证通过

**改动文件：**
- `garment-server/.../vo/BossDashboardVO.java` — 新增
- `garment-server/.../service/DashboardService.java` — 新增 getBossStats 方法
- `garment-server/.../controller/DashboardController.java` — 新增 /boss-stats 接口
- `garment-web/src/api/dashboard.js` — 新增 getBossStats API
- `garment-web/src/views/Dashboard.vue` — 老板统计卡片+快捷操作+款式进度

---

### 2026-06-02（主管工作台功能）

**完成内容：**
1. 后端新增主管统计接口 `GET /api/dashboard/supervisor-stats`：
   - 我的扎货总数和总件数
   - 今日报工条数和产值（本公司）
   - 进行中款式数
   - 近7天退回记录数
   - 我的款式进度概览（按款式分组，显示进度百分比）
   - 最近5条报工记录（含工人姓名、工序、扎号、数量、金额、时间）
2. 后端新增 `SupervisorDashboardVO` VO 类（含内部类 MyStyleProgress、RecentReport）
3. `DashboardService` 新增 `getSupervisorStats()` 方法，注入 WorkRecordMapper 和 UserMapper
4. 前端 `Dashboard.vue` 改造：
   - 主管角色显示专属统计卡片：我的扎货 / 今日报工 / 进行中款式 / 退回记录
   - 主管专属快捷操作：扫码报工 / 报工审核 / 工作看板 / 分扎管理
   - 新增「我的款式进度」区域：显示主管创建的扎货所在款式的进度条
   - 新增「最近报工」区域：显示本公司最近5条报工记录
   - 老板和管理员角色保持原有逻辑不变
5. 前端 `dashboard.js` 新增 `getSupervisorStats()` API
6. 后端编译验证通过

**改动文件：**
- `garment-server/.../vo/SupervisorDashboardVO.java` — 新增
- `garment-server/.../service/DashboardService.java` — 新增 getSupervisorStats 方法
- `garment-server/.../controller/DashboardController.java` — 新增 /supervisor-stats 接口
- `garment-web/src/api/dashboard.js` — 新增 getSupervisorStats API
- `garment-web/src/views/Dashboard.vue` — 主管专属统计+快捷操作+款式进度+最近报工

---

### 2026-06-02（报工审核扫码+筛选功能）

**完成内容：**
1. 在主管的报工审核页面（`WorkReview.vue`）顶部添加**筛选区域**：
   - 状态筛选：全部/已通过/已退回
   - 工人姓名筛选（模糊匹配）
   - 款式编号筛选（模糊匹配）
   - 扎号筛选（模糊匹配）
   - 日期范围筛选
   - 重置按钮
2. 添加**二维码查询功能**：
   - 输入框可手动输入二维码内容查询
   - 扫码按钮（手机端跳转扫码页，PC端提示）
3. 报工记录列表从**卡片形式**改为**表格形式**：
   - 使用 `el-table` 组件，更紧凑清晰
   - 列：工人、款式、扎号、工序、数量、金额、进度条、状态、时间、备注、操作
4. 分页功能增强：
   - 支持每页条数选择（10/20/50/100）
   - 支持跳转到指定页
5. 新增**扎货工序详情弹窗**：
   - 输入二维码内容查询后弹出
   - 显示所有工序完成情况，已完成工序可退回
6. 后端接口增强：
   - `WorkRecordController.list()` 新增筛选参数：workerName、styleNo、bundleNo
   - `WorkRecordService.listWorkRecords()` 支持模糊匹配筛选

**使用场景：**
- 主管查看特定工人的报工 → 输入工人姓名筛选
- 主管查看特定款式的报工 → 输入款式编号筛选
- 主管查看特定扎号的报工 → 输入扎号筛选
- 主管查看某段时间的报工 → 选择日期范围

**改动文件：**
- `garment-web/src/views/WorkReview.vue` — 筛选区域 + 表格重构 + 分页增强
- `garment-server/.../controller/WorkRecordController.java` — 新增筛选参数
- `garment-server/.../service/WorkRecordService.java` — 模糊匹配筛选逻辑

---

### 2026-06-02（工资结算功能）

**完成内容：**
1. 数据库新增 2 张表：
   - `wage_settlement`（工资结算单）— 月份、总额、工人数、签名进度、状态
   - `wage_settlement_detail`（工资结算明细）— 工人、件数、金额、签名状态、签名时间
2. 后端新增模块（entity → mapper → service → controller 完整四层）：
   - `WageSettlementService` — 6个核心方法：
     - 创建结算单（汇总该月已通过报工记录，按工人分组生成明细）
     - 结算单列表、结算单详情（含工人明细和签名状态）
     - 撤销结算（重置所有签名状态）
     - 工人待签名列表、工人签名确认（全部签名后自动锁定）
   - `WageSettlementController` — 6个接口
   - DTO：CreateSettlementDTO、SignSettlementDTO
   - VO：WageSettlementVO、WageSettlementDetailVO
3. 前端新增：
   - `api/wageSettlement.js` — API 封装
   - `WageSettlement.vue`（PC端）— 结算单列表，支持选择月份发起结算
   - `WageSettlementDetail.vue`（PC端）— 结算详情，工人签名明细表格，撤销结算
   - `MobileWageSign.vue`（手机端）— 工人查看待签名结算、确认金额、签名
4. 路由和菜单：
   - PC端新增 `/wage-settlement` 和 `/wage-settlement/:id` 路由
   - 手机端新增 `/mobile/wage-sign` 路由
   - 侧边栏新增「工资结算」菜单（仅老板可见）
   - 个人中心新增「工资签名」入口
   - 工人角色访问工资结算自动跳转到手机端签名页
5. 编译验证：后端 `mvn compile` + 前端 `npm run build` 均通过

**业务流程：**
```
老板选择月份 → 发起结算 → 系统汇总已通过报工 → 生成结算单
→ 工人手机端查看工资明细 → 确认签名
→ 全部签名完成 → 结算单锁定（不可修改）
→ 老板可撤销结算 → 重置签名状态
```

**需要执行的SQL：**
```sql
-- 新增工资结算表
CREATE TABLE IF NOT EXISTS `wage_settlement` (...);
CREATE TABLE IF NOT EXISTS `wage_settlement_detail` (...);
-- 完整建表语句见 schema.sql 末尾
```

**改动文件：**
- `garment-server/src/main/resources/db/schema.sql` — 新增2张表
- `garment-server/.../entity/WageSettlement.java` — 新增
- `garment-server/.../entity/WageSettlementDetail.java` — 新增
- `garment-server/.../mapper/WageSettlementMapper.java` — 新增
- `garment-server/.../mapper/WageSettlementDetailMapper.java` — 新增
- `garment-server/.../dto/CreateSettlementDTO.java` — 新增
- `garment-server/.../dto/SignSettlementDTO.java` — 新增
- `garment-server/.../vo/WageSettlementVO.java` — 新增
- `garment-server/.../vo/WageSettlementDetailVO.java` — 新增
- `garment-server/.../service/WageSettlementService.java` — 新增
- `garment-server/.../controller/WageSettlementController.java` — 新增
- `garment-web/src/api/wageSettlement.js` — 新增
- `garment-web/src/views/WageSettlement.vue` — 新增
- `garment-web/src/views/WageSettlementDetail.vue` — 新增
- `garment-web/src/views/mobile/MobileWageSign.vue` — 新增
- `garment-web/src/router/index.js` — 新增路由
- `garment-web/src/views/layout/Layout.vue` — 新增菜单
- `garment-web/src/views/mobile/MobileProfile.vue` — 新增工资签名入口

---

### 2026-06-02（报工流程重构：免审核 + 主管退回）

**完成内容：**
1. 报工流程重构（免审核直接生效）：
   - 员工扫码报工**不再填写数量**，默认整扎件数
   - 新增**备注输入**（选填）
   - 提交后**直接生效**，不再经过主管审核
   - 工序状态直接变为「已完成」，扎货完成自动检测
2. 主管退回功能：
   - 主管重新扫码 → 已完成的工序旁显示「退回」按钮
   - 退回需**填写退回原因**
   - 退回后工序状态恢复为「未开始」，员工可重新报工
   - 报工记录标记为「已退回」，保留历史留痕
3. 数据库变更：
   - `work_record` 表新增字段：`worker_remark`（员工备注）、`return_reason`（退回原因）、`returner_id`（退回人ID）、`return_time`（退回时间）
   - 状态扩展：0待审核(废弃) / 1通过 / 2驳回 / 3已退回
4. 后端改动：
   - `SubmitWorkRecordDTO` — 去掉 quantity，添加 remark
   - 新增 `ReturnWorkRecordDTO` — 退回原因
   - `WorkRecordService.submitWorkRecord()` — 免审核直接生效，数量=扎货件数
   - `WorkRecordService.returnWorkRecord()` — 退回工序逻辑
   - `BundleService.scanBundle()` — 已完成的工序附带报工人信息
   - `BundleVO.ProcessItem` — 新增 workRecordId、workerName、workerRemark、workTime
5. 前端改动：
   - `MobileScan.vue` — 去掉数量调整，添加备注输入，工序状态更新
   - `ScanReport.vue` — PC端同样简化 + 主管退回按钮
   - `WorkReview.vue` — 从审核页改为记录查看页（全部/已通过/已退回），支持退回操作
   - `Layout.vue` — 报工审核菜单只对主管显示（老板看不到）
   - `MobileRecords.vue` / `MobileHome.vue` — 状态映射更新，工资统计排除已退回记录
6. 编译验证：后端 `mvn compile` + 前端 `npm run build` 均通过

**需要执行的SQL：**
```sql
-- 如果 work_record 表已存在，执行以下 ALTER TABLE
ALTER TABLE `work_record` ADD COLUMN IF NOT EXISTS `worker_remark` VARCHAR(500) DEFAULT NULL COMMENT '员工报工备注';
ALTER TABLE `work_record` ADD COLUMN IF NOT EXISTS `return_reason` VARCHAR(500) DEFAULT NULL COMMENT '退回原因';
ALTER TABLE `work_record` ADD COLUMN IF NOT EXISTS `returner_id` BIGINT DEFAULT NULL COMMENT '退回人ID';
ALTER TABLE `work_record` ADD COLUMN IF NOT EXISTS `return_time` DATETIME DEFAULT NULL COMMENT '退回时间';
```

**改动文件：**
- `garment-server/src/main/resources/db/schema.sql` — work_record 表新增4个字段
- `garment-server/.../entity/WorkRecord.java` — 新增字段 + getter/setter
- `garment-server/.../dto/SubmitWorkRecordDTO.java` — 去掉 quantity，添加 remark
- `garment-server/.../dto/ReturnWorkRecordDTO.java` — 新增
- `garment-server/.../service/WorkRecordService.java` — 重写提交逻辑 + 新增退回方法
- `garment-server/.../service/BundleService.java` — scanBundle 返回报工记录信息
- `garment-server/.../controller/WorkRecordController.java` — 新增退回接口
- `garment-server/.../vo/WorkRecordVO.java` — 新增退回相关字段
- `garment-server/.../vo/BundleVO.java` — ProcessItem 新增报工信息字段
- `garment-web/src/api/workRecord.js` — 新增 returnWorkRecord API
- `garment-web/src/views/layout/Layout.vue` — 审核菜单只对主管显示
- `garment-web/src/views/mobile/MobileScan.vue` — 简化报工界面
- `garment-web/src/views/mobile/MobileRecords.vue` — 状态映射更新
- `garment-web/src/views/mobile/MobileHome.vue` — 统计逻辑更新
- `garment-web/src/views/ScanReport.vue` — 简化 + 主管退回
- `garment-web/src/views/WorkReview.vue` — 改为记录查看 + 退回
- `garment-web/src/views/Dashboard.vue` — 统计卡标签更新

---

### 2026-06-02（手机端员工端界面）

**完成内容：**
1. 手机端布局组件 `MobileLayout.vue`：
   - 顶部标题栏（页面标题 + 当前公司名称）
   - 底部导航栏（工作台 / 扫码 / 记录 / 我的）
   - 固定底部安全区适配（iPhone 手势区）
   - 页面切换滑入动画
2. 手机端工作台首页 `MobileHome.vue`：
   - 欢迎卡片（头像 + 问候语 + 日期）
   - **扫码报工大按钮**（渐变背景 + 浮动圆点动画）
   - 今日统计（已报工序 / 完成件数 / 预计收入）
   - 本月工资预估卡片（进度条：已审核 vs 待审核）
   - 最近报工记录列表（5条）
3. 手机端扫码报工页 `MobileScan.vue`：
   - 扫码视图（扫描框 + 扫描线动画 + 手动输入框）
   - 扎货信息卡片（款式、扎号、件数、尺码）
   - 工序选择列表（复选框 + 状态标签）
   - 数量调整（+/- 大按钮，适合戴手套操作）
   - 报工汇总（工序数 / 总件数 / 预计收入）
   - 固定底部提交按钮
4. 手机端记录页 `MobileRecords.vue`：
   - 日期筛选：今日 / 本周 / 本月 / 自定义
   - 统计卡片（报工次数 / 完成件数 / 预计收入）
   - 按日期分组的报工明细列表
   - 每条记录显示：款式、扎号、工序、数量、金额、状态、时间
5. 手机端个人中心 `MobileProfile.vue`：
   - 用户信息（头像、姓名、角色标签、公司名称）
   - 本月统计（收入 + 件数）
   - 功能菜单（报工记录、扫码报工、修改密码、关于系统、退出登录）
   - 修改密码弹窗 + 关于弹窗
6. 路由配置修改：
   - 添加 `/mobile/*` 路由体系
   - 工人角色访问任何 PC 端页面 → 自动跳转到 `/mobile`
   - 扫码页 `/scan-report` → `/mobile/scan`
7. 后端接口增强：
   - `WorkRecordController` 新增 `startDate`/`endDate` 参数
   - `WorkRecordService.listWorkRecords()` 支持日期范围筛选
8. 前端 API 调用：
   - 手机端所有记录查询自动带上 `workerId`（只能看自己的记录）

**设计特点（专为车间工人）：**
- 大按钮、大字体 — 戴手套也能准确点击
- 高对比度配色 — 车间光线不好也看得清
- 底部导航 — 单手操作，不用够屏幕顶部
- 数量加减按钮 — 圆形大按钮，方便调整件数
- 安全区适配 — iPhone 刘海屏、底部手势区自动留白

**改动文件：**
- `garment-web/src/views/layout/MobileLayout.vue` — 新增
- `garment-web/src/views/mobile/MobileHome.vue` — 新增
- `garment-web/src/views/mobile/MobileScan.vue` — 新增
- `garment-web/src/views/mobile/MobileRecords.vue` — 新增
- `garment-web/src/views/mobile/MobileProfile.vue` — 新增
- `garment-web/src/router/index.js` — 添加手机端路由 + 自动跳转逻辑
- `garment-server/.../controller/WorkRecordController.java` — 新增 startDate/endDate 参数
- `garment-server/.../service/WorkRecordService.java` — 日期范围筛选逻辑

---

### 2026-06-01（床号功能 - 第二版：款式简化+床号增强）

**完成内容：**
1. 款式管理简化：
   - 款式页面只保留款号输入，去掉名称、颜色、数量、工序配置
   - Style 实体只保留 id、companyId、styleNo、deleted、createTime、updateTime
   - StyleVO 只保留 id、styleNo、bedCount、createTime
   - 删除款式时级联删除床号和工序
2. 床号管理增强：
   - bed 表新增 name（名称）、colors（颜色）字段
   - 工序从 style 级别改为 bed 级别（process 表新增 bed_id 字段）
   - 每个床号可以独立配置工序（序号、名称、单价）
   - 床号管理放在款式编辑抽屉中
3. 分扎逻辑调整：
   - BundleService.batchSave 从床号获取工序模板（不再从款式）
   - 三步流程：选款式 → 选床号 → 分扎
4. 前端改动：
   - Style.vue — 简化款式表单，床号管理增加名称、颜色、工序配置
   - Bundle.vue — 床号卡片显示名称、颜色、工序数
   - api/style.js — 去掉 getProcesses、saveProcesses 接口
   - api/bed.js — 保持不变

**需要执行的SQL：**
```sql
ALTER TABLE `bed` ADD COLUMN `name` VARCHAR(100) DEFAULT NULL COMMENT '床号名称' AFTER `bed_no`;
ALTER TABLE `bed` ADD COLUMN `colors` VARCHAR(500) DEFAULT NULL COMMENT '颜色' AFTER `name`;
ALTER TABLE `process` ADD COLUMN `bed_id` BIGINT DEFAULT NULL COMMENT '所属床号' AFTER `style_id`;
DELETE FROM `process` WHERE `bed_id` IS NULL; -- 删除旧工序
```

**改动文件：**
- `garment-server/.../entity/Style.java` — 简化，只保留 styleNo
- `garment-server/.../entity/Bed.java` — 新增 name、colors 字段
- `garment-server/.../entity/Process.java` — 新增 bedId 字段
- `garment-server/.../dto/SaveStyleDTO.java` — 简化，只保留 styleNo
- `garment-server/.../dto/SaveBedDTO.java` — 新增 name、colors、processes 字段
- `garment-server/.../vo/StyleVO.java` — 简化，只保留 id、styleNo、bedCount、createTime
- `garment-server/.../vo/BedVO.java` — 新增 name、colors、processCount、processes 字段
- `garment-server/.../service/StyleService.java` — 简化，去掉工序管理逻辑
- `garment-server/.../service/BedService.java` — 增加工序管理逻辑
- `garment-server/.../service/BundleService.java` — 从床号获取工序模板
- `garment-server/.../controller/StyleController.java` — 去掉 getProcesses、saveProcesses 接口
- `garment-web/src/api/style.js` — 去掉 getProcesses、saveProcesses
- `garment-web/src/views/Style.vue` — 重写，简化款式表单，床号管理增强
- `garment-web/src/views/Bundle.vue` — 床号卡片显示更多信息

---

### 2026-06-01（床号功能 - 第一版）

**完成内容：**
1. 数据库新增 bed 表（床号表）
2. bundle 表新增 bed_id 字段
3. 后端新增 Bed 实体、Mapper、Service、Controller
4. 分扎逻辑从按款式改为按床号
5. 前端三步流程：选款式 → 选床号 → 分扎
6. 款式编辑抽屉增加床号管理区域

---

### 2026-06-01（分扎主管字母前缀）

**完成内容：**
1. 扎号规则调整：
   - 每个主管分配唯一字母前缀（A/B/C...），按入职顺序分配，不复用
   - 扎号格式：`{前缀}{数字}`，默认3位（A001~A999），超过999后自然扩展（A1000、A1001...）
   - boss 分扎默认前缀 A
2. 后端改动：
   - `user_company` 表新增 `bundle_prefix` 字段（VARCHAR(2)）
   - `UserCompany` 实体新增 `bundlePrefix` + getter/setter
   - `EmployeeService` — 添加主管时自动分配字母前缀（A→B→C...Z→AA→AB...）
   - `BundleService` — 扎号生成逻辑改用主管前缀，新增 `getMyPrefix()` 接口
   - `BundleController` — 新增 `GET /api/bundle/my-prefix` 接口
   - `EmployeeVO` — 新增 `bundlePrefix` 字段
3. 前端改动：
   - `bundle.js` — 新增 `getMyPrefix()` API
   - `Bundle.vue` — 扎号生成/placeholder 改用动态前缀，页面加载时获取当前用户前缀
   - `Employee.vue` — 主管职位标签旁显示扎号字母前缀
4. 后端编译 + 前端构建验证通过

**改动文件：**
- `garment-server/src/main/resources/db/schema.sql` — user_company 新增 bundle_prefix
- `garment-server/.../entity/UserCompany.java` — 新增字段
- `garment-server/.../service/EmployeeService.java` — 主管自动分配前缀 + 辅助方法
- `garment-server/.../service/BundleService.java` — 扎号生成改用前缀 + getMyPrefix
- `garment-server/.../controller/BundleController.java` — 新增 my-prefix 接口
- `garment-server/.../vo/EmployeeVO.java` — 新增 bundlePrefix 字段
- `garment-web/src/api/bundle.js` — 新增 API
- `garment-web/src/views/Bundle.vue` — 动态前缀
- `garment-web/src/views/Employee.vue` — 显示前缀标签

---

### 2026-06-01（扫码报工 + 审核优化）

**完成内容：**
1. 扫码报工页面优化：
   - 添加**手动输入框**（电脑端测试用，无摄像头可手动输入二维码UUID）
   - 二维码打印页面新增「测试报工」按钮，一键跳转测试
2. 工序报工防重复机制：
   - 前端：已报工工序显示锁定图标，不可点击
   - 后端：提交报工时校验工序状态，已报工返回错误提示
3. 报工审核页面全面重构 (`WorkReview.vue`)：
   - 顶部统计栏：待审核/已通过/已驳回数量，点击切换
   - **批量通过功能**：左侧勾选框 + 全选 + 批量通过按钮
   - 信息更全面：显示报工数量/扎货数量对比（如 30/50件）
   - **缺件红色提醒**：数量不足时显示「缺X件」标签 + 卡片变红背景
   - 工序进度条：可视化显示本扎工序完成比例
   - 驳回弹窗优化：6个常用原因选项（数量有误、工序未完成、质量不合格等）
4. 审核状态联动：
   - 驳回后 → 工序状态恢复为「未开始」，允许工人重新报工
   - 通过后 → 工序状态变为「已完成」，显示完成标签
5. 后端 VO 扩展 (`WorkRecordVO`)：
   - 新增 `workerPhone`（工人手机号）
   - 新增 `bundleQuantity`（扎货总件数）
   - 新增 `totalProcesses` / `completedProcesses`（工序进度）

**改动文件：**
- `garment-web/src/views/ScanReport.vue` — 添加手动输入框
- `garment-web/src/views/QrPrint.vue` — 添加测试报工按钮
- `garment-web/src/views/WorkReview.vue` — 全面重构（新增批量通过、缺件提醒、驳回选项）
- `garment-server/.../vo/WorkRecordVO.java` — 扩展字段
- `garment-server/.../service/WorkRecordService.java` — 驳回恢复工序状态

---

### 2026-05-29（晚上）

**完成内容（主管生成二维码流程设计）：**
1. 与橙子头脑风暴确认新业务流程：主管分扎 → 选工序模板 → 调整工序 → 生成二维码
2. 确认10个关键细节：
   - 二维码破损可重新打印
   - 不需要临时工，必须注册账号
   - 主管数据隔离（只能看自己的扎货和报工）
   - 支持补报（标红显示）
   - 主管自报问题：创建两个账号
   - 审核通过后支持撤回修改
   - 支持批量审核通过
   - 返工：重新生成二维码
   - 需要扎货进度可视化看板
   - 3天无报工超时标红提醒
3. 确认数据模型调整：
   - 新增 `bundle_process` 表（扎货工序快照）
   - 调整 `bundle` 表（增加 creator_id、team_id、qr_code、status 等）
   - 调整 `work_record` 表（增加 bundle_process_id、is_makeup、review_remark）
4. 确认业务规则：工序半严格顺序、数量控制、二维码失效逻辑
5. 输出设计文档：`docs/superpowers/specs/2026-05-29-主管生成二维码与扫码报工流程-design.md`

### 2026-05-31（分扎管理页面重构）

**完成内容：**
1. 分扎管理页面 (`Bundle.vue`) 全面重构：
   - 款式选择从下拉框改为**卡片网格**（与款式管理风格统一）
   - 每张卡片显示款式编号、名称、件数、工序数、分扎状态标签（已分扎/未分扎）
   - 顶部添加**搜索栏**（关键词搜索 + 300ms 防抖）
   - 分页支持（12/24/48 条每页）
2. 交互逻辑简化：
   - **未分扎款式** → 点击卡片自动生成表格（按默认5扎均分），可直接编辑
   - **已分扎款式** → 加载已保存扎货到编辑表格，内联编辑，无需弹窗
   - 去掉"设置扎数"独立步骤，压缩到快捷操作栏
   - 去掉"已保存扎货"只读表格，统一在一个表格里编辑
3. 统一保存机制：
   - 新建模式：批量保存
   - 更新模式：自动对比差异，分别调用 update/delete/create
4. 快捷操作栏保留：扎数设置、一键生成、统一件数、统一尺码、自动均分

**改动文件：**
- `garment-web/src/views\Bundle.vue` — 重写（997行 → 1019行）

### 2026-05-30（晚上 - 分扎+二维码+报工模块）

**完成内容：**
1. 数据库 — 新增 3 张表：`bundle`（扎货表）、`bundle_process`（扎货工序快照表）、`work_record`（报工记录表）
2. 后端实现：
   - `BundleController` — 5个接口：扎货列表、扎货详情、扫码查询、批量保存、删除扎货
   - `WorkRecordController` — 3个接口：提交报工、报工列表、审核报工
   - `BundleService` — 批量保存自动快照工序、自动生成扎号(B001)和二维码(UUID)
   - `WorkRecordService` — 报工计算金额、审核自动更新工序/扎货状态
   - 实体类：Bundle、BundleProcess、WorkRecord
   - DTO/VO：SaveBundleListDTO、SaveBundleItemDTO、SubmitWorkRecordDTO、ReviewWorkRecordDTO、BundleVO、WorkRecordVO
3. 前端实现：
   - 安装 `qrcode.vue@3` 依赖
   - `bundle.js` / `workRecord.js` — API 封装
   - `Bundle.vue` — 分扎管理页面（款式选择、批量生成、卡片式编辑、保存）
   - `QrPrint.vue` — 二维码打印页面（40×30mm标签排版、浏览器打印）
   - `ScanReport.vue` — 扫码报工页面（工人扫码、选择工序、提交）
   - `WorkReview.vue` — 报工审核页面（主管审核通过/驳回）
4. 路由和菜单 — 新增4条路由 + 侧边栏3个菜单（分扎管理、报工审核、扫码报工）
5. 后端编译 + 前端构建验证通过

---

### 2026-05-30（晚上）

**完成内容（款式管理 + 工序管理模块）：**
1. 数据库 — 新增 `style`（款式表）和 `process`（工序表）
2. 后端实现：
   - `StyleController` — 7个接口：款式列表、款式详情、工序列表、新增款式、编辑款式、删除款式、保存工序
   - `StyleService` — 完整业务逻辑，款式CRUD + 工序批量保存（先删后插）
   - `Style`、`Process` 实体类，`StyleVO`、`SaveStyleDTO`、`SaveProcessListDTO` 数据传输对象
3. 前端实现：
   - `style.js` — API 封装（7个函数）
   - `Style.vue` — 款式管理页面
   - 款式列表：搜索、添加、编辑、删除
   - 工序配置弹窗：表格形式增删工序，设置名称和计件单价
4. 路由和菜单 — 添加款式管理菜单（老板可见）
5. 后端编译 + 前端构建验证通过
6. 更新 `PROGRESS.md`

**追加完成（款式扩展字段）：**
1. style 表新增 3 个字段：`style_no`（款式编号）、`colors`（颜色，逗号分隔）、`image_url`（图片，预留）
2. 后端同步更新：实体类、DTO、VO、Service 均新增 styleNo 和 colors 字段
3. 前端实现：
   - 表格新增「款式编号」列（紫色等宽字体）和「颜色」列（圆点展示）
   - 弹窗新增「款式编号」输入框
   - 颜色选择器：7 个常用色（黑、白、灰、红、蓝、绿、黄），点击选中，带勾号标识
   - 已选颜色文本提示：「已选：红色、蓝色」
4. 修复 application.yml 语法错误（`configuration:cd` 和多余的 `claude`）
5. **当前停在这里**：需要在 MySQL 执行 ALTER TABLE 添加新字段，然后测试

### 2026-05-30（下午）

**完成内容（员工管理完善）：**
1. 离职/恢复机制重新设计 — 离职只改 `status=0`（不改 `deleted`），避免 MyBatis-Plus `@TableLogic` 导致查不到记录
2. 新增恢复入职接口 `PUT /api/employee/{id}/restore`
3. 前端添加「在职员工 / 已离职」Tab 切换
4. 在职列表：编辑 | 重置密码 | 离职
5. 离职列表：恢复入职
6. 菜单权限 — 「员工管理」仅老板可见（`isBoss()`）
7. 离职员工无法登录（登录时检查 `status=1`）
8. 后端编译重启验证通过

### 2026-05-30（上午）

**完成内容（员工管理模块）：**
1. 数据库变更 — user 表新增 `gender`（性别）和 `bank_card`（银行卡号）字段
2. 后端实现：
   - `EmployeeController` — 7个接口：添加员工、员工列表（支持 status 筛选）、编辑员工、员工离职、恢复入职、重置密码、检查手机号
   - `EmployeeService` — 完整业务逻辑，支持两种添加流程（新用户 / 已注册跨厂）
   - `EmployeeVO`、`CheckPhoneVO`、`AddEmployeeDTO` — 数据传输对象
3. 前端实现：
   - `employee.js` — API 封装
   - `Employee.vue` — 员工管理页面（列表/搜索/添加/编辑/离职/恢复）
   - 统计卡片（员工总数/主管/工人）
   - 支持跨厂员工添加：手机号已注册时自动检测并弹窗确认
   - 手机号脱敏 + 小眼睛切换显示
4. 路由和菜单 — 添加员工管理菜单
5. 更新 `PROGRESS.md`

### 2026-05-29

**上午完成内容：**
1. 公司禁用功能完善 — 禁用公司后，该公司所有员工无法登录（登录时检查关联公司状态）
2. 请求拦截增强 — JwtAuthFilter 增加公司禁用检查，被禁用公司返回 403
3. 公司列表搜索增强 — 支持按企业名、手机号码、老板名字、状态筛选
4. 禁用行直观展示 — 禁用公司整行变灰，状态标签使用深色"已禁用"带锁图标
5. 菜单顺序调整 — "公司管理"放到菜单最下方

**下午完成内容（员工管理设计）：**
1. 头脑风暴确认员工添加流程：老板填写信息 → 系统创建账号 + 关联本公司
2. 确认字段：必填（手机号、姓名、性别、职位），选填（身份证、银行卡）
3. 确认支持临时工跨厂：手机号已存在时弹窗确认，只创建关联不重复建账号
4. 初始密码统一 123456
5. 输出完整设计文档：`docs/superpowers/specs/2026-05-29-employee-management-design.md`

### 2026-05-28

**完成内容：**
1. 重新设计超级管理员流程 — 创建公司时自动生成老板账号（手机号作用户名，默认密码123456）
2. 公司列表接口改造 — 返回老板用户名和姓名，一屏看完
3. 新增重置老板密码接口（PUT /api/company/{id}/reset-password）
4. 新建前端公司管理页 CompanyManage.vue（列表/新增/编辑/禁用/重置密码）
5. 侧边栏添加"公司管理"菜单（仅超级管理员可见）
6. 仪表盘改造 — 超级管理员/普通用户分别显示不同统计卡片和快捷操作
7. 修复 Bug：AuthController 的 @RequestAttribute("userId") 改为从 TenantContext 获取，解决 403 问题

**当前可操作功能：**
- 使用初始化的 admin 凭据登录 → 进入超级管理员后台
- 新增公司（自动创建老板账号）→ 编辑/禁用/重置密码
- 禁用公司后该公司所有员工无法登录
- 老板用手机号/123456 登录 → 进入公司选择 → 进入工作台

**下一步：** 进入第二阶段，从员工管理或款式管理开始

---

---

## 第一阶段：核心框架 + 多租户基础（2周） ✅ 已完成

### 后端
- [x] Spring Boot 项目骨架搭建
- [x] MySQL 数据库初始化（garment_db）
- [x] 多租户数据隔离机制（company_id + TenantContext + X-Company-Id）
- [x] Spring Security + JWT 认证（JwtUtil、JwtAuthFilter）
- [x] CORS 跨域配置
- [x] MyBatis-Plus 分页 + 自动填充 createTime/updateTime
- [x] 统一响应体 Result\<T\>
- [x] 用户实体 + BCrypt 密码加密
- [x] 公司实体 + CRUD 接口（列表/新增/编辑/启停）
- [x] 创建公司时自动生成老板账号（手机号作用户名，默认密码123456）
- [x] 公司列表返回老板信息（bossUsername、bossRealName）
- [x] 重置老板密码接口（PUT /api/company/{id}/reset-password）
- [x] 用户公司关联实体（UserCompany，含角色：老板/主管/工人）
- [x] 登录接口（POST /api/auth/login）
- [x] 公司列表接口（GET /api/auth/companies）
- [x] DataInitializer 初始化管理员账号（密码来自环境变量或首次启动日志）
- [x] 修复 AuthController 403 Bug（@RequestAttribute → TenantContext）

### 前端
- [x] Vue 3 + Vite + Element Plus 项目搭建
- [x] Axios 封装（request.js，自动带 JWT + company_id，401 跳登录）
- [x] Pinia 用户状态管理（token、用户信息、当前公司ID）
- [x] 路由守卫（无 token 跳登录页）
- [x] 登录页（品牌面板 + 表单 + 入场动画）
- [x] 公司选择页（卡片式，角色标签，单公司自动进入）
- [x] 主布局 Layout（可折叠侧边栏 + 顶栏 + 时钟 + 用户下拉）
- [x] 仪表盘（超级管理员/普通用户分别显示不同统计和快捷操作）
- [x] 公司管理页（超级管理员：列表/新增/编辑/禁用 + 老板信息展示 + 重置密码）
- [x] 全局 CSS 设计系统（配色、动画、Element Plus 覆写）

### 数据库表
- [x] user（用户表）— 已扩展 gender、bank_card 字段
- [x] company（公司表）
- [x] user_company（用户公司关联表）
- [x] style（款式表）— 已扩展 style_no、colors、image_url 字段
- [x] process（工序表）

---

## 第二阶段：基础数据管理（2周） 🟡 进行中

### 后端
- [x] 员工管理接口（本公司员工 CRUD、角色分配）
- [x] 款式实体 + 接口（Style：名称、工序列表）
- [x] 工序实体 + 接口（Process：名称、序号、计件单价）
- [x] 扎货实体 + 接口（Bundle：扎号、数量、二维码）
- [x] 扎货工序快照（BundleProcess：工序名称/单价快照）
- [x] 报工记录实体 + 接口（WorkRecord：员工、扎货、工序、数量、单价、小计）
- [x] 报工审核接口（通过/驳回，驳回恢复工序状态）
- [ ] 物料实体 + 接口（Material：名称、规格、单位、类型、安全库存）
- [ ] 订单实体 + 接口（Order：客户、款式、数量、交期、状态）
- [x] 数据库表：bundle、bundle_process、work_record

### 前端
- [x] 公司管理页（超级管理员：列表/新增/编辑/禁用 + 老板信息 + 重置密码）
- [x] 员工管理页（老板：本公司员工列表 + 添加/编辑/角色分配）
- [x] 款式管理页（老板：款式列表 + 工序配置 + 计件单价）
- [x] 分扎管理页（主管/老板：选款式 → 批量生成 → 编辑 → 保存）
- [x] 二维码打印页（40×30mm标签排版，浏览器打印）
- [x] 扫码报工页（工人：扫码/手动输入 → 选择工序 → 填数量 → 提交，已报工工序锁定）
- [x] 报工审核页（主管：批量通过、缺件提醒、驳回常用选项、工序进度显示）
- [ ] 物料管理页（老板：物料档案 CRUD）
- [ ] 订单列表页（老板/主管：搜索、筛选、状态管理）
- [ ] 订单详情页（查看各工序进度）
- [x] 侧边栏菜单扩展（分扎管理 ✅、报工审核 ✅、扫码报工 ✅）

---

## 第三阶段：生产与报工（2周） ✅ 核心功能已完成

### 后端
- [x] 扎货实体 + 接口（Bundle：扎号、数量、二维码）
- [x] 扎货工序快照（BundleProcess：从款式模板复制，支持调整）
- [x] 报工记录实体 + 接口（WorkRecord：员工、扎货、工序、数量、单价、小计）
- [x] 报工审核接口（通过/驳回，自动更新工序/扎货状态）
- [x] 二维码生成（前端 qrcode.vue，UUID 唯一标识）
- [ ] 裁剪批次（暂不需要，主管直接分扎）
- [x] 进度查询接口（订单各工序完成扎数、百分比）

### 前端
- [x] 分扎管理页（主管：选款式 → 批量生成扎 → 编辑 → 保存）
- [x] 二维码打印页（40×30mm标签纸排版，浏览器 window.print）
- [x] 扫码报工页（工人：扫码/手动输入 → 勾选多道工序 → 填数量 → 提交）
- [x] 报工审核页（主管：批量通过、缺件提醒、驳回选项、进度显示）
- [ ] 我的报工页（工人：个人历史记录查看）
- [x] 进度看板页（订单各工序进度可视化）

---

## 第四阶段：工资与库存（2周） ⏳ 待开始

### 后端
- [x] 工资统计接口（按月/按人/按工序汇总）— 工资结算功能已实现按月按人汇总
- [ ] 工资导出接口（Excel）
- [ ] 出入库记录接口（StockRecord：入库/出库）
- [ ] 库存查询接口（实时库存量）
- [ ] 库存预警接口（低于安全库存提醒）
- [ ] 全局看板数据接口（超级管理员：公司数、订单数、员工数、产量趋势）

### 前端
- [x] 工资统计页（老板/主管：按月/按人汇总，导出 Excel）— 工资结算页面已实现
- [x] 工资明细页（工人：个人每日/每月工资）— 手机端工资签名页面已实现
- [ ] 出入库记录页（新增/查看出入库）
- [ ] 库存查询页（实时库存、预警提示）
- [x] 全局数据看板页（超级管理员：汇总数据 + 趋势图 + 员工分布）— 已完成
- [x] 首页仪表盘 — 主管工作台（主管：我的扎货/今日报工/款式进度/退回记录/最近报工）
- [x] 首页仪表盘 — 老板工作台（老板：今日产值/本月工资/在职员工/进行中款式/全公司款式进度）
- [x] 扫码出货页（主管：扫码查看工序完成进度，指派员工补做，确认出货/撤销出货）
- [x] 每日出货页（老板：按日期范围查看出货扎数/件数/款号/出货人，明细分页表格）

---

## 第五阶段：优化与上线（1周） ⏳ 待开始

- [ ] 手机端界面适配优化
- [x] 二维码扫码报工流程完善
- [ ] 全流程测试
- [ ] Bug 修复
- [ ] 部署上线

---

## 超管功能增强计划（按需推进）

> 基于 2026-06-03 评估，超级管理员当前已具备公司管理、续费、基础工作台等能力，以下功能待补充。

### 🔴 高优先级（近期开发）

#### 1. 公司数据穿透查看
- **需求来源**：项目需求文档 2.1 节明确要求"可穿透查看任意一家公司的订单、工资、库存等明细数据"
- **当前问题**：超管只能看汇总统计，无法帮客户排查具体数据问题
- **实现方案**：在公司管理列表/活跃公司排名增加「进入公司」按钮，超管临时切换 company_id 查看该公司全部数据（款式、员工、报工、工资结算等）
- **涉及文件**：`CompanyManage.vue`、`Dashboard.vue`、后端 TenantContext 穿透逻辑

#### 2. 全局操作日志 / 审计日志
- **需求来源**：SaaS 平台安全审计标配
- **实现方案**：新增 `operation_log` 表，关键接口（创建公司、禁用、续费、重置密码）自动记录；超管可按时间/操作人/类型筛选查看
- **涉及文件**：新增 `OperationLog.java` / `OperationLogMapper.java` / `OperationLogService.java` / `OperationLogController.java` + 前端日志查看页

#### 3. 系统级公告（全系统通知）
- **需求来源**：运营沟通刚需，区别于现有公司内部群发公告
- **实现方案**：`notification` 表增加 `scope` 字段（`company`/`global`）；超管发公告时可选范围；所有公司用户登录后都能看到全局公告
- **涉及文件**：`Notification.java`、`NotificationService.java`、`NotificationManage.vue`

### 🟡 中优先级（后续补充）

#### 4. 产量趋势图表
- **需求来源**：项目需求文档 2.1 节"全局看板"要求"产量趋势"
- **实现方案**：超管工作台增加近 30 天每日全局产值折线图
- **涉及文件**：`Dashboard.vue`、`DashboardService.java`、引入图表库（如 ECharts）
- **状态**：✅ 已完成（2026-06-03）— 近6个月产值趋势折线图 + 公司员工分布图

#### 5. 登录日志
- **实现方案**：新增 `login_log` 表，记录登录时间、IP、结果；超管可查看和筛选

### 🟢 低优先级（可选）

#### 6. 全局数据导出
- 超管导出所有公司汇总表（带老板联系方式、到期时间、员工数等）

#### 7. 系统配置面板
- 维护模式开关、默认密码策略等全局参数配置

---

## UI 统一 + 明亮/暗黑主题切换

- **状态**：✅ 已完成（2026-06-30）
- **实现方案**：
  - 引入 Element Plus 暗黑变量 `element-plus/theme-chalk/dark/css-vars.css`，新建 `src/styles/theme.css` 集中管理 `:root` 与 `html.dark` 语义变量（`--bg-page`/`--bg-card`/`--text-primary`/`--border-color` 等）
  - 新建 `src/stores/theme.js`（mode 持久化 localStorage、`toggle()` 切 `<html>` 的 `dark` 类、首启跟随系统 `prefers-color-scheme`，`initTheme()` 在挂载前调用防首屏闪烁）
  - 新建 `src/components/ThemeToggle.vue`（日/月图标按钮），接入 PC 顶栏、主管端、员工端三处布局
  - 通过覆盖 `--color-slate/-light/-mist` 文字变量 + 批量替换 `#fff` 卡片底为 `var(--bg-card)`，让全项目页面随主题切换；侧边栏保持炭灰不变
  - ECharts 适配：新建 `src/utils/echartsTheme.js`，`Dashboard.vue`/`EmployeeWork.vue` 图表坐标轴/文字色跟随主题，切换时自动重渲染
- **涉及文件**：`main.js`、`App.vue`、`styles/theme.css`、`stores/theme.js`、`components/ThemeToggle.vue`、`utils/echartsTheme.js`、`views/layout/{Layout,MobileLayout,MobileSupervisorLayout}.vue`、`views/{Dashboard,EmployeeWork}.vue` 及核心页面批量替换
- **范围说明**：核心界面（背景/卡片/文字/边框/按钮/表单/图表）已主题化；零散状态渐变与 Material 调色板暂保留（明亮下无影响），后续可迭代

---

## 生产管理 · 全部扎货二维码总览（可筛选 / 可打印）

- **状态**：✅ 已完成（2026-06-30）
- **背景**：原生产管理页二维码仅在「打印二维码」步骤按单个床号加载，无法跨款式/跨床号总览。新增入口可直接预览全公司所有扎货二维码并筛选、补打。
- **后端**：`GET /api/bundle` 新增 `keyword`（跨表模糊匹配款号/床号/名称/扎号）与 `printed`（0/1）筛选参数；`BundleService.listBundles` 增参实现（先查命中的 styleId/bedId 集合再 `or in`，空集合不拼 `in`）。原 `styleId/bedId/bundleNo` 调用全部兼容。
- **前端**：
  - 新建 `utils/bundleLabelPrint.js`，将标签打印纯函数（`getQrUrl`/`buildLabelHtml`/`openPrintWindow` 等）从 `BundlePrintDrawer.vue` 提取为公共 util，新旧组件共用，消除重复
  - 新建 `components/BundleQrOverviewDialog.vue`：4 列网格预览（每页 40 条）+ 筛选（关键词 / 款式下拉 / 打印状态）+ 卡片勾选 + 全选当前页 + 批量打印 + 分页；打印后自动标记并刷新
  - `views/Bundle.vue` 顶部加「查看全部二维码」入口按钮，挂载 dialog
- **涉及文件**：`BundleController.java`、`BundleService.java`、`utils/bundleLabelPrint.js`、`components/BundlePrintDrawer.vue`、`components/BundleQrOverviewDialog.vue`、`views/Bundle.vue`
- **验证**：后端 `mvn compile` 通过；前端 `npm run build` 通过（仅 @vueuse/core 无关警告）

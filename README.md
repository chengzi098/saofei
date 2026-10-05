# 制衣厂综合管理系统（扫菲程序）

为制衣厂开发的扫码报工与计件工资管理系统。支持多租户，覆盖生产进度跟踪、员工计件工资、库存物料管理三大核心模块。

## 项目信息

- **项目编号**：03
- **技术栈**：Spring Boot 3.2 + Vue 3 + MySQL 8
- **生产环境**：101.34.209.192（Ubuntu 22.04）
- **在线地址**：生产服务器 IP 直接访问
- **初始管理员**：账号 `admin`，密码来自 `ADMIN_INIT_PASSWORD` 或首次启动日志

## 系统架构

```
┌─────────────────────────────────────────────────┐
│                    Nginx :80                     │
├─────────────────────────────────────────────────┤
│  frontend/dist/    →  静态页面                   │
│  /api/*            →  反向代理到 :8080           │
├─────────────────────────────────────────────────┤
│              garment-server :8080                │
│         Spring Boot + MyBatis-Plus + JWT         │
│              MySQL 8 (company_id 多租户)          │
├─────────────────────────────────────────────────┤
│  print-agent :9876  →  ESC/POS 热敏标签打印      │
└─────────────────────────────────────────────────┘
```

## 用户角色

| 角色 | 设备 | 权限 |
|------|------|------|
| 超级管理员 | 电脑网页 | 管理所有公司租户 |
| 老板/公司管理员 | 电脑网页 | 本公司全局管理 |
| 车间主管 | 手机/平板 | 分配工序、审核报工 |
| 工人 | 手机 | 扫码报工、查工资 |

## 文件结构

```
扫菲程序/
├── garment-server/              # 后端（Spring Boot 3.2, Java 21）
│   └── src/main/java/com/garment/
│       ├── config/              # 配置（Security, JWT, MyBatis, 多租户）
│       ├── entity/              # 实体类
│       ├── mapper/              # MyBatis Mapper
│       ├── service/             # 业务逻辑
│       ├── controller/          # REST API
│       └── dto/ vo/             # 数据传输对象
│
├── garment-web/                 # 前端（Vue 3 + Element Plus + Pinia）
│   └── src/views/
│       ├── Login.vue            # 登录
│       ├── Dashboard.vue        # 仪表盘
│       ├── Employee.vue         # 员工管理
│       ├── Style.vue            # 款式管理
│       ├── Bundle.vue           # 分扎管理
│       ├── ProcessTemplate.vue  # 工序模板
│       ├── QrPrint.vue          # 二维码打印
│       ├── WorkRecords.vue      # 工作记录
│       ├── WorkReview.vue       # 工作审核
│       ├── WageSettlement.vue   # 工资结算
│       ├── WagePrint.vue        # 工资条打印
│       ├── mobile/              # 移动端（工人+主管）
│       └── layout/              # 布局组件
│
├── print-agent/                 # 打印代理（Node.js, 端口 9876）
│   └── index.js                 # ESC/POS 热敏打印服务
│
├── deploy/                      # 部署脚本与打包产物
│   ├── build.bat                # Windows 打包
│   ├── deploy.sh                # Linux 部署
│   ├── setup-server.sh          # 服务器环境安装
│   └── upload/                  # 预编译部署包
│
├── CLAUDE.md                    # Claude Code 项目指引
├── 项目需求.md                   # 需求文档
├── PROGRESS.md                  # 开发进度
├── 数据分析API文档.md            # API 文档
│
├── start-server.bat             # 启动后端
├── stop-server.bat              # 停止后端
├── backup-db.bat                # 数据库备份（Windows）
├── backup-linux.sh              # 数据库备份（Linux）
└── restore-db.bat               # 数据库恢复
```

## 本地开发

```bash
# 1. 启动后端
cd garment-server
mvn spring-boot:run

# 2. 启动前端
cd garment-web
npm install
npm run dev

# 3. 启动打印代理（可选）
cd print-agent
node index.js
```

## 部署

```bash
# Windows 打包
cd deploy
build.bat

# 上传到服务器后执行
chmod +x deploy.sh
./deploy.sh
```

详细部署文档：[[deploy/README.md]]

## 相关文档

- [[CLAUDE.md]] — 开发指引和技术细节
- [[项目需求.md]] — 完整需求文档
- [[PROGRESS.md]] — 开发进度
- [[deploy.md]] — 服务器部署指南
- [[数据分析API文档.md]] — API 接口文档

---

*维护者：橙子*
*最后更新：2026-06-16*

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

制衣厂综合管理系统（服装厂扫菲/计件工资系统）。前后端分离，多租户架构（所有业务表通过 `company_id` 隔离公司数据）。

详细需求见 `项目需求.md`，开发进度见 `PROGRESS.md`。

> **每次完成一个功能模块后，必须更新 `PROGRESS.md` 中对应项的 `[ ]` → `[x]`。**

## 项目结构

```
garment-server/                    → Spring Boot 3.2.5 后端（端口 8080）
├── src/main/java/com/garment/
│   ├── common/                    → 公共类：Result（统一响应）、TenantContext（租户上下文）
│   ├── config/                    → 配置类：JwtUtil、SecurityConfig、CorsConfig、MybatisPlusConfig
│   ├── controller/                → REST 控制器
│   ├── dto/                       → 数据传输对象
│   ├── entity/                    → MyBatis-Plus 实体类
│   ├── mapper/                    → Mapper 接口
│   ├── service/                   → 业务逻辑层
│   └── vo/                        → 视图对象
└── src/main/resources/
    └── db/schema.sql              → 建表 SQL

garment-web/                       → Vue 3 + Element Plus 前端（端口 5173）
├── src/
│   ├── api/                       → API 接口定义
│   ├── assets/                    → 静态资源
│   ├── components/                → 公共组件
│   ├── router/                    → 路由配置
│   ├── stores/                    → Pinia 状态管理
│   ├── utils/                     → 工具函数
│   └── views/                     → 页面组件
│       ├── layout/                → 布局组件
│       └── mobile/                → 移动端页面
└── public/                        → 公共静态文件
```

## 常用命令

```bash
# 后端
cd garment-server
mvn compile                    # 编译
mvn spring-boot:run            # 启动后端

# 前端
cd garment-web
npm install                    # 安装依赖
npm run dev                    # 启动开发服务器
npm run build                  # 构建生产包
```

## 技术栈

- **后端**: Spring Boot 3.2.5 + Spring Security + MyBatis-Plus 3.5.6 + MySQL + JWT (jjwt 0.12.5)
- **前端**: Vue 3 + Vite 8 + Element Plus + Pinia + Vue Router 4 + Axios
- **注意**: 不使用 Lombok（与当前 Java 25 不兼容），所有实体类手写 getter/setter

## 架构要点

### 后端包结构 (com.garment)

| 包 | 用途 |
|---|------|
| `config/` | 配置类：JwtUtil、SecurityConfig、CorsConfig、MybatisPlusConfig、DataInitializer |
| `common/` | 公共：Result（统一响应）、TenantContext（ThreadLocal 租户上下文） |
| `entity/` | MyBatis-Plus 实体，每个业务实体都有 company_id 字段 |
| `mapper/` | Mapper 接口，继承 BaseMapper |
| `service/` | 业务逻辑，继承 ServiceImpl |
| `controller/` | REST 控制器，统一返回 Result<T> |

### 认证流程

1. `POST /api/auth/login` → 返回 JWT token
2. 前端请求自动在 `Authorization: Bearer <token>` 和 `X-Company-Id` 头中携带信息
3. `SecurityConfig.JwtAuthFilter` 解析 token，将 userId/userType 写入 `TenantContext` ThreadLocal
4. `SecurityConfig` 放行 `/api/auth/login`，其余接口需认证
5. 超级管理员（type=0）无 company_id 限制，普通用户需在前端选择公司后带上 `X-Company-Id`

### 前端结构

- `src/api/request.js` — axios 封装，拦截器自动加 JWT 和 company_id，401 自动跳登录
- `src/stores/user.js` — Pinia 状态：token、用户信息、当前公司ID
- `src/router/index.js` — 路由守卫：无 token 跳登录页
- `src/views/Login.vue` — 登录后超级管理员进首页，普通用户进公司选择
- `src/views/CompanySelect.vue` — 多公司用户选择当前工作公司
- `src/views/mobile/` — 移动端页面（员工端、主管端）
- `src/views/QrPrint.vue` — 二维码打印页面（内嵌 iframe 打印）
- `src/views/Bundle.vue` — 分扎管理页面
- `src/views/WageSettlement.vue` — 工资结算页面
- `src/views/WagePrint.vue` — 工资条打印页面

### 多租户数据隔离

- 所有业务表必须有 `company_id` 字段
- 前端通过 `X-Company-Id` 请求头传递当前公司
- `TenantContext` ThreadLocal 存储当前 userId、companyId、isSuperAdmin
- 超级管理员不受 company_id 限制，可穿透查看所有公司数据

### 数据库

- 数据库名：`garment_db`
- 建表 SQL：`garment-server/src/main/resources/db/schema.sql`
- 默认管理员账号通过 `DataInitializer` 自动创建，初始密码来自环境变量或首次启动日志
- 逻辑删除字段 `deleted`：0=未删，1=已删

## 开发规范

- 新增业务模块时：entity → mapper → service → controller 四层依次创建
- 实体类不用 Lombok，手写 getter/setter
- Controller 返回 `Result<T>`，不要直接返回业务对象
- 前端 API 文件按模块命名（如 `auth.js`、`company.js`），统一通过 `request.js` 发请求
- 前端页面放 `src/views/`，路由在 `router/index.js` 中添加
- **重要**：新增实体字段时必须同步更新 `schema.sql` 的 CREATE TABLE 语句，不要依赖 ALTER TABLE（MySQL 不支持 `ADD COLUMN IF NOT EXISTS`）

## 生产环境部署

### 服务器信息
- **IP**: `101.34.209.192`
- **系统**: Ubuntu 22.04（4核 4G 40G）
- **数据库**: MySQL 8，密码通过 `DB_PASSWORD` 环境变量提供，数据库名 `garment_db`
- **JDK**: OpenJDK 21
- **Web 服务器**: Nginx（端口 80）+ Spring Boot（端口 8080）

### 部署架构
```
用户 → Nginx:80 → 静态文件（前端 dist）
                → /api/ 反向代理 → Spring Boot:8080
```

### 部署文件
- `deploy/setup-server.sh` — 服务器环境安装脚本（JDK、MySQL、Nginx）
- `deploy/deploy.sh` — 项目部署脚本
- `deploy/build.bat` — Windows 本地打包脚本
- `deploy/README.md` — 详细部署文档
- `garment-server/src/main/resources/application-prod.yml` — 生产环境配置

### 部署步骤
1. 本地运行 `deploy/build.bat` 打包 → 生成 `deploy/garment-deploy.zip`
2. `scp deploy/garment-deploy.zip ubuntu@101.34.209.192:/tmp/`
3. SSH 登录服务器，执行环境安装和部署（见 `deploy/README.md`）

### 常用运维命令
```bash
# 后端
sudo systemctl status garment      # 查看状态
sudo systemctl restart garment     # 重启
sudo tail -f /opt/garment/logs/backend.log  # 查看日志

# Nginx
sudo systemctl status nginx
sudo systemctl restart nginx

# 数据库
mysql -u root -p garment_db
```

# 员工管理模块设计文档

> 日期：2026-05-29
> 状态：已确认，待实现

---

## 1. 背景与目标

### 1.1 背景
制衣厂员工流动性大，临时工多，老板需要方便地管理本公司员工信息、分配角色（主管/工人）。

### 1.2 目标
- 老板可以在系统中直接注册员工账号
- 支持员工跨厂（临时工可属于多家公司）
- 员工登录后如有多家公司，进入公司选择页

---

## 2. 数据模型

### 2.1 user 表变更

新增两个字段：

```sql
ALTER TABLE `user` ADD COLUMN `gender` TINYINT DEFAULT NULL COMMENT '性别：0女 1男';
ALTER TABLE `user` ADD COLUMN `bank_card` VARCHAR(30) DEFAULT NULL COMMENT '银行卡号';
```

user 表完整字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名（=手机号） |
| password | VARCHAR(200) | BCrypt加密密码 |
| real_name | VARCHAR(50) | 真实姓名 |
| phone | VARCHAR(20) | 手机号 |
| id_card | VARCHAR(20) | 身份证号 |
| **gender** | **TINYINT** | **性别：0女 1男（新增）** |
| **bank_card** | **VARCHAR(30)** | **银行卡号（新增）** |
| type | TINYINT | 用户类型：0超级管理员 1普通用户 |
| status | TINYINT | 状态：0禁用 1启用 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 2.2 user_company 表（不变）

已有字段足够，无需变更：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| company_id | BIGINT | 公司ID |
| role | VARCHAR(20) | 角色：boss / supervisor / worker |
| team_id | BIGINT | 所属班组ID（预留） |
| hire_date | DATE | 入职时间 |
| status | TINYINT | 状态：0离职 1在职 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

---

## 3. 后端 API 设计

### 3.1 接口列表

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `POST /api/employee` | 新增 | 添加员工 | 老板 |
| `GET /api/employee` | 列表 | 本公司员工列表（分页+搜索） | 老板 |
| `PUT /api/employee/{id}` | 编辑 | 修改员工信息+角色 | 老板 |
| `DELETE /api/employee/{id}` | 删除 | 员工离职（逻辑删除user_company） | 老板 |
| `GET /api/employee/check-phone` | 检查 | 检测手机号是否已注册 | 老板 |

### 3.2 添加员工流程

**流程1 — 手机号未注册：**

```
POST /api/employee
Content-Type: application/json
X-Company-Id: 1

{
  "phone": "13800138000",
  "realName": "张三",
  "gender": 1,
  "idCard": "441234567890123456",
  "bankCard": "6222021234567890123",
  "role": "worker"
}
```

后端逻辑：
1. 校验 phone 是否已存在（user 表 username）
2. 不存在 → 创建 user 记录（username=phone, password=BCrypt("123456")）
3. 创建 user_company 记录（companyId=当前公司, role=指定角色, hireDate=今天）
4. 返回成功

**流程2 — 手机号已注册：**

先调用检查接口：

```
GET /api/employee/check-phone?phone=13800138000
```

返回：
```json
{
  "exists": true,
  "userId": 123,
  "realName": "张三",
  "existingCompanies": ["XX制衣厂"]
}
```

老板确认后调用：

```
POST /api/employee
Content-Type: application/json
X-Company-Id: 1

{
  "phone": "13800138000",
  "role": "worker",
  "existingUserId": 123
}
```

后端逻辑：
1. 校验 existingUserId 存在
2. 校验该用户是否已关联本公司（避免重复关联）
3. 只创建 user_company 记录
4. 返回成功

### 3.3 员工列表

```
GET /api/employee?page=1&size=20&keyword=张三
```

返回分页结果，字段：
- id（user_company.id）
- userId
- realName
- phone
- gender（0女 1男）
- idCard
- bankCard
- role（中文显示：主管/工人）
- hireDate
- status（在职/离职）

### 3.4 编辑员工

```
PUT /api/employee/{userCompanyId}
{
  "realName": "张三",
  "gender": 1,
  "idCard": "...",
  "bankCard": "...",
  "role": "supervisor"
}
```

- 同时修改 user 表（realName, gender, idCard, bankCard）
- 和 user_company 表（role）

### 3.5 员工离职

```
DELETE /api/employee/{userCompanyId}
```

- 逻辑删除 user_company 记录（deleted=1）
- user 表记录保留（员工可能在其他公司还有关联）

---

## 4. 前端页面设计

### 4.1 员工管理页路由

- 路径：`/employee`
- 角色权限：仅老板可见

### 4.2 页面布局

```
┌─────────────────────────────────────────┐
│ 员工管理                    [+ 添加员工] │
├─────────────────────────────────────────┤
│ [输入姓名/手机号...]  [搜索]            │
├─────────────────────────────────────────┤
│ 姓名 │ 手机号 │ 性别 │ 角色 │ 入职时间 │ 状态 │ 操作 │
├─────────────────────────────────────────┤
│ 张三 │ 138**** │ 男  │ 工人 │ 2026-05-01│ 在职 │ 编辑 离职│
│ 李四 │ 139**** │ 女  │ 主管 │ 2026-04-15│ 在职 │ 编辑 离职│
└─────────────────────────────────────────┘
```

### 4.3 添加员工弹窗

**正常情况（手机号未注册）：**

- 手机号（必填，校验11位手机号格式）
- 姓名（必填）
- 性别（必填，单选：男/女）
- 身份证号（选填，正则校验）
- 银行卡号（选填）
- 职位（必填，下拉：主管 / 工人）
- 入职时间（默认今天，日期选择器）

**手机号已注册时：**

弹窗顶部显示提示：
> "该手机号已注册（姓名：张三，已有公司：XX制衣厂），确认添加到本公司？"

表单变为只读：
- 姓名：张三（只读）
- 性别：男（只读）
- 身份证号：****（只读，脱敏）
- 银行卡号：****（只读，脱敏）

可编辑字段：
- 职位（必填）
- 入职时间（默认今天）

### 4.4 编辑员工弹窗

- 同添加弹窗，所有字段可编辑
- 手机号不可修改（username 不变）

---

## 5. 权限与数据隔离

- 所有接口强制校验 `X-Company-Id`
- 只能查看/操作本公司员工（user_company.company_id = 当前公司）
- 超级管理员不走此页面（超级管理员管理公司，不管理员工）

---

## 6. 边界情况处理

| 场景 | 处理 |
|------|------|
| 手机号已注册且已在本公司 | 直接报错"该员工已在公司中" |
| 手机号格式不对 | 表单校验拦截，提示正确格式 |
| 身份证格式不对 | 表单校验拦截（选填，填了就校验） |
| 老板填了自己的手机号 | 允许添加，但建议不要（系统不做强制限制） |
| 离职员工重新入职 | 新建一条 user_company 记录（支持重复hire） |

---

## 7. 实现顺序

1. 数据库变更（user表加字段）
2. 后端：EmployeeController + EmployeeService + 新增VO/DTO
3. 前端：employee.js API + Employee.vue 页面
4. 侧边栏添加"员工管理"菜单
5. 更新 PROGRESS.md

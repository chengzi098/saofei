# 数据分析 API 文档

> 本文档包含制衣厂管理系统的数据分析API，可以直接发给AI进行分析。

## 基础信息

- **Base URL**: `http://your-server/api/analysis`
- **认证方式**: JWT Token（在请求头中添加 `Authorization: Bearer <token>`）
- **公司ID**: 在请求头中添加 `X-Company-Id: <公司ID>`

## API 接口列表

### 1. 获取员工绩效分析（Top N）

**接口**: `GET /api/analysis/employee-performance`

**参数**:
- `topN` (可选，默认10): 返回前N名员工

**返回示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "workerId": 1,
      "workerName": "张三",
      "totalQuantity": 15600,
      "totalAmount": 93600.00,
      "monthQuantity": 2800,
      "monthAmount": 16800.00,
      "workDays": 22,
      "avgDailyOutput": 127.27
    }
  ]
}
```

**字段说明**:
- `workerId`: 工人ID
- `workerName`: 工人姓名
- `totalQuantity`: 总产量（件）
- `totalAmount`: 总工资（元）
- `monthQuantity`: 本月产量（件）
- `monthAmount`: 本月工资（元）
- `workDays`: 工作天数
- `avgDailyOutput`: 日均产量（件/天）

---

### 2. 获取产量趋势分析（最近N天）

**接口**: `GET /api/analysis/production-trend`

**参数**:
- `days` (可选，默认30): 最近N天

**返回示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "date": "2026-06-01",
      "quantity": 1560,
      "amount": 9360.00,
      "recordCount": 45
    }
  ]
}
```

**字段说明**:
- `date`: 日期（yyyy-MM-dd）
- `quantity`: 当日产量（件）
- `amount`: 当日产值（元）
- `recordCount`: 报工次数

---

### 3. 获取款式进度分析

**接口**: `GET /api/analysis/style-progress`

**返回示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "styleId": 1,
      "styleNo": "2026001",
      "styleName": "夏季T恤",
      "totalBundles": 100,
      "completedBundles": 75,
      "totalQuantity": 2000,
      "completedQuantity": 1500,
      "progressPercent": 75.00,
      "totalAmount": 12000.00
    }
  ]
}
```

**字段说明**:
- `styleId`: 款式ID
- `styleNo`: 款式编号
- `styleName`: 款式名称
- `totalBundles`: 总扎数
- `completedBundles`: 已完成扎数
- `totalQuantity`: 总件数
- `completedQuantity`: 已完成件数
- `progressPercent`: 进度百分比（%）
- `totalAmount`: 总产值（元）

---

### 4. 获取工序效率分析

**接口**: `GET /api/analysis/process-efficiency`

**返回示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "processId": 0,
      "processName": "裁剪",
      "totalQuantity": 5000,
      "avgPrice": 2.50,
      "workerCount": 8,
      "totalAmount": 12500.00
    }
  ]
}
```

**字段说明**:
- `processId`: 工序ID（0表示未知）
- `processName`: 工序名称
- `totalQuantity`: 总产量（件）
- `avgPrice`: 平均单价（元/件）
- `workerCount`: 参与工人数
- `totalAmount`: 总产值（元）

---

### 5. 获取工资分布分析

**接口**: `GET /api/analysis/wage-distribution`

**返回示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "range": "0-1000元",
      "count": 5,
      "percentage": 20.00
    },
    {
      "range": "1000-2000元",
      "count": 10,
      "percentage": 40.00
    }
  ]
}
```

**字段说明**:
- `range`: 工资区间
- `count`: 人数
- `percentage`: 占比（%）

---

### 6. 获取综合分析报告（一次性获取所有数据）

**接口**: `GET /api/analysis/report`

**返回示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "companyInfo": {
      "companyId": 1,
      "companyName": "示例制衣厂",
      "contactPerson": "张三",
      "contactPhone": "13800138000",
      "address": "广东省广州市...",
      "status": 1,
      "expireTime": "2027-06-13T00:00:00",
      "createTime": "2026-01-01T10:00:00",
      "workerCount": 25,
      "supervisorCount": 3,
      "styleCount": 12,
      "bedCount": 48,
      "monthQuantity": 15600,
      "monthAmount": 93600.00
    },
    "topPerformers": [...],
    "productionTrends": [...],
    "styleProgress": [...],
    "processEfficiency": [...],
    "wageDistribution": [...],
    "summary": {
      "公司名称": "示例制衣厂",
      "员工总数": 28,
      "工人数量": 25,
      "主管数量": 3,
      "活跃款式数": 12,
      "床次总数": 48,
      "本月产量": "15600件",
      "本月工资总额": "93600元",
      "Top3员工": [
        {"姓名": "张三", "本月产量": "2800件", "本月工资": "16800元"},
        {"姓名": "李四", "本月产量": "2600件", "本月工资": "15600元"},
        {"姓名": "王五", "本月产量": "2400件", "本月工资": "14400元"}
      ],
      "已完成款式": "5/12",
      "主要工资区间": "3000-5000元"
    }
  }
}
```

---

### 7. 获取数据摘要（适合快速分析）

**接口**: `GET /api/analysis/summary`

**返回示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "公司名称": "示例制衣厂",
    "员工总数": 28,
    "工人数量": 25,
    "主管数量": 3,
    "活跃款式数": 12,
    "床次总数": 48,
    "本月产量": "15600件",
    "本月工资总额": "93600元",
    "Top3员工": [...],
    "已完成款式": "5/12",
    "主要工资区间": "3000-5000元"
  }
}
```

---

## 使用示例

### 示例 1：获取综合分析报告

```bash
curl -X GET "http://your-server/api/analysis/report" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Company-Id: 1"
```

### 示例 2：获取员工绩效 Top 5

```bash
curl -X GET "http://your-server/api/analysis/employee-performance?topN=5" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Company-Id: 1"
```

### 示例 3：获取最近7天产量趋势

```bash
curl -X GET "http://your-server/api/analysis/production-trend?days=7" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Company-Id: 1"
```

---

## AI 分析建议

将以下数据发给AI，可以得到有价值的分析：

### 1. 综合分析报告（推荐）
**接口**: `GET /api/analysis/report`

**AI分析方向**:
- 公司整体运营状况评估
- 员工绩效排名和效率分析
- 款式进度预测和风险预警
- 工资结构合理性分析
- 产能瓶颈识别

### 2. 员工绩效分析
**接口**: `GET /api/analysis/employee-performance?topN=10`

**AI分析方向**:
- 高绩效员工特征识别
- 低绩效员工改进方案
- 激励机制设计建议

### 3. 产量趋势分析
**接口**: `GET /api/analysis/production-trend?days=30`

**AI分析方向**:
- 产量波动原因分析
- 产能预测
- 生产计划优化建议

### 4. 工资分布分析
**接口**: `GET /api/analysis/wage-distribution`

**AI分析方向**:
- 工资结构合理性评估
- 薪酬激励效果分析
- 成本控制建议

---

## 数据分析提示词模板

### 提示词 1：综合分析

```
请分析以下制衣厂数据，并给出：
1. 公司整体运营状况评估（优秀/良好/一般/较差）
2. 员工绩效分析：哪些员工表现优秀，哪些需要改进
3. 款式进度分析：哪些款式进度滞后，可能的原因
4. 工资结构分析：工资分布是否合理，是否需要调整
5. 产能瓶颈识别：当前最大的生产瓶颈是什么
6. 改进建议：针对发现的问题，提出3-5条具体可行的改进建议

数据如下：
[粘贴API返回的JSON数据]
```

### 提示词 2：员工绩效分析

```
请分析以下员工绩效数据，并给出：
1. Top 3 高绩效员工的共同特征
2. 低绩效员工的改进方案
3. 激励机制设计建议
4. 培训需求分析

数据如下：
[粘贴API返回的JSON数据]
```

### 提示词 3：产量趋势分析

```
请分析以下产量趋势数据，并给出：
1. 产量波动的主要原因
2. 未来7天的产量预测
3. 生产计划优化建议
4. 产能提升方案

数据如下：
[粘贴API返回的JSON数据]
```

---

## 注意事项

1. **权限控制**: 所有接口都需要登录并选择公司
2. **数据范围**: 只能查看当前登录用户所在公司的数据
3. **实时统计**: 每次调用都会实时计算，数据是最新的
4. **性能考虑**: 综合分析报告接口返回数据较多，建议适当调用
5. **数据安全**: 请勿将敏感数据泄露给第三方AI服务

---

## 常见问题

### Q1: 如何获取认证Token？
A1: 调用登录接口 `POST /api/auth/login`，传入用户名和密码即可获取Token。

### Q2: 如何获取公司ID？
A2: 登录后，如果用户属于多个公司，需要先选择公司，公司ID会在响应中返回。

### Q3: 数据更新频率？
A3: 所有数据都是实时统计的，每次调用都会获取最新数据。

### Q4: 是否支持导出数据？
A4: 目前只支持JSON格式返回，后续可以考虑添加Excel导出功能。

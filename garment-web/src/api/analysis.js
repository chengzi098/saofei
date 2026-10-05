import request from './request'

/**
 * 数据分析 API（用于AI分析）
 */

// 获取员工绩效分析（Top N）
export function getEmployeePerformance(topN = 10) {
  return request.get('/analysis/employee-performance', { params: { topN } })
}

// 获取产量趋势分析（最近N天）
export function getProductionTrend(days = 30) {
  return request.get('/analysis/production-trend', { params: { days } })
}

// 获取款式进度分析
export function getStyleProgress() {
  return request.get('/analysis/style-progress')
}

// 获取工序效率分析
export function getProcessEfficiency() {
  return request.get('/analysis/process-efficiency')
}

// 获取工资分布分析
export function getWageDistribution() {
  return request.get('/analysis/wage-distribution')
}

// 获取综合分析报告（一次性获取所有数据）
export function getAnalysisReport() {
  return request.get('/analysis/report')
}

// 获取数据摘要（适合快速分析）
export function getSummary() {
  return request.get('/analysis/summary')
}

/**
 * 员工工作汇总（用于老板端「员工工作」页面汇总卡片）
 * @param {Object} params { startDate, endDate, workerId? }
 */
export function getEmployeeWorkSummary(params) {
  return request.get('/work-record/summary', { params })
}

/**
 * 报工筛选候选项（款号/床号/扎号/工序联动，去重，新增在上）
 * @param {Object} params { styleNo?, bedNo?, bundleNo? }
 */
export function getFilterOptions(params) {
  return request.get('/work-record/filter-options', { params })
}

/**
 * 员工每扎作业耗时（相邻报工时间差法，用于作业速度柱状图）
 * @param {Object} params { startDate, endDate, workerId }
 */
export function getWorkerSpeed(params) {
  return request.get('/work-record/worker-speed', { params })
}

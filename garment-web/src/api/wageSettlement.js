import request from './request'

// 获取上次结算信息
export function getLastSettlement() {
  return request.get('/wage-settlement/last')
}

// 预览全员明细（不保存）
export function previewSettlement(data) {
  return request.post('/wage-settlement/preview', data)
}

// 批量确认员工（生成结算记录）
export function confirmEmployees(data) {
  return request.post('/wage-settlement/confirm', data)
}

// 结算单列表（老板）
export function getSettlementList() {
  return request.get('/wage-settlement')
}

// 结算单详情（含工人明细）
export function getSettlementDetail(id) {
  return request.get(`/wage-settlement/${id}`)
}

// 某个工人的报工明细（老板查看）
export function getWorkerDetail(settlementId, workerId) {
  return request.get(`/wage-settlement/${settlementId}/worker/${workerId}`)
}

// 设置员工可见性
export function setEmployeeVisibility(settlementId, workerId, visible) {
  return request.post(`/wage-settlement/${settlementId}/worker/${workerId}/visibility`, { visible })
}

// 员工端：我的工资结算
export function getMySettlements() {
  return request.get('/wage-settlement/my')
}

// 员工端：确认查看工资
export function confirmViewing(settlementId) {
  return request.post(`/wage-settlement/${settlementId}/confirm-view`)
}

// 工资统计（按月查看报工工资汇总）
export function getWageStatistics(yearMonth) {
  return request.get('/wage-settlement/statistics', { params: { yearMonth } })
}

// 打印工资条数据（所有工人按工序汇总）
export function getPrintData(id) {
  return request.get(`/wage-settlement/${id}/print-data`)
}

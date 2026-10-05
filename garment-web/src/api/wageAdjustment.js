import request from './request'

// 添加调整记录（老板/主管）
export function createAdjustment(data) {
  return request.post('/wage-adjustment', data)
}

// 调整列表（老板/主管）
export function getAdjustmentList(params) {
  return request.get('/wage-adjustment', { params })
}

// 编辑（老板/主管，仅待审批）
export function updateAdjustment(id, data) {
  return request.post(`/wage-adjustment/${id}/update`, data)
}

// 审批（仅老板）
export function approveAdjustment(id) {
  return request.post(`/wage-adjustment/${id}/approve`)
}

// 拒绝审批（仅老板）
export function rejectAdjustment(id) {
  return request.post(`/wage-adjustment/${id}/reject`)
}

// 删除（老板/主管）
export function deleteAdjustment(id) {
  return request.post(`/wage-adjustment/${id}/delete`)
}

// 工人查看自己的记录
export function getMyAdjustments() {
  return request.get('/wage-adjustment/my')
}

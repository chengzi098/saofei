import request from './request'

// 标记一扎为已出货
export function shipBundle(data) {
  return request.post('/shipment/ship', data)
}

// 撤销出货
export function unshipBundle(data) {
  return request.post('/shipment/unship', data)
}

// 每日出货明细分页
export function getShipmentDaily(params) {
  return request.get('/shipment/daily', { params })
}

// 出货汇总
export function getShipmentSummary(params) {
  return request.get('/shipment/summary', { params })
}

// 未出货扎货清单（含生产进度）
export function getUnshippedList(params) {
  return request.get('/shipment/unshipped', { params })
}

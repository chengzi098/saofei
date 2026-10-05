import request from './request'

export function getMyPrefix() {
  return request.get('/bundle/my-prefix')
}

export function getBundleList(params) {
  return request.get('/bundle', { params })
}

export function getBundleById(id) {
  return request.get(`/bundle/${id}`)
}

export function scanBundle(qrCode) {
  // 对二维码内容进行 URL 编码，避免特殊字符导致路径解析错误
  return request.get(`/bundle/scan/${encodeURIComponent(qrCode)}`)
}

export function batchSaveBundles(data) {
  return request.post('/bundle/batch', data)
}

export function deleteBundle(id) {
  return request.delete(`/bundle/${id}`)
}

export function updateBundle(id, data) {
  return request.put(`/bundle/${id}`, data)
}

export function markPrintedBundles(ids) {
  return request.post('/bundle/mark-printed', ids)
}

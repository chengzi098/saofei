import request from './request'

export function getStyleList(params) {
  return request.get('/style', { params })
}

export function addStyle(data) {
  return request.post('/style', data)
}

export function updateStyle(id, data) {
  return request.put(`/style/${id}`, data)
}

export function deleteStyle(id) {
  return request.delete(`/style/${id}`)
}

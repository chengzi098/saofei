import request from './request'

export function getBeds(styleId) {
  return request.get(`/style/${styleId}/bed`)
}

export function addBed(styleId, data) {
  return request.post(`/style/${styleId}/bed`, data)
}

export function updateBed(id, data) {
  return request.put(`/bed/${id}`, data)
}

export function deleteBed(id) {
  return request.delete(`/bed/${id}`)
}

import request from './request'

export function getEmployeeList(params) {
  return request.get('/employee', { params })
}

export function restoreEmployee(id) {
  return request.put(`/employee/${id}/restore`)
}

export function addEmployee(data) {
  return request.post('/employee', data)
}

export function batchAddEmployees(data) {
  return request.post('/employee/batch', data)
}

export function updateEmployee(id, data) {
  return request.put(`/employee/${id}`, data)
}

export function deleteEmployee(id) {
  return request.delete(`/employee/${id}`)
}

export function checkPhone(phone) {
  return request.get('/employee/check-phone', { params: { phone } })
}

export function resetEmployeePassword(id) {
  return request.put(`/employee/${id}/reset-password`)
}

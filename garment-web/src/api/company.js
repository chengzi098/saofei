import request from './request'

export function getCompanyList(params) {
  return request.get('/company/list', { params })
}

export function getCompanyById(id) {
  return request.get(`/company/${id}`)
}

export function getCompanyInfo() {
  return request.get('/company/info')
}

export function createCompany(data) {
  return request.post('/company', data)
}

export function updateCompany(id, data) {
  return request.put(`/company/${id}`, data)
}

export function toggleCompanyStatus(id, status) {
  return request.put(`/company/${id}/status`, null, { params: { status } })
}

export function resetBossPassword(id) {
  return request.put(`/company/${id}/reset-password`)
}

export function renewCompany(id, months) {
  return request.put(`/company/${id}/renew`, { months })
}

export function getRenewLogs(id) {
  return request.get(`/company/${id}/renew-logs`)
}

export function getAllRenewLogs(params) {
  return request.get('/company/renew-logs', { params })
}

import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function getCompanies() {
  return request.get('/auth/companies')
}

export function changePassword(data) {
  return request.post('/auth/change-password', data)
}

import request from './request'

export function getDashboard() {
  return request.get('/dashboard')
}

export function getHistoryStyles() {
  return request.get('/style/history')
}

export function archiveStyle(id) {
  return request.put(`/style/${id}/archive`)
}

export function unarchiveStyle(id) {
  return request.put(`/style/${id}/unarchive`)
}

export function getSupervisorStats() {
  return request.get('/dashboard/supervisor-stats')
}

export function getBossStats() {
  return request.get('/dashboard/boss-stats')
}

export function getAdminStats() {
  return request.get('/dashboard/admin-stats')
}

export function getWorkerMonthlyIncome() {
  return request.get('/dashboard/worker-monthly-income')
}

export function getWorkerNotifications() {
  return request.get('/dashboard/worker-notifications')
}

export function markNotificationsViewed(type) {
  return request.post(`/dashboard/worker-notifications/viewed?type=${type}`)
}

export function getSupervisorNotifications() {
  return request.get('/dashboard/supervisor-notifications')
}

export function markSupervisorNotificationsViewed(type) {
  return request.post(`/dashboard/supervisor-notifications/viewed?type=${type}`)
}

export function getBossNotifications() {
  return request.get('/dashboard/boss-notifications')
}

export function markBossNotificationsViewed(type) {
  return request.post(`/dashboard/boss-notifications/viewed?type=${type}`)
}

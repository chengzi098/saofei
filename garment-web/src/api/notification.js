import request from './request'

// 发送通知
export function sendNotification(data) {
  return request.post('/notification/send', data)
}

// 我发送的通知列表（老板/主管）
export function getSentNotifications() {
  return request.get('/notification/sent')
}

// 我收到的通知列表（员工端）
export function getMyNotifications() {
  return request.get('/notification/my')
}

// 标记单条已读
export function markAsRead(id) {
  return request.post(`/notification/${id}/read`)
}

// 全部标记已读
export function markAllAsRead() {
  return request.post('/notification/read-all')
}

// 未读计数
export function getUnreadCount() {
  return request.get('/notification/unread-count')
}

// 删除通知
export function deleteNotification(id) {
  return request.delete(`/notification/${id}`)
}

// 获取通知已读详情（谁读了、谁没读）
export function getReadDetail(id) {
  return request.get(`/notification/${id}/read-detail`)
}

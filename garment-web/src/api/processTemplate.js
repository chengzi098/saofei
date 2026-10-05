import request from './request'

export function getProcessTemplateList() {
  return request.get('/process-template')
}

export function getProcessTemplateDetail(id) {
  return request.get(`/process-template/${id}`)
}

export function addProcessTemplate(data) {
  return request.post('/process-template', data)
}

export function updateProcessTemplate(id, data) {
  return request.put(`/process-template/${id}`, data)
}

export function deleteProcessTemplate(id) {
  return request.delete(`/process-template/${id}`)
}

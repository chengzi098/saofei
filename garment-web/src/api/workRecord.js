import request from './request'

export function submitWorkRecord(data) {
  return request.post('/work-record', data)
}

export function getWorkRecordList(params) {
  return request.get('/work-record', { params })
}

export function returnWorkRecord(bundleProcessId, data) {
  return request.put(`/work-record/bundle-process/${bundleProcessId}/return`, data)
}

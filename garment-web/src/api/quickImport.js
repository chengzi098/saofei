import request from './request'

/**
 * 一键录入：自动查找或创建款号、床号，批量创建扎货
 */
export function quickImport(data) {
  return request.post('/quick-import', data)
}

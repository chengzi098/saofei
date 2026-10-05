import { timingSafeEqual } from 'node:crypto'

const SAFE_PRINTER_NAME = /^[\p{L}\p{N} ._()\-]{1,128}$/u
const SAFE_PORT = /^(USB\d{3}|LPT\d+|COM\d+|IP_[A-Za-z0-9._:-]{1,80})$/
const MAX_LABELS = 500
const MAX_FIELD_LENGTH = 2048

export function isAllowedOrigin(origin, allowedOrigins) {
  return !origin || allowedOrigins.includes(origin)
}

export function isValidAgentKey(provided, expected) {
  if (!provided || !expected) return false
  const providedBuffer = Buffer.from(provided)
  const expectedBuffer = Buffer.from(expected)
  return providedBuffer.length === expectedBuffer.length
    && timingSafeEqual(providedBuffer, expectedBuffer)
}

export function isValidPrinterPort(port) {
  return typeof port === 'string' && SAFE_PORT.test(port)
}

export function validatePrintPayload({ printerName, labels }) {
  if (!SAFE_PRINTER_NAME.test(printerName || '')) {
    throw new Error('打印机名称无效')
  }
  if (!Array.isArray(labels) || labels.length === 0) {
    throw new Error('没有标签数据')
  }
  if (labels.length > MAX_LABELS) {
    throw new Error('单次最多打印500张标签')
  }
  for (const label of labels) {
    if (!label || typeof label !== 'object') throw new Error('标签数据无效')
    for (const value of Object.values(label)) {
      if (typeof value === 'string' && value.length > MAX_FIELD_LENGTH) {
        throw new Error('标签字段过长')
      }
    }
  }
}

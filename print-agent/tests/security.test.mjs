import assert from 'node:assert/strict'
import {
  isAllowedOrigin,
  isValidAgentKey,
  isValidPrinterPort,
  validatePrintPayload
} from '../security.js'

assert.equal(isAllowedOrigin('https://www.fjsaofei.cn', [
  'https://www.fjsaofei.cn'
]), true)
assert.equal(isAllowedOrigin('https://evil.example', [
  'https://www.fjsaofei.cn'
]), false)
assert.equal(isAllowedOrigin(undefined, ['https://www.fjsaofei.cn']), true)

assert.equal(isValidAgentKey('correct-key', 'correct-key'), true)
assert.equal(isValidAgentKey('wrong-key', 'correct-key'), false)
assert.equal(isValidAgentKey('', 'correct-key'), false)

assert.equal(isValidPrinterPort('USB001'), true)
assert.equal(isValidPrinterPort('LPT1'), true)
assert.equal(isValidPrinterPort('IP_192.168.1.20'), true)
assert.equal(isValidPrinterPort('USB001 & whoami'), false)
assert.equal(isValidPrinterPort('../file'), false)

assert.doesNotThrow(() => validatePrintPayload({
  printerName: 'CX-805',
  labels: [{ bundleNo: '001', quantity: 20, qrCode: 'B-001' }]
}))
assert.throws(() => validatePrintPayload({
  printerName: 'CX-805',
  labels: Array.from({ length: 501 }, () => ({ qrCode: 'x' }))
}), /500/)
assert.throws(() => validatePrintPayload({
  printerName: 'CX-805 & whoami',
  labels: [{ qrCode: 'x' }]
}), /打印机名称/)
assert.throws(() => validatePrintPayload({
  printerName: 'CX-805',
  labels: [{ qrCode: 'x'.repeat(2049) }]
}), /字段过长/)

console.log('print agent security ok')

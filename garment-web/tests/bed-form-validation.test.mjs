import assert from 'node:assert/strict'
import { validateBedForm, normalizeBedName } from '../src/utils/bedFormValidation.js'

assert.equal(normalizeBedName('  大货  '), '大货')

assert.deepEqual(
  validateBedForm({ bedNo: '1号床', name: '', quantity: 100 }),
  { valid: false, message: '请输入名称' }
)

assert.deepEqual(
  validateBedForm({ bedNo: '1号床', name: '大货', quantity: 100 }),
  { valid: true }
)

console.log('bed form validation ok')

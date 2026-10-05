import assert from 'node:assert/strict'
import {
  buildQrPrintParams,
  filterSelectedBed,
  formatBedDisplayName,
  normalizeBundleNo
} from '../src/utils/qrPrintFilters.js'

assert.deepEqual(
  buildQrPrintParams({ styleId: 3, bedId: 8, bundleNo: ' A001 ' }),
  { pageNum: 1, pageSize: 500, styleId: 3, bedId: 8, bundleNo: 'A001' }
)

assert.deepEqual(
  buildQrPrintParams({ styleId: 3, bedId: '', bundleNo: '' }),
  { pageNum: 1, pageSize: 500, styleId: 3 }
)

assert.equal(normalizeBundleNo(' a001 '), 'A001')
assert.equal(normalizeBundleNo(''), '')

assert.equal(filterSelectedBed([{ id: 1 }, { id: 2 }], 2), 2)
assert.equal(filterSelectedBed([{ id: 1 }, { id: 2 }], 9), '')

assert.equal(formatBedDisplayName({ bedNo: '1号床', bedName: '大货' }), '1号床 · 大货')
assert.equal(formatBedDisplayName({ bedNo: '1号床', name: '返工' }), '1号床 · 返工')
assert.equal(formatBedDisplayName({ bedNo: '1号床' }), '1号床')

console.log('qr print filters ok')

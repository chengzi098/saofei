import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const {
  normalizeDateRange,
  getMobileDatePreset
} = await import('../src/utils/mobileDateRange.js')

const now = new Date(2026, 5, 18, 12, 0, 0)

assert.deepEqual(normalizeDateRange(['2026-06-18', '2026-06-01']), ['2026-06-18', '2026-06-18'])
assert.deepEqual(normalizeDateRange(['2026-06-01', '2026-06-18']), ['2026-06-01', '2026-06-18'])
assert.deepEqual(getMobileDatePreset('today', now), ['2026-06-18', '2026-06-18'])
assert.deepEqual(getMobileDatePreset('recent7', now), ['2026-06-12', '2026-06-18'])
assert.deepEqual(getMobileDatePreset('week', now), ['2026-06-15', '2026-06-18'])
assert.deepEqual(getMobileDatePreset('month', now), ['2026-06-01', '2026-06-18'])

const pickerSource = readFileSync(resolve(root, 'src/components/mobile/MobileDateRangePicker.vue'), 'utf8')
assert.equal((pickerSource.match(/type="date"/g) || []).length, 2)
assert(pickerSource.includes(':min="startDate"'))
assert(pickerSource.includes('最近7天'))

const recordsSource = readFileSync(resolve(root, 'src/views/mobile/MobileRecords.vue'), 'utf8')
const reviewSource = readFileSync(resolve(root, 'src/views/mobile/MobileSupervisorReview.vue'), 'utf8')
const boardSource = readFileSync(resolve(root, 'src/views/mobile/MobileSupervisorBoard.vue'), 'utf8')

assert(recordsSource.includes('MobileDateRangePicker'))
assert(!recordsSource.includes('<el-date-picker'))
assert(reviewSource.includes('MobileDateRangePicker'))
assert(boardSource.includes('搜索款号或床号'))
assert(boardSource.includes('匹配床号'))
assert(boardSource.includes('openMatchedBed'))
assert(boardSource.includes('v-model="activeSearchKeyword"'))
assert(boardSource.includes(':placeholder="searchPlaceholder"'))
assert(boardSource.includes("const bedSearchKeyword = ref('')"))
assert(boardSource.includes('const filteredBeds = computed'))
assert(boardSource.includes('v-for="(bed, index) in filteredBeds"'))
assert(boardSource.includes('没有匹配的床号'))

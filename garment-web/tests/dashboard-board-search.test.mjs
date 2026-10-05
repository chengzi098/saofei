import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const {
  filterDashboardBeds,
  filterDashboardStyles,
  getMatchedBeds
} = await import('../src/utils/dashboardBoardSearch.js')

const styles = [
  {
    styleId: 1,
    styleNo: 'TX-001',
    beds: [
      { bedId: 11, bedNo: '1号床' },
      { bedId: 12, bedNo: '返工床-A' }
    ]
  },
  {
    styleId: 2,
    styleNo: 'TX-002',
    beds: [
      { bedId: 21, bedNo: '2号床' },
      { bedId: 22, bedNo: '返工床-B' }
    ]
  }
]

assert.deepEqual(filterDashboardStyles(styles, 'tx-001').map((style) => style.styleId), [1])
assert.deepEqual(filterDashboardStyles(styles, '2号床').map((style) => style.styleId), [2])
assert.deepEqual(filterDashboardStyles(styles, '不存在'), [])
assert.deepEqual(getMatchedBeds(styles[0], '返工床').map((bed) => bed.bedId), [12])
assert.deepEqual(getMatchedBeds(styles[1], '返工床').map((bed) => bed.bedId), [22])
assert.deepEqual(getMatchedBeds(styles[0], 'TX-001'), [])
assert.deepEqual(filterDashboardBeds(styles[0].beds, '').map((bed) => bed.bedId), [11, 12])
assert.deepEqual(filterDashboardBeds(styles[0].beds, ' 返工床-a ').map((bed) => bed.bedId), [12])
assert.deepEqual(filterDashboardBeds(styles[0].beds, '不存在'), [])

const boardSource = readFileSync(resolve(root, 'src/views/DashboardBoard.vue'), 'utf8')
assert(boardSource.includes('搜索款号或床号'))
assert(boardSource.includes('匹配床号'))
assert(boardSource.includes('selectMatchedBed'))

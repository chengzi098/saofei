import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')

const {
  PRODUCTION_STAGES,
  getProductionStage
} = await import('../src/utils/productionWorkflow.js')

assert.deepEqual(
  PRODUCTION_STAGES.map((stage) => stage.key),
  ['style', 'bed', 'bundle', 'print']
)

assert.equal(getProductionStage({}), 'style')
assert.equal(getProductionStage({ styleId: 1 }), 'bed')
assert.equal(getProductionStage({ styleId: 1, bedId: 2 }), 'bundle')
assert.equal(getProductionStage({ styleId: 1, bedId: 2, printOpen: true }), 'print')

const bundleSource = readFileSync(resolve(root, 'src/views/Bundle.vue'), 'utf8')
const layoutSource = readFileSync(resolve(root, 'src/views/layout/Layout.vue'), 'utf8')
const routerSource = readFileSync(resolve(root, 'src/router/index.js'), 'utf8')

assert(bundleSource.includes('BundlePrintDrawer'))
assert(bundleSource.includes('新增款式'))
assert(bundleSource.includes('新增床号'))
assert(bundleSource.includes('返回款式列表'))
assert(bundleSource.includes('返回床号列表'))
assert(bundleSource.includes('level-back-button'))
assert(!bundleSource.includes("router.push({ name: 'QrPrint'"))

assert.equal((layoutSource.match(/index="\/style"/g) || []).length, 1)
assert(!layoutSource.includes('index="/bundle"'))
assert(!layoutSource.includes('index="/bundle/print"'))
assert(layoutSource.includes('工序模板'))

assert(routerSource.includes("redirect: (to) => ({ path: '/style', query: to.query })"))

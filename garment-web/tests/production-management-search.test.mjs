import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const {
  filterProductionBeds,
  filterProcessTemplates
} = await import('../src/utils/productionManagementSearch.js')

const beds = [
  {
    id: 1,
    bedNo: 'BED-A01',
    name: 'Main Run',
    colors: 'Black / Navy',
    processes: [{ name: 'Cutting' }, { name: 'Sewing' }]
  },
  {
    id: 2,
    bedNo: '返工床-B',
    name: 'Repair',
    colors: 'White',
    processes: [{ name: 'Pressing' }]
  }
]

assert.deepEqual(filterProductionBeds(beds, '   ').map((bed) => bed.id), [1, 2])
assert.deepEqual(filterProductionBeds(beds, ' bed-a01 ').map((bed) => bed.id), [1])
assert.deepEqual(filterProductionBeds(beds, 'REPAIR').map((bed) => bed.id), [2])
assert.deepEqual(filterProductionBeds(beds, 'navy').map((bed) => bed.id), [1])
assert.deepEqual(filterProductionBeds(beds, 'PRESS').map((bed) => bed.id), [2])
assert.deepEqual(filterProductionBeds(beds, 'missing'), [])

const templates = [
  {
    id: 11,
    name: 'T-Shirt Standard',
    remark: 'Summer line',
    items: [{ name: 'Cutting' }, { name: 'Sewing' }]
  },
  {
    id: 12,
    name: 'Hoodie Heavy',
    remark: 'Fleece',
    items: [{ name: 'Pressing' }]
  }
]

assert.deepEqual(filterProcessTemplates(templates, '').map((item) => item.id), [11, 12])
assert.deepEqual(filterProcessTemplates(templates, ' t-SHIRT ').map((item) => item.id), [11])
assert.deepEqual(filterProcessTemplates(templates, 'FLEECE').map((item) => item.id), [12])
assert.deepEqual(filterProcessTemplates(templates, 'sew').map((item) => item.id), [11])
assert.deepEqual(filterProcessTemplates(templates, 'missing'), [])

const bundleSource = readFileSync(resolve(root, 'src/views/Bundle.vue'), 'utf8')
const templateSource = readFileSync(resolve(root, 'src/views/ProcessTemplate.vue'), 'utf8')
const bundleManageRule = bundleSource.match(/\.bundle-manage\s*\{[^}]*\}/s)?.[0] ?? ''
const templateManageRule = templateSource.match(/\.template-manage\s*\{[^}]*\}/s)?.[0] ?? ''

assert(
  bundleSource.includes('<div v-if="!selectedBedId" class="toolbar"'),
  'Bundle toolbar must be hidden when a bed is selected'
)
assert(bundleSource.includes(':placeholder="searchPlaceholder"'))
assert(bundleSource.includes('搜索款号'))
assert(bundleSource.includes('搜索床号、名称、颜色或工序'))
assert.match(
  bundleSource,
  /import\s+\{\s*filterProductionBeds\s*\}\s+from\s+['"]\.\.\/utils\/productionManagementSearch['"]/
)
assert.match(bundleSource, /filterProductionBeds\s*\(/)
assert.match(bundleManageRule, /width:\s*100%/)
assert.match(bundleManageRule, /min-width:\s*0/)
assert.doesNotMatch(bundleManageRule, /max-width:\s*1100px/)

assert(templateSource.includes('搜索模板、备注或工序'))
assert.match(
  templateSource,
  /import\s+\{\s*filterProcessTemplates\s*\}\s+from\s+['"]\.\.\/utils\/productionManagementSearch['"]/
)
assert.match(templateSource, /filterProcessTemplates\s*\(/)
assert.match(
  templateSource,
  /import\s+\{[^}]*getProcessTemplateDetail[^}]*\}\s+from\s+['"]\.\.\/api\/processTemplate['"]/
)
assert.match(templateSource, /getProcessTemplateDetail\s*\(/)
assert(templateSource.includes('templates.map(async (template) =>'))
assert(templateSource.includes('const detailRes = await getProcessTemplateDetail(template.id)'))
assert(templateSource.includes('items: detailRes.data?.items || []'))
assert(templateSource.includes('items: []'))
assert.match(templateManageRule, /width:\s*100%/)
assert.match(templateManageRule, /min-width:\s*0/)
assert.doesNotMatch(templateManageRule, /max-width:\s*1100px/)

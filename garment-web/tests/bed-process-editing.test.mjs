import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const bundleSource = readFileSync(resolve(root, 'src/views/Bundle.vue'), 'utf8')

assert(bundleSource.includes('编辑工序'))
assert(bundleSource.includes('processEditorVisible'))
assert(bundleSource.includes('processForm.rows'))
assert(bundleSource.includes(':disabled="row.locked"'))
assert(bundleSource.includes('locked: originalPrice > 0'))
assert(bundleSource.includes('hasWorkRecords: Number(process.completedQuantity || 0) > 0'))
assert(bundleSource.includes('row.hasWorkRecords'))
assert(bundleSource.includes('已有报工，不能删除'))
assert(bundleSource.includes('await updateBed(processForm.bedId'))
assert(bundleSource.includes('await reloadBeds()'))

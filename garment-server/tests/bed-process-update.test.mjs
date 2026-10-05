import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const source = readFileSync(resolve(root, 'src/main/java/com/garment/service/BedService.java'), 'utf8')

assert(source.includes('updatePendingPriceRecords(id, companyId, dto.getProcesses(), oldProcesses)'))
assert(source.includes('String previousName = oldProcess != null ? oldProcess.getName() : item.getName()'))
assert(source.includes('.eq(BundleProcess::getProcessName, previousName)'))
assert(source.includes('bp.setProcessName(item.getName())'))

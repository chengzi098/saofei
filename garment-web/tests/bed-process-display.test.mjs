import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const bundleSource = readFileSync(resolve(root, 'src/views/Bundle.vue'), 'utf8')

assert(bundleSource.includes('class="bed-processes"'))
assert(bundleSource.includes('v-for="process in bed.processes"'))
assert(bundleSource.includes('{{ process.name }}'))
assert(bundleSource.includes('暂无工序'))
assert(bundleSource.includes('.bed-process-list'))

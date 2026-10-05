# Production Management Layout and Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the production-management and process-template pages use the full available content width and provide search behavior that matches the current business context.

**Architecture:** Add one small pure utility module for bed and process-template filtering so behavior can be tested independently. Keep style search on the existing paginated API, filter already-loaded beds locally, and enrich process-template list records with existing detail requests before local filtering.

**Tech Stack:** Vue 3 Composition API, Element Plus, Node.js `assert`, Vite.

---

### Task 1: Add searchable-data utilities

**Files:**
- Create: `garment-web/src/utils/productionManagementSearch.js`
- Create: `garment-web/tests/production-management-search.test.mjs`

- [ ] **Step 1: Write the failing behavior test**

Create `garment-web/tests/production-management-search.test.mjs`:

```js
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
    bedNo: '1号床',
    name: '大货',
    colors: '黑色',
    processes: [{ name: '裁剪' }, { name: '缝纫' }]
  },
  {
    id: 2,
    bedNo: '返工床-A',
    name: '返工',
    colors: '白色',
    processes: [{ name: '整烫' }]
  }
]

assert.deepEqual(filterProductionBeds(beds, '').map((bed) => bed.id), [1, 2])
assert.deepEqual(filterProductionBeds(beds, ' 1号床 ').map((bed) => bed.id), [1])
assert.deepEqual(filterProductionBeds(beds, '返工').map((bed) => bed.id), [2])
assert.deepEqual(filterProductionBeds(beds, '黑色').map((bed) => bed.id), [1])
assert.deepEqual(filterProductionBeds(beds, '整烫').map((bed) => bed.id), [2])
assert.deepEqual(filterProductionBeds(beds, '不存在'), [])

const templates = [
  {
    id: 11,
    name: 'T恤通用',
    remark: '夏装',
    items: [{ name: '裁剪' }, { name: '缝纫' }]
  },
  {
    id: 12,
    name: '卫衣标准',
    remark: '加绒',
    items: [{ name: '整烫' }]
  }
]

assert.deepEqual(filterProcessTemplates(templates, '').map((item) => item.id), [11, 12])
assert.deepEqual(filterProcessTemplates(templates, 't恤').map((item) => item.id), [11])
assert.deepEqual(filterProcessTemplates(templates, '加绒').map((item) => item.id), [12])
assert.deepEqual(filterProcessTemplates(templates, '缝纫').map((item) => item.id), [11])
assert.deepEqual(filterProcessTemplates(templates, '不存在'), [])

const bundleSource = readFileSync(resolve(root, 'src/views/Bundle.vue'), 'utf8')
const templateSource = readFileSync(resolve(root, 'src/views/ProcessTemplate.vue'), 'utf8')

assert(bundleSource.includes('<div v-if="!selectedBedId" class="toolbar"'))
assert(bundleSource.includes(':placeholder="searchPlaceholder"'))
assert(bundleSource.includes('搜索款号'))
assert(bundleSource.includes('搜索床号、名称、颜色或工序'))
assert(bundleSource.includes('filterProductionBeds'))
assert(/\.bundle-manage\s*\{[^}]*width:\s*100%[^}]*min-width:\s*0/s.test(bundleSource))
assert(!/\.bundle-manage\s*\{[^}]*max-width:\s*1100px/s.test(bundleSource))

assert(templateSource.includes('搜索模板、备注或工序'))
assert(templateSource.includes('filterProcessTemplates'))
assert(templateSource.includes('getProcessTemplateDetail'))
assert(/\.template-manage\s*\{[^}]*width:\s*100%[^}]*min-width:\s*0/s.test(templateSource))
assert(!/\.template-manage\s*\{[^}]*max-width:\s*1100px/s.test(templateSource))
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
cd garment-web
node tests/production-management-search.test.mjs
```

Expected: FAIL with `ERR_MODULE_NOT_FOUND` for `productionManagementSearch.js`.

- [ ] **Step 3: Add the minimal pure filtering utility**

Create `garment-web/src/utils/productionManagementSearch.js`:

```js
function normalize(value) {
  return String(value ?? '').trim().toLowerCase()
}

function includesKeyword(value, keyword) {
  return normalize(value).includes(keyword)
}

export function filterProductionBeds(beds, keyword) {
  const normalizedKeyword = normalize(keyword)
  if (!normalizedKeyword) return beds || []

  return (beds || []).filter((bed) =>
    includesKeyword(bed.bedNo, normalizedKeyword) ||
    includesKeyword(bed.name, normalizedKeyword) ||
    includesKeyword(bed.colors, normalizedKeyword) ||
    (bed.processes || []).some((process) =>
      includesKeyword(process.name, normalizedKeyword)
    )
  )
}

export function filterProcessTemplates(templates, keyword) {
  const normalizedKeyword = normalize(keyword)
  if (!normalizedKeyword) return templates || []

  return (templates || []).filter((template) =>
    includesKeyword(template.name, normalizedKeyword) ||
    includesKeyword(template.remark, normalizedKeyword) ||
    (template.items || []).some((item) =>
      includesKeyword(item.name, normalizedKeyword)
    )
  )
}
```

- [ ] **Step 4: Run the test and verify the utility assertions pass**

Run:

```powershell
node tests/production-management-search.test.mjs
```

Expected: utility assertions pass; source assertions fail because pages are not updated yet.

- [ ] **Step 5: Commit the utility and test**

```powershell
git add garment-web/src/utils/productionManagementSearch.js garment-web/tests/production-management-search.test.mjs
git commit -m "test: define production search behavior"
```

### Task 2: Make production-management search context-aware

**Files:**
- Modify: `garment-web/src/views/Bundle.vue`
- Test: `garment-web/tests/production-management-search.test.mjs`

- [ ] **Step 1: Restrict the toolbar to style and bed levels**

Replace the toolbar opening element with:

```vue
<div v-if="!selectedBedId" class="toolbar" :class="{ 'entered': mounted }">
```

Replace the fixed placeholder with:

```vue
:placeholder="searchPlaceholder"
```

- [ ] **Step 2: Import the bed filter**

Add:

```js
import { filterProductionBeds } from '../utils/productionManagementSearch'
```

- [ ] **Step 3: Add the contextual placeholder**

After the search state declarations, add:

```js
const searchPlaceholder = computed(() =>
  selectedStyleId.value
    ? '搜索床号、名称、颜色或工序'
    : '搜索款号'
)
```

- [ ] **Step 4: Combine bed status and keyword filtering**

Replace `filteredBeds` with:

```js
const filteredBeds = computed(() => {
  let beds = bedList.value
  if (bedFilter.value === 'bundled') {
    beds = beds.filter((bed) => bed.bundleCount > 0)
  } else if (bedFilter.value === 'unbundled') {
    beds = beds.filter((bed) => !bed.bundleCount || bed.bundleCount === 0)
  }
  return filterProductionBeds(beds, searchKeyword.value)
})
```

- [ ] **Step 5: Make the search watcher level-aware**

Replace the watcher with:

```js
let searchTimer = null
watch(searchKeyword, () => {
  if (selectedStyleId.value) {
    bedPageNum.value = 1
    return
  }

  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pageNum.value = 1
    loadStyles()
  }, 300)
})
```

- [ ] **Step 6: Clear search in the correct hierarchy order**

In `selectStyle`, set the selected style before clearing the keyword:

```js
selectedStyleId.value = s.id
searchKeyword.value = ''
```

In `selectBed`, set the selected bed before clearing the keyword:

```js
selectedBedId.value = bed.id
searchKeyword.value = ''
```

In `backToStyle`, clear the selected style before clearing the keyword:

```js
selectedStyleId.value = null
searchKeyword.value = ''
```

In `backToBed`, clear the selected bed before clearing the keyword:

```js
selectedBedId.value = null
searchKeyword.value = ''
```

The order matters: returning to the style level must let the keyword watcher reload the unfiltered server-side style page, while entering a deeper level must not issue an unnecessary style request.

- [ ] **Step 7: Use the full available width**

Replace:

```css
.bundle-manage {
  max-width: 1100px;
}
```

with:

```css
.bundle-manage {
  width: 100%;
  min-width: 0;
}
```

- [ ] **Step 8: Run the targeted test**

Run:

```powershell
node tests/production-management-search.test.mjs
```

Expected: production-management source assertions pass; process-template assertions still fail.

- [ ] **Step 9: Run the existing workflow test**

Run:

```powershell
node tests/production-workflow.test.mjs
```

Expected: PASS.

- [ ] **Step 10: Commit the production-management page**

```powershell
git add garment-web/src/views/Bundle.vue
git commit -m "fix: align production search with workflow level"
```

### Task 3: Expand process-template search

**Files:**
- Modify: `garment-web/src/views/ProcessTemplate.vue`
- Test: `garment-web/tests/production-management-search.test.mjs`

- [ ] **Step 1: Update the search prompt**

Replace the placeholder with:

```vue
placeholder="搜索模板、备注或工序"
```

- [ ] **Step 2: Import the template filter**

Add:

```js
import { filterProcessTemplates } from '../utils/productionManagementSearch'
```

- [ ] **Step 3: Replace inline name-only filtering**

Replace `filteredList` with:

```js
const filteredList = computed(() =>
  filterProcessTemplates(list.value, searchKeyword.value)
)
```

- [ ] **Step 4: Enrich list records with template items**

Replace `loadList` with:

```js
async function loadList() {
  loading.value = true
  try {
    const res = await getProcessTemplateList()
    const templates = res.data || []
    const details = await Promise.all(
      templates.map(async (template) => {
        try {
          const detailRes = await getProcessTemplateDetail(template.id)
          return {
            ...template,
            items: detailRes.data?.items || []
          }
        } catch (e) {
          return {
            ...template,
            items: []
          }
        }
      })
    )
    list.value = details
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 5: Use the full available width**

Replace:

```css
.template-manage {
  max-width: 1100px;
}
```

with:

```css
.template-manage {
  width: 100%;
  min-width: 0;
}
```

- [ ] **Step 6: Run the targeted test and verify GREEN**

Run:

```powershell
node tests/production-management-search.test.mjs
```

Expected: PASS.

- [ ] **Step 7: Commit the process-template page**

```powershell
git add garment-web/src/views/ProcessTemplate.vue
git commit -m "fix: expand process template search"
```

### Task 4: Regression verification

**Files:**
- Modify only if verification exposes a defect.

- [ ] **Step 1: Run production search tests**

```powershell
cd garment-web
node tests/production-management-search.test.mjs
```

Expected: PASS.

- [ ] **Step 2: Run production workflow tests**

```powershell
node tests/production-workflow.test.mjs
```

Expected: PASS.

- [ ] **Step 3: Run the existing scanner suite**

```powershell
npm run test:scanner
```

Expected: PASS.

- [ ] **Step 4: Build the frontend**

```powershell
npm run build
```

Expected: Vite exits with code 0 and creates `garment-web/dist`.

- [ ] **Step 5: Inspect the scoped diff**

```powershell
git diff -- garment-web/src/views/Bundle.vue garment-web/src/views/ProcessTemplate.vue garment-web/src/utils/productionManagementSearch.js garment-web/tests/production-management-search.test.mjs
```

Expected: only layout and search changes described in the design.

- [ ] **Step 6: Commit any verification-only correction**

Only if a defect was found and corrected:

```powershell
git add garment-web/src/views/Bundle.vue garment-web/src/views/ProcessTemplate.vue garment-web/src/utils/productionManagementSearch.js garment-web/tests/production-management-search.test.mjs
git commit -m "fix: complete production search verification"
```

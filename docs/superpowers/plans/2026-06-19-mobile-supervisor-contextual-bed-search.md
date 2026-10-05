# Mobile Supervisor Contextual Bed Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the supervisor mobile work-board search styles or beds on the style-list page, then search only beds inside the selected style.

**Architecture:** Add a pure bed-list filtering helper beside the existing style search helpers. Keep separate list-page and detail-page search state in `MobileSupervisorBoard.vue`, expose one writable computed value to the existing input, and render a computed filtered bed list without mutating dashboard data.

**Tech Stack:** Vue 3 Composition API, Node.js strict assertions, Vite.

---

### Task 1: Add current-style bed filtering

**Files:**
- Modify: `garment-web/src/utils/dashboardBoardSearch.js`
- Modify: `garment-web/tests/dashboard-board-search.test.mjs`

- [x] **Step 1: Write the failing helper test**

Update the utility import:

```js
const {
  filterDashboardBeds,
  filterDashboardStyles,
  getMatchedBeds
} = await import('../src/utils/dashboardBoardSearch.js')
```

Add these assertions after the style fixtures:

```js
assert.deepEqual(filterDashboardBeds(styles[0].beds, '').map((bed) => bed.bedId), [11, 12])
assert.deepEqual(filterDashboardBeds(styles[0].beds, ' 返工床-a ').map((bed) => bed.bedId), [12])
assert.deepEqual(filterDashboardBeds(styles[0].beds, '不存在'), [])
```

- [x] **Step 2: Run the test and verify RED**

Run:

```powershell
node tests/dashboard-board-search.test.mjs
```

Working directory: `garment-web`

Expected: FAIL because `dashboardBoardSearch.js` does not export `filterDashboardBeds`.

- [x] **Step 3: Implement the minimal helper**

Add to `dashboardBoardSearch.js`:

```js
export function filterDashboardBeds(beds, keyword) {
  const normalizedKeyword = normalize(keyword)
  if (!normalizedKeyword) return beds || []

  return (beds || []).filter((bed) =>
    normalize(bed.bedNo).includes(normalizedKeyword)
  )
}
```

- [x] **Step 4: Run the helper test and verify GREEN**

Run:

```powershell
node tests/dashboard-board-search.test.mjs
```

Expected: exit code 0.

### Task 2: Switch mobile search behavior by page level

**Files:**
- Modify: `garment-web/src/views/mobile/MobileSupervisorBoard.vue`
- Modify: `garment-web/tests/mobile-supervisor-and-date.test.mjs`

- [x] **Step 1: Write failing component integration assertions**

Add assertions for the required wiring:

```js
assert(boardSource.includes('v-model="activeSearchKeyword"'))
assert(boardSource.includes(':placeholder="searchPlaceholder"'))
assert(boardSource.includes('const bedSearchKeyword = ref(\'\')'))
assert(boardSource.includes('const filteredBeds = computed'))
assert(boardSource.includes('v-for="(bed, index) in filteredBeds"'))
assert(boardSource.includes('没有匹配的床号'))
```

- [x] **Step 2: Run the test and verify RED**

Run:

```powershell
node tests/mobile-supervisor-and-date.test.mjs
```

Working directory: `garment-web`

Expected: FAIL on the first new source assertion because the input still binds directly to `searchKeyword`.

- [x] **Step 3: Add independent search state and computed results**

Import the helper:

```js
import {
  filterDashboardBeds,
  filterDashboardStyles,
  getMatchedBeds
} from '../../utils/dashboardBoardSearch'
```

After declaring `selectedStyle`, add:

```js
const bedSearchKeyword = ref('')

const activeSearchKeyword = computed({
  get: () => selectedStyle.value ? bedSearchKeyword.value : searchKeyword.value,
  set: (value) => {
    if (selectedStyle.value) {
      bedSearchKeyword.value = value
    } else {
      searchKeyword.value = value
    }
  }
})

const searchPlaceholder = computed(() =>
  selectedStyle.value ? '搜索床号...' : '搜索款号或床号...'
)

const filteredBeds = computed(() =>
  filterDashboardBeds(selectedStyle.value?.beds, bedSearchKeyword.value)
)

const filterCountText = computed(() =>
  selectedStyle.value
    ? `共 ${filteredBeds.value.length} 个床号`
    : `共 ${filteredStyles.value.length} 个款式`
)
```

Declare `filterCountText` after `filteredStyles` so its dependency is already defined.

- [x] **Step 4: Wire the template to contextual search**

Change the input to:

```vue
<el-input
  v-model="activeSearchKeyword"
  :placeholder="searchPlaceholder"
  clearable
  :prefix-icon="Search"
  size="default"
  class="search-input"
/>
```

Change the count to:

```vue
<span class="filter-count">{{ filterCountText }}</span>
```

Change the bed loop to:

```vue
v-for="(bed, index) in filteredBeds"
```

Render distinct empty states:

```vue
<div
  v-if="selectedStyle.beds?.length && !filteredBeds.length"
  class="empty-state small"
>
  <p>没有匹配的床号</p>
</div>

<div v-else-if="!selectedStyle.beds?.length" class="empty-state small">
  <p>暂无床号数据</p>
</div>
```

- [x] **Step 5: Reset only the detail search when entering a style**

Update `selectStyle`:

```js
function selectStyle(style) {
  bedSearchKeyword.value = ''
  selectedStyle.value = { ...style }
}
```

This preserves `searchKeyword` when returning to the style list while ensuring every newly opened style starts with all beds visible.

- [x] **Step 6: Run the component test and verify GREEN**

Run:

```powershell
node tests/mobile-supervisor-and-date.test.mjs
```

Expected: exit code 0.

### Task 3: Regression verification

**Files:**
- Verify only; no new production changes expected.

- [x] **Step 1: Run focused search tests**

Run from `garment-web`:

```powershell
node tests/dashboard-board-search.test.mjs
node tests/mobile-supervisor-and-date.test.mjs
```

Expected: both commands exit 0.

- [x] **Step 2: Run all frontend Node tests**

Run:

```powershell
Get-ChildItem tests -Filter *.test.mjs | ForEach-Object {
  node $_.FullName
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
```

Expected: exit code 0 with no assertion failures.

- [x] **Step 3: Build the frontend**

Run:

```powershell
npm run build
```

Expected: Vite production build exits with code 0.

- [x] **Step 4: Review changed files**

Run:

```powershell
git diff -- garment-web/src/utils/dashboardBoardSearch.js garment-web/src/views/mobile/MobileSupervisorBoard.vue garment-web/tests/dashboard-board-search.test.mjs garment-web/tests/mobile-supervisor-and-date.test.mjs
```

Expected: only the contextual bed-search behavior and tests are changed. Do not create a commit because this repository has no baseline commit and all project files are currently untracked.

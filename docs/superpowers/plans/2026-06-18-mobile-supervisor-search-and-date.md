# Mobile Supervisor Search and Date Picker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bed-number direct search to the supervisor mobile board and replace desktop date-range popups with native mobile date inputs.

**Architecture:** Reuse `dashboardBoardSearch.js` for mobile-board matching. Put date normalization and presets in `mobileDateRange.js`, then expose them through one `MobileDateRangePicker.vue` component used by employee records and supervisor review.

**Tech Stack:** Vue 3 Composition API, native HTML date inputs, Node assert tests, Vite.

---

### Task 1: Mobile date behavior

**Files:**
- Create: `garment-web/src/utils/mobileDateRange.js`
- Create: `garment-web/tests/mobile-supervisor-and-date.test.mjs`

- [ ] Test range normalization and today/recent-seven-days/week/month presets.
- [ ] Run the test and confirm module-not-found failure.
- [ ] Implement the pure date helper and re-run until green.

### Task 2: Native date component

**Files:**
- Create: `garment-web/src/components/mobile/MobileDateRangePicker.vue`
- Modify: `garment-web/tests/mobile-supervisor-and-date.test.mjs`

- [ ] Assert two native `type="date"` inputs, minimum-date binding, and quick presets.
- [ ] Run the test and confirm failure.
- [ ] Implement the component with `v-model` and `change` events.

### Task 3: Date integration

**Files:**
- Modify: `garment-web/src/views/mobile/MobileRecords.vue`
- Modify: `garment-web/src/views/mobile/MobileSupervisorReview.vue`

- [ ] Replace `el-date-picker` in employee records.
- [ ] Add the same picker to supervisor review's custom tab.
- [ ] Load records after a complete range is selected.

### Task 4: Supervisor mobile bed search

**Files:**
- Modify: `garment-web/src/views/mobile/MobileSupervisorBoard.vue`
- Modify: `garment-web/tests/mobile-supervisor-and-date.test.mjs`

- [ ] Assert the new search placeholder, matched-bed label, and direct-open function.
- [ ] Run the test and confirm failure.
- [ ] Integrate shared style/bed filtering and matched-bed buttons.

### Task 5: Verify

- [ ] Run the new test.
- [ ] Run dashboard search, production workflow, and scanner tests.
- [ ] Run `npm run build`.

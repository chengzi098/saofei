# Dashboard Bed Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the PC work board search by style number or bed number and open a matched bed directly.

**Architecture:** Put pure matching behavior in `dashboardBoardSearch.js` so style and bed filtering can be tested without mounting Vue. `DashboardBoard.vue` uses the helper to filter style cards, renders matched-bed buttons, and reuses the existing `selectStyle` and `selectBed` navigation functions.

**Tech Stack:** Vue 3 Composition API, Node assert tests, Vite.

---

### Task 1: Search matching helper

**Files:**
- Create: `garment-web/src/utils/dashboardBoardSearch.js`
- Create: `garment-web/tests/dashboard-board-search.test.mjs`

- [ ] Test style-number matching, bed-number matching, no-match behavior, and multiple matched beds.
- [ ] Run `node tests/dashboard-board-search.test.mjs` and confirm module-not-found failure.
- [ ] Implement `getMatchedBeds` and `filterDashboardStyles`.
- [ ] Re-run the test and confirm it passes.

### Task 2: Work-board integration

**Files:**
- Modify: `garment-web/src/views/DashboardBoard.vue`
- Modify: `garment-web/tests/dashboard-board-search.test.mjs`

- [ ] Add source assertions for “搜索款号或床号”, “匹配床号”, and `selectMatchedBed`.
- [ ] Run the test and confirm it fails.
- [ ] Integrate helper filtering and matched-bed buttons.
- [ ] Clicking a matched bed must call `selectStyle(style)` then `selectBed(bed)`.
- [ ] Run the search test, existing tests, and `npm run build`.

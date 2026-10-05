# Production Management Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build one local production-management workbench for the style → bed → bundle → print workflow.

**Architecture:** Reuse `Bundle.vue` as the workflow container because it already owns style selection, bed selection, and bundle editing. Add focused dialogs for creating styles and beds, and extract printing into a drawer component so printing does not navigate away. Keep legacy routes as redirects/compatibility entries.

**Tech Stack:** Vue 3 Composition API, Vue Router, Element Plus, qrcode.vue, Node-based source tests, Vite.

---

### Task 1: Define workflow behavior

**Files:**
- Create: `garment-web/src/utils/productionWorkflow.js`
- Create: `garment-web/tests/production-workflow.test.mjs`

- [ ] Write tests for four workflow stages and next-stage selection.
- [ ] Run `node tests/production-workflow.test.mjs` and confirm failure because the helper does not exist.
- [ ] Implement the minimal workflow helper.
- [ ] Re-run the test and confirm it passes.

### Task 2: Add in-place printing

**Files:**
- Create: `garment-web/src/components/BundlePrintDrawer.vue`
- Modify: `garment-web/src/views/Bundle.vue`

- [ ] Extend the workflow test with source assertions for the print drawer integration and confirm failure.
- [ ] Add a drawer that loads labels by current bed ID and supports one/all printing.
- [ ] Replace `router.push()` printing with opening the drawer.
- [ ] Re-run the workflow test.

### Task 3: Add style and bed creation to the workbench

**Files:**
- Modify: `garment-web/src/views/Bundle.vue`

- [ ] Extend the source test for “新增款式” and “新增床号” controls and confirm failure.
- [ ] Add style creation dialog using `addStyle`.
- [ ] Select the returned style and load its bed list after save.
- [ ] Add bed creation dialog using `addBed`, including process template import.
- [ ] Select the returned bed and enter bundle editing after save.
- [ ] Re-run the workflow test.

### Task 4: Simplify navigation

**Files:**
- Modify: `garment-web/src/views/layout/Layout.vue`
- Modify: `garment-web/src/router/index.js`

- [ ] Extend the source test to require one production entry and legacy route compatibility; confirm failure.
- [ ] Point `/style` to the unified workbench.
- [ ] Keep `/bundle` as a redirect to `/style`.
- [ ] Remove separate bundle and print menu entries while preserving the process-template entry.
- [ ] Re-run the workflow test.

### Task 5: Verify

**Files:**
- Modify only if verification exposes a defect.

- [ ] Run `npm run test:scanner`.
- [ ] Run `node tests/production-workflow.test.mjs`.
- [ ] Run `npm run build`.
- [ ] Inspect local diffs and confirm no server/deployment files changed.

### Task 6: Add an obvious back button

**Files:**
- Modify: `garment-web/src/views/Bundle.vue`
- Modify: `garment-web/tests/production-workflow.test.mjs`

- [ ] Add source assertions for the labels `返回款式列表` and `返回床号列表`, plus the `level-back-button` style hook.
- [ ] Run `node tests/production-workflow.test.mjs` and confirm it fails.
- [ ] Replace the subtle breadcrumb-only navigation with a prominent button that calls `backToStyle` or `backToBed` according to the current level.
- [ ] Run the workflow test and `npm run build`.

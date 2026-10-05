# Bed Process Editing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow bosses to edit zero-priced processes from each bed card in production management.

**Architecture:** Extend the existing `Bundle.vue` view with a focused process editor dialog. Reuse the existing bed update API and server-side price locking rules, then reload beds after save.

**Tech Stack:** Vue 3, Element Plus, Axios, Node assertion tests, Vite

---

### Task 1: Add regression coverage

**Files:**
- Create: `garment-web/tests/bed-process-editing.test.mjs`

- [ ] Add source assertions for the edit entry, process rows, zero-price edit rule, locked controls, and `updateBed`.
- [ ] Run `node tests/bed-process-editing.test.mjs` and verify it fails because the editor is absent.

### Task 2: Implement the process editor

**Files:**
- Modify: `garment-web/src/views/Bundle.vue`
- Modify: `garment-web/src/api/bed.js` only if the existing update export is unavailable.

- [ ] Add an “编辑工序” command to each bed card.
- [ ] Add dialog state and copy the selected bed processes into an editable form.
- [ ] Disable fields and row operations when the original price is greater than 0 or the row is locked.
- [ ] Submit the complete ordered process list through `updateBed`.
- [ ] Reload the selected style’s beds after saving.
- [ ] Add compact dialog styles consistent with the current production page.

### Task 3: Verify and deploy

**Files:**
- Build output: `garment-web/dist/`
- Archive: `deploy/garment-frontend-20260619.zip`

- [ ] Run the focused regression test.
- [ ] Run every `garment-web/tests/*.test.mjs`.
- [ ] Run `npm run build`.
- [ ] Package `dist` with forward-slash ZIP entries.
- [ ] Deploy with the existing SSH frontend deployment helper.
- [ ] Compare online and local entry/resource SHA256 hashes.

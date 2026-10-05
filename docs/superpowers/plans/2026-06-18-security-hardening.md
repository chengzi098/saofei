# System Security Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve the 100-year JWT lifetime while enforcing company roles, securing the local print agent, removing exposed secrets and runtime QR script injection, and preventing sensitive artifacts from entering Git.

**Architecture:** Add one backend authorization service backed by `user_company`, expose it to Spring method security, and retain service-level ownership checks. Extract print-agent validation into testable pure functions, bind the service to localhost, authenticate protected routes, and replace shell interpolation with argument-based child processes. Keep the existing frontend authentication model while removing remote QR script injection.

**Tech Stack:** Java 21, Spring Boot 3.2, Spring Security, JUnit 5, Vue 3, Vite, Node.js, Express.

---

### Task 1: Central company-role authorization

**Files:**
- Create: `garment-server/src/main/java/com/garment/security/CompanyAuthorization.java`
- Create: `garment-server/src/test/java/com/garment/security/CompanyAuthorizationTest.java`
- Modify: `garment-server/src/main/java/com/garment/config/SecurityConfig.java`
- Modify: `garment-server/src/main/java/com/garment/common/GlobalExceptionHandler.java`

- [ ] Write tests proving boss, supervisor, worker, and super-admin penetration decisions.
- [ ] Run `mvn -Dtest=CompanyAuthorizationTest test` and verify the tests fail because the component does not exist.
- [ ] Implement role lookup using `user_id`, `company_id`, active status, and logical deletion.
- [ ] Enable method security and map authorization failures to HTTP 403.
- [ ] Run the focused test and confirm it passes.

### Task 2: Protect controllers by role

**Files:**
- Modify controllers under `garment-server/src/main/java/com/garment/controller/`
- Create: `garment-server/src/test/java/com/garment/security/ControllerAuthorizationRulesTest.java`

- [ ] Write a source-level authorization contract test for management, production, personal-record, wage-adjustment, and wage-settlement endpoints.
- [ ] Run the focused test and verify it fails on currently unprotected methods.
- [ ] Add `@PreAuthorize` rules using the central authorization component.
- [ ] Ensure wage adjustment creation permits boss/supervisor, approval and rejection permit boss only, and `/my` permits the current company member.
- [ ] Ensure worker-facing list services force the current worker ID.
- [ ] Run focused and full backend tests.

### Task 3: Remove committed secret defaults

**Files:**
- Modify: `garment-server/src/main/resources/application.yml`
- Modify: `.env.example`
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Modify deployment documentation and scripts containing exposed credentials
- Create: `garment-server/src/test/java/com/garment/config/SensitiveConfigurationTest.java`

- [ ] Write a test that rejects known passwords and fixed JWT defaults while asserting JWT expiration remains `3153600000000`.
- [ ] Run it and verify it fails against current files.
- [ ] Require `DB_PASSWORD` and `JWT_SECRET` environment variables.
- [ ] Replace documentation credentials with placeholders and environment-variable commands.
- [ ] Run the focused test.

### Task 4: Secure the print agent

**Files:**
- Create: `print-agent/security.js`
- Create: `print-agent/tests/security.test.mjs`
- Modify: `print-agent/index.js`
- Modify: `print-agent/package.json`
- Modify: `print-agent/README.md`
- Modify: `print-agent/start.bat`

- [ ] Write tests for origin allowlisting, API-key validation, printer-name matching, port validation, and payload limits.
- [ ] Run `npm test` in `print-agent` and verify failure before implementation.
- [ ] Implement pure validation helpers.
- [ ] Require `PRINT_AGENT_KEY`, bind to `127.0.0.1`, authenticate protected routes, and limit CORS.
- [ ] Replace interpolated shell commands with `execFile`/`spawn` arguments and validate the selected printer and port.
- [ ] Run print-agent tests and syntax check.

### Task 5: Remove runtime QR script injection

**Files:**
- Modify: `garment-web/src/views/QrPrint.vue`
- Modify: `garment-web/src/components/BundlePrintDrawer.vue`
- Create: `garment-web/tests/qr-render-security.test.mjs`
- Modify: `garment-web/package.json`

- [ ] Write a test rejecting CDN QR scripts and QR-related `innerHTML`.
- [ ] Run it and verify failure.
- [ ] Render QR output using the installed `qrcode.vue`/local dependency and DOM-safe APIs.
- [ ] Run the focused test, all frontend tests, and production build.

### Task 6: Protect local and deployment artifacts

**Files:**
- Modify: `.gitignore`
- Create: `docs/security-deployment-checklist.md`

- [ ] Add ignore rules for backups, deployment archives/uploads, drivers, installers, local print-agent secrets, and build outputs.
- [ ] Document required production secret rotation and print-agent key setup without recording secret values.
- [ ] Verify with `git status --short --ignored` that sensitive local artifacts are ignored.

### Task 7: Final verification

- [ ] Run `mvn test` in `garment-server`.
- [ ] Run every frontend test and `npm run build` in `garment-web`.
- [ ] Run `npm test` and `node --check index.js` in `print-agent`.
- [ ] Scan tracked source/docs for known exposed credentials, unrestricted print-agent CORS, QR CDN scripts, and QR `innerHTML`.
- [ ] Confirm `jwt.expiration` is still `3153600000000`.
- [ ] Review `git diff` and report any operational actions that still require manual production secret rotation.

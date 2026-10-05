# Database Schema Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make fresh and existing databases match the fields required by the current Java application, then safely migrate production.

**Architecture:** Treat entity mappings as the application contract. Keep `schema.sql` as the canonical final-state schema and `migration.sql` as an idempotent, non-destructive upgrade path for existing databases.

**Tech Stack:** MySQL 8, Spring Boot 3, MyBatis-Plus, JUnit 5, PowerShell, OpenSSH

---

### Task 1: Audit all schema sources

**Files:**
- Inspect: `garment-server/src/main/java/com/garment/entity/*.java`
- Inspect: `garment-server/src/main/resources/db/schema.sql`
- Inspect: `garment-server/src/main/resources/db/migration.sql`

- [ ] Export local and production `information_schema.columns`.
- [ ] Compare required entity fields against both databases and SQL scripts.
- [ ] Record only differences that affect current application reads, writes, defaults, or nullability.

### Task 2: Add structure contract tests

**Files:**
- Modify: `garment-server/src/test/java/com/garment/db/ProcessTemplateItemSchemaTest.java`

- [ ] Add assertions for the corrected notification, notification-read, process-template-item, and bed definitions.
- [ ] Run `mvn -Dtest=ProcessTemplateItemSchemaTest test`.
- [ ] Confirm the test fails against the current incomplete schema.

### Task 3: Correct fresh-install schema and existing-database migration

**Files:**
- Modify: `garment-server/src/main/resources/db/schema.sql`
- Modify: `garment-server/src/main/resources/db/migration.sql`
- Modify: `deploy/upload/schema.sql`
- Modify: `deploy/upload/migration.sql`

- [ ] Make fresh-install table definitions match current entity mappings.
- [ ] Add idempotent migration checks for every required production difference.
- [ ] Backfill nullable data before applying stricter nullability.
- [ ] Keep source and deployment copies byte-identical.

### Task 4: Verify scripts locally

- [ ] Run the JUnit suite with `mvn test`.
- [ ] Create a temporary MySQL database and import `schema.sql`.
- [ ] Execute `migration.sql` twice against the temporary database.
- [ ] Compare the resulting critical columns against the application contract.

### Task 5: Migrate production safely

- [ ] Query production structure before mutation.
- [ ] Create a timestamped `mysqldump` backup under `/opt/garment/backup`.
- [ ] Upload the verified migration script.
- [ ] Execute the migration once and stop on any SQL error.
- [ ] Verify critical production columns and run a representative template-item query.
- [ ] Check `garment` service status and recent logs.

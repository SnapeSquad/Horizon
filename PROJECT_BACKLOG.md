# Horizon Execution Backlog

Last updated: 2026-03-06

## Goals
- Stabilize build pipeline for active modules (`api-server`, `launcher-java`, `admin-panel`).
- Remove cross-module API contract drift.
- Ship a reproducible release candidate with minimal quality gates.

## Priority Rules
- P0: Blocks core flows or build/release.
- P1: Major feature gaps/risk but not immediate build blocker.
- P2: Improvements and polish.

## P0
- [x] `admin-panel` TypeScript build is green.
  - Scope: fix type errors in UI components and remove unused imports/state.
  - DoD: `npm run build` succeeds in `admin-panel`.
- [x] Payment link endpoint contract aligned.
  - Scope: backend supports endpoint expected by launcher (`/api/payment/generate`) or launcher switched to existing endpoint.
  - DoD: launcher call path and server route match, endpoint returns `paymentUrl`.
- [x] Define runtime write safety for user DB migrations.
  - Scope: safe `ALTER TABLE users ADD COLUMN role` with no crash if already exists.
  - DoD: server boot handles both fresh and existing DB.

## P1
- [x] Forum API returns author role data.
  - Scope: include role for topics/posts responses.
  - DoD: JSON payload includes role fields and Java client can parse/use.
- [x] Store preview TODO in Java launcher closed.
  - Scope: implement accessory preview loading flow in `StoreController`.
  - DoD: preview action applies model/texture path flow without placeholder TODO.
- [x] Document single source of truth for launcher target.
  - Scope: explicit status for `launcher-java` vs legacy electron launcher paths.
  - DoD: README + spec updated consistently.

## P2
- [x] Add smoke tests (API auth + store/forum minimal paths).
- [x] Add CI workflow for `admin-panel build` and `api-server` lint/check.
- [x] Cleanup repository hygiene (`node_modules`, `target`, runtime artifacts in git).

## Execution Log
- 2026-03-06: Backlog initialized.
- 2026-03-06: Fixed `admin-panel` TypeScript build blockers (`GlassPanel` props + unused variables/imports).
- 2026-03-06: Implemented `POST /api/payment/generate` and safe `users.role` migration.
- 2026-03-06: Added `author_role` in forum API (`/api/forum/topics`, `/api/forum/posts`).
- 2026-03-06: Implemented store accessory preview loading in `StoreController` (model + texture async load).
- 2026-03-06: Verification green: `admin-panel npm run build`, `api-server node --check`, `launcher-java mvn -DskipTests compile`.
- 2026-03-06: Updated docs to define `launcher-java` as primary target and marked legacy electron path.
- 2026-03-06: Added GitHub Actions CI workflow for API syntax check, admin-panel build, and launcher-java compile.
- 2026-03-06: Added `api-server/smoke-test.js` and wired it into `npm test` + CI.
- 2026-03-06: Added root `.gitignore` as first step of repository hygiene cleanup.
- 2026-03-06: Removed tracked generated/runtime artifacts from git index (`node_modules`, `target`, `dist`, `.env`) and deleted local copies.
- 2026-03-06: Reinstalled active dependencies (`api-server`, `admin-panel`) and re-verified builds/tests.
- 2026-03-06: Extended API smoke to include auth register/login flow.
- 2026-03-06: Added `api-server/.env.example` and synced `api-server/README.md` with actual endpoints/scripts.
- 2026-03-06: Added `scripts/bootstrap.ps1` and `scripts/verify.ps1` for repeatable setup/verification.
- 2026-03-06: Downgraded missing Telegram token startup message from error to warning for cleaner local/CI runs.
- 2026-03-06: Added root npm orchestration scripts (`setup`, `verify`, `dev:*`) and dev process management scripts (`dev-all.ps1`, `stop-dev.ps1`).

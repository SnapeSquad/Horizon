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
- [x] Dependency security baseline hardened for active Node modules.
  - Scope: remove vulnerable Telegram SDK chain, upgrade lint/build deps, apply safe npm overrides for sqlite toolchain.
  - DoD: `npm audit` reports `0 vulnerabilities` in `api-server` and `admin-panel`.

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
- [x] Replace admin-panel mocks with live API integration.
  - Scope: wire auth/2FA + admin users/bans/news/cosmetics to `api-server`.
  - DoD: admin-panel performs real CRUD/actions against backend contracts.
- [x] Close admin access bootstrap and role-management flow.
  - Scope: bootstrap admin account from `.env`, enforce admin access check in panel auth, allow role edits from admin UI/API.
  - DoD: JWT admin login works with bootstrap account; role update endpoint + UI are covered by smoke checks.
- [x] Harden local dev orchestration (`dev:all`) and diagnostics.
  - Scope: add preflight checks, process status/log visibility, and reliable process-tree stop.
  - DoD: `npm run dev:all`, `npm run dev:status`, `npm run dev:stop`, `npm run doctor` provide deterministic local workflow.

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
- 2026-03-06: Extended smoke tests with forum topic/post creation and `author_role` contract assertions.
- 2026-03-06: Replaced `node-telegram-bot-api` with in-repo Telegram Bot API client (`utils/telegramBotClient.js`) preserving polling/message flow.
- 2026-03-06: Upgraded Node dependencies (`bcrypt@6`, `@typescript-eslint/*@8.56.1`) and added `api-server` overrides to patched `node-gyp/tar` chain.
- 2026-03-06: Added root quality scripts `audit:*` and `verify:full`; confirmed `npm run verify:full` is green.
- 2026-03-06: Reworked `admin-panel` auth flow (login/2FA + optional admin token) and persisted session handling.
- 2026-03-06: Connected admin-panel tabs to live API (`/api/admin/users|bans|news|cosmetics`) and enabled real actions (ban/unban, give currency, upload/delete cosmetics, create/delete news).
- 2026-03-06: Added `DELETE /api/admin/cosmetics/:id` backend endpoint and expanded smoke checks for admin contracts.
- 2026-03-06: Hardened verification script (`scripts/verify.ps1`) to fail on external command non-zero exit codes.
- 2026-03-06: Added admin bootstrap from `.env` (`ADMIN_USERNAME` / `ADMIN_PASSWORD`) and role update API (`PATCH /api/admin/users/:id/role`).
- 2026-03-06: Added role management in admin-panel moderation table and enforced admin access check after login/2FA.
- 2026-03-06: Extended smoke tests with admin role change contract checks.
- 2026-03-06: Reworked dev orchestration scripts with preflight checks, log files, process status command, doctor diagnostics, and tree-based stop.

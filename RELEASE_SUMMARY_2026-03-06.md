# Release Summary (2026-03-06)

## Scope
This release delivers a clean, reproducible baseline for Horizon with stabilized builds, aligned API contracts, and repository hygiene fixes.

## Key Changes
- Stabilized `admin-panel` TypeScript build and fixed UI prop typing issues.
- Added API contract endpoint: `POST /api/payment/generate`.
- Added safe DB migration path for `users.role`.
- Extended forum API responses with `author_role`.
- Implemented accessory preview loading flow in Java launcher store.
- Added API smoke tests (auth + cosmetics + payment + forum categories).
- Extended API smoke tests with forum topic/post flows and `author_role` response checks.
- Added CI workflow for API checks, admin-panel build, and Java compile.
- Added bootstrap/verify scripts:
  - `scripts/bootstrap.ps1`
  - `scripts/verify.ps1`
- Added `api-server/.env.example` and synced docs with actual endpoints.
- Removed generated/runtime artifacts from version control (`node_modules`, `target`, `dist`, `.env`, Gradle caches).
- Replaced `node-telegram-bot-api` with a local Telegram Bot API polling client (`api-server/utils/telegramBotClient.js`).
- Upgraded Node dependency baseline (`bcrypt@6`, `@typescript-eslint/*@8.56.1`, `vite@7`) and hardened `sqlite3` toolchain via npm `overrides`.
- Added root audit scripts (`audit:api`, `audit:admin`, `audit:all`) and one-command full gate (`verify:full`).
- Achieved clean security baseline for active modules: `npm audit` = `0 vulnerabilities` in `api-server` and `admin-panel`.

## Verification Command
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify.ps1
# full check (build/test + security audit)
npm run verify:full
```

## Notes
- Telegram integration is optional in local/test mode.
- In test mode (`NODE_ENV=test` or `HORIZON_SILENT_TELEGRAM=1`), Telegram missing-token warnings are suppressed for cleaner CI logs.

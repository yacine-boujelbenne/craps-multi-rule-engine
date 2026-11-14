# AGENTS.md: rules for every coding agent (Codex, OpenCode)

Read this file and `docs/00-context.md` before any task. Each Auto Run task starts in a fresh session, so never assume memory of earlier tasks. Check the repo (`git log`, existing files) to see what is already done.

## Owner execution rules — 2026-11-14

Read and apply these rules before every task. Correctness > minimal changes > token efficiency > additional features. These owner instructions override conflicting older guidance below and in other project documents.

- Keep reasoning, explanations and responses concise. Avoid repetition, obvious implementation explanations, tutorials and unnecessary planning. Ask only when essential information is missing.
- Implement exactly the requested scope. Reuse existing functions, dependencies and architecture; make small patches without unrelated refactors, optional features or abstractions.
- Do not create Markdown files unless explicitly requested. Do not generate README/CHANGELOG/TODO/SUMMARY/REPORT files or implementation notes. Prefer modifying existing files; create temporary scripts, examples, demonstrations or sample files only when strictly necessary. No backups or duplicate implementations.
- Inspect only relevant files with targeted searches and short output. Avoid rereading unchanged files or scanning dependencies, generated directories, build outputs or virtual environments unless necessary.
- Do not create new test files unless explicitly requested. Prefer targeted validation; do not run the full suite for small isolated changes or repeat unchanged checks. Run expensive integration, Docker, browser or end-to-end tests only when necessary or explicitly requested. Never claim unexecuted checks passed.
- Final responses contain only files modified, a brief description (1–3 sentences), test status when applicable, and important errors/blockers. Omit lengthy explanations, code dumps and step-by-step summaries unless requested.
- Use only `/run/media/yacine/yassine/DMS/export-docs-platform`. No copies or additional worktrees. Agents share the current branch; coordinate branch switches, staging, commits and integration to preserve other agents' work.
- Agent 1 owns architecture, schema/migrations, API contracts, task boundaries and acceptance review. Agent 2 implements backend services/APIs against those contracts. Coordinate shared-file changes and report contract gaps instead of inventing business rules.

## Project
Export documentation platform for seafood shipments. Exactly one shipment is one **Project**; its documents (invoice, packing list, catch certificates, BL, health certificate, certificate of origin, processing statement) are forms linked through one source of truth. Data enters by manual forms or by import (PDF via OCR, Excel/Word/CSV). MVP generates HC, draft BL, CC, CO and Shipping List. Generate two variants: an unsigned ready-for-real-signing copy and a prominently watermarked CLIENT PREVIEW NOT VALID FOR OFFICIAL USE showing authorized sample signature placement. Manual entry first; OCR later. Seal and lock are separate identifiers, with lock added late.

## Stack and layout
- `backend/`: Java 21, Spring Boot 3, Spring Data JPA, Flyway, PostgreSQL 16, Maven. Base package `com.exportdocs`.
- `frontend/`: Angular (latest stable), standalone components, signals, Reactive Forms, Angular Material.
- `docs/`: specs. `docs/01-field-ownership.md` is the contract for which document owns which field.
- Database is a single PostgreSQL (local via `docker compose up -d db`; Supabase may host it in cloud). No second database.

## Backend conventions
- Packages: `domain`, `application`, `infrastructure`, `api`. Controllers never touch repositories directly.
- Schema changes only through Flyway migrations in `backend/src/main/resources/db/migration` (`V<n>__<name>.sql`). Never edit an applied migration.
- DTOs via MapStruct. Validation via Bean Validation plus the validation engine (`application.validation`).
- Every write to a business entity creates an `audit_log` row.
- Tests: JUnit 5, Testcontainers for PostgreSQL. Write the failing test first for rules, mappings, validators.

## Frontend conventions
- Feature folders under `src/app/features/<feature>`. Typed API client generated from the backend OpenAPI spec (`npm run gen:api`).
- Inherited fields are read-only and show a source badge. Never let a form silently overwrite a field owned by another document.
- i18n FR/EN with Angular i18n or ngx-translate (decide once in Phase 0, then stay consistent).

## Workspace rule — owner update, 2026-11-12
Use one physical checkout: `/run/media/yacine/yassine/DMS/export-docs-platform`. Keep backend, frontend, docs and infra together here. Use isolated Git branches and coordinate branch switches; do not create additional worktree folders or duplicate project copies. Historical paths in verification reports describe past runs, not active checkouts.

## Git rules
- Work on a branch named `feat/<area>-<short-name>`. Small commits, conventional messages (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
- Never commit secrets, `.env`, uploaded documents, or real customer data.
- Run backend `./mvnw verify` and frontend `npm test -- --watch=false` before marking a task done. If a check fails, fix it before ticking the box.

## Auto Run rules
- Do exactly one unchecked task per session, then tick its box (`- [x]`) and add a one-line result note under it.
- If a task is blocked by a missing decision, do NOT guess: write the question in `docs/OPEN-QUESTIONS.md`, KEEP THE BOX UNCHECKED, label it `BLOCKED` below the item, and stop that playbook. Only `[x]` means implemented, tested and accepted.
- Do not modify files outside the task's stated scope.

## Master architecture and UI specification
Read `docs/MASTER-PLAN.md` for milestones, business and security constraints. Treat unresolved P0 questions as hard decisions, not blanks to guess. Lucent/Uberdesigner is only a reference for selective free animations/styles, not a template. Angular only, no React. Do not fabricate external authority seals or carrier originals.

## 2026-11-11 confirmed business rules
- Raw catch input (~22,000 kg *sample*) differs from finished net export (~19,200 kg *sample*); model processing yield and box-size-grade allocations explicitly.
- Shipping List is separate from packing list and is an HC annex tied to vessel/lot records (sample dossier pages 16,17,22).
- ZERO signature or stamp graphics in any signing-ready PDF on any page. Client preview has conspicuous non-official markings and only illustrative signature locations. Do not mimic government-issued or carrier-signed approvals.

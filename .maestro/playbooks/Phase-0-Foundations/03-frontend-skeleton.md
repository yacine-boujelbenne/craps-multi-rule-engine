# Phase 0.3: Angular skeleton
**Agent:** frontend (OpenCode) | **Branch:** feat/frontend-skeleton
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Create an Angular app in `frontend/` (latest stable, standalone, routing, SCSS, strict mode) and add Angular Material.
- [x] Add i18n with FR and EN (use `@ngx-translate/core` with JSON files in `src/assets/i18n`). Default language FR. Write a unit test that the language switcher changes a translated label.
  - Result: `@ngx-translate/core`/`http-loader` v18 wired in `app.config.ts` (lang `fr`, fallback `en`, loader `./assets/i18n/`); `src/assets/i18n/{fr,en}.json` + `src/assets` asset entry; `LanguageSwitcherComponent` in the topbar; shell (sidebar/topbar) labels translated; `language-switcher.component.spec.ts` proves the label switches FR→EN. 78 tests pass, build clean.
- [ ] Create the app shell: top bar, side navigation (Dashboard, Projects, Master data, Admin), `features/` folders with lazy-loaded routes, and a 404 page.
- [ ] Add `npm run gen:api` that generates a typed client from the backend OpenAPI URL (`http://localhost:8080/v3/api-docs`) using `openapi-typescript-codegen` or `@openapitools/openapi-generator-cli`; document usage in `frontend/README.md`.
- [ ] Run `npm test -- --watch=false` and `npm run build`, then commit `feat: frontend skeleton`.

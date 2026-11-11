# ExportDocs — Frontend

Angular front end for the seafood export documentation platform. This milestone
delivers the application shell, dashboard, project creation, project listing and
the project workspace (with document-step placeholders).

## Stack

- Angular 22 (standalone components, signals, zoneless)
- TypeScript (strict)
- Angular Reactive Forms
- Angular Material 3 (thin custom theme) + Angular CDK
- SCSS with a CSS custom-property token layer
- Vitest (via `@angular/build:unit-test`)

## Commands

```bash
npm install
npm start          # dev server on http://localhost:4200
npm run build      # production build
npm test           # unit tests (watch)
npm run test:ci    # unit tests (single run)
npm run format     # prettier
npm run gen:api    # regenerate the typed client from the backend OpenAPI spec
```

## Folder structure

```
src/
  app/
    core/            cross-cutting, framework-light code
      config/        runtime configuration (API base URL, mock toggle)
      data/          anonymized mock fixtures
      models/        typed domain interfaces + status metadata
      services/      ProjectService contract, mock + HTTP impls, store, notifications
      utils/         formatting helpers
    layout/          app shell, sidebar, topbar
    shared/          reusable presentational components (icon, panel, badges…)
    features/
      dashboard/
      projects/      project-list, project-form, project-details
      not-found/
  styles/            design tokens, mixins, shared component classes
  styles.scss        global styles + Angular Material theme
```

Feature folders follow `AGENTS.md` (`src/app/features/<feature>`).

## API integration

`core/services/project.service.ts` defines the transport-agnostic `ProjectService`
contract and a `PROJECT_SERVICE` injection token. Endpoints follow the
representative API in `docs/MASTER-PLAN.md` §10:

| Operation | HTTP |
|---|---|
| List projects | `GET /api/projects` |
| Get project | `GET /api/projects/{id}` |
| Create project | `POST /api/projects` |
| Update project | `PATCH /api/projects/{id}` |

Two implementations exist:

- `ProjectMockService` — isolated in-memory implementation seeded from
  anonymized fixtures (`core/data/mock-projects.ts`). It persists to
  `localStorage` so created projects survive a reload. **This is active by default.**
- `ProjectApiService` — real `HttpClient` implementation for the endpoints above.

Switch between them in `core/config/app-config.ts`:

```ts
export const APP_CONFIG = {
  apiBaseUrl: '/api',
  useMockApi: true, // set to false once the backend contract is frozen
};
```

When the backend publishes its OpenAPI document, run `npm run gen:api` to
generate typed schemas into `src/app/core/api/schema.d.ts` and map them onto the
interfaces in `core/models`.

## Design system

Tokens live in `src/styles/_tokens.scss` (Lucent-inspired: `#FAFAFA` canvas,
`#1F2022` ink, `#61A6F6` accent, `#A4A4A4` muted, plus accessible semantic
states). Spacing is on a 4/8/12/16/24/32 scale; radii are 8/12/16px. Motion is
restrained (120–180ms) and respects `prefers-reduced-motion`.

## Testing

Unit tests cover formatting utilities, the mock service, the create-project form
(validation, submit, notify-party convenience), the project list (rendering and
filtering) and the dashboard. Run `npm run test:ci`.

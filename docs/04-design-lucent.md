# Angular UI design contract — Uberdesigner reference library

Reference: https://uberdesigner.ai/t/lucent. The user supplied this site as a resource for free animations and styles, NOT as an application template. Review licenses of individual assets and implement small useful details natively in Angular. Do not add React.

- Colors: background `#FAFAFA`, ink `#1F2022`, blue `#61A6F6`, muted `#A4A4A4`. Add WCAG-conforming dark blue for text on light backgrounds.
- Application: practical back-office interface, NOT landing-page animations. Angular standalone components + Angular Material/CDK; no React/GSAP required.
- Screens: project list, project workspace stepper, invoice/packing/catch forms, transport, validations, import review, PDF preview, admin.
- Tokens: spacing 4/8/12/16/24/32px, border radii 8/12px, clear semantic states, readable system font, keyboard-first workflow.
- Behaviors: form autosave with explicit saved state, inherited-field provenance badges, sticky save/preview actions, consistent row errors, accessible reduced-motion transitions.
- Definition of done: keyboard/accessibility review, reliable responsive tables, forms remain usable with many invoice lines; no excessive motion.

- PDF action bar: **Unsigned for signature** vs **Client preview (sample signature placement)**, with visibly different badges and protective client-preview watermark.

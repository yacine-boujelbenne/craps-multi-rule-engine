# Phase 4.1: State machine, ETD reminders, email
**Agent:** backend, frontend | **Branch:** feat/workflow
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Implement the project state machine (DRAFT_DATA, DOCS_IN_PROGRESS, DRAFTS_GENERATED, SENT_TO_CLIENT, CLIENT_VALIDATED, AWAITING_AUTHORITY, CODES_RECEIVED, FINALIZED, ARCHIVED) with guards using the validation engine. Test each allowed and forbidden transition.
- [ ] Scheduler: daily job creating a reminder when the early draft invoice is due (ETD minus 3 days) and has not been generated; plus alerts for missing documents. Test with a controllable clock.
- [ ] Email sending of generated PDFs (Spring Mail) with a `sent_log` table. Test with GreenMail.
- [ ] Frontend: dashboard (projects by status, upcoming ETDs, overdue drafts, open issues).
- [ ] Commit `feat: workflow and reminders`.

# Phase 1.3: Packing list and catch certificates
**Agent:** backend, frontend | **Branch:** feat/packing-catch
> Before any task: read `AGENTS.md`, `docs/00-context.md`, `docs/01-field-ownership.md`. One unchecked task per session; tick it and add a one-line result note.

- [ ] Backend: `packing_list`, `packing_line`, `container` (container number, seal number, lock number) tables and API. Lock number is optional until BL generation. Tests: net <= gross per line; ISO 6346 container number validator with valid and invalid examples.
- [ ] Backend: `catch_certificate` and `catch_vessel` tables and API. The certificate total is computed as the sum of vessel quantities. Tests: total recomputed on vessel add/update/delete; duplicate vessel on one certificate rejected.
- [ ] Backend: grand total across all catch certificates of a project exposed as `catchTotalKg`. Test with two certificates.
- [ ] Frontend: packing list form (lines linked to invoice items, container block) and catch certificate form (vessel table with per-vessel quantity and running total, project-level total shown at the bottom).
- [ ] Commit `feat: packing list and catch certificates`.

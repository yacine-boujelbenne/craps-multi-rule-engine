# Business decisions — confirmed 2026-11-11
- One project = exactly one shipment.
- Generate HC, draft BL, CC, CO, and Shipping List. Two outputs: unsigned signing-ready draft and clearly invalid, watermarked client preview showing pre-scanned signature placement.
- Seal and lock are different; lock arrives late.
- All data should be coherent; processing yield needs a confirmed rule before asserting catch weights equal exported net weights.
- Manual entry first; OCR/import is later. Angular only; Lucent is a source of visual/animation ideas, not a template.

# Unresolved business choices (do not invent)
1. RESOLVED 2026-11-12: one supplier/factory, one consignee, one client, one importer and one general factory invoice per shipment. Client/importer/consignee may differ. Trinity is only a tag. OPEN: who is billed on the factory invoice? See architecture/COMMERCIAL-MODEL.md.
2. Who supplies lock number and which outputs require it?
3. Exact catch-to-export batch/processing yield equation and permitted deviations.
4. Which authority/issuer and accepted template for HC, CC, CO; what destinations at launch?
5. Shipping List: expected column layout, signatories, and example form?
6. What is 'CS' and is the Processing Statement required at launch?
7. Client preview: authorized pre-scanned signature belongs to whom (company signatory vs official authority)? Only authorized company samples may be placed; never imply official approval.
8. Carrier BL: which fields available before draft; who issues final BL?
9. Which document inputs also need generated PDF output (invoice/packing list)?
10. Confirm deadline business/calendars for early draft invoice.

## Backend Project API verification — BLOCKED (2026-11-11)

Agent 1 supplied the frozen contract and migration; Agent 2 implemented the four
Project endpoints on `feat/backend-projects`. Maven Central DNS resolution fails
and Docker socket access is denied, so compilation and PostgreSQL integration
tests cannot be verified here. Supply a Java 21/Maven environment with repository
and Docker access, then run `cd backend && ./mvnw verify` before acceptance.
The implementation report is `backend/PROJECT-IMPLEMENTATION-HANDOFF.md`.
No completion box has been checked.

## Local accounts and form editing verification — BLOCKED (2026-11-12)

The owner-confirmed account/form access contract and V2 migration are available.
Agent 2 implemented the follow-up on `feat/backend-local-accounts`; see
`backend/ACCOUNTS-IMPLEMENTATION-HANDOFF.md`. Runtime acceptance still requires
Maven repository/DNS access, Docker permissions and Java 21. The prepared local
session/CSRF/account/assignment integration suite has not compiled or run here.

# Newly confirmed 2026-11-11
- Catch input is **pre-processing**, around 22,000 kg in the sample; exported finished net is around 19,200 kg, separated into three box-size categories. Do not equate raw catch mass to finished export mass.
- Shipping List is separate from Packing List; dossier pages 16, 17, 22 show vessel catch quantity/date information and vessel/lot recap, attached among HC documents.
- Signing-ready PDFs must be clean and free of *every* signature or stamp image; blank areas only. Client previews should indicate all relevant signature locations and remain unambiguously NON-OFFICIAL.
- Still open: exact box-size grades/weight allocations; meaning of each vessel date/delay column; raw-material loss categories; approval to use specific signatory illustrations.

## Accounts and assignments — resolved by owner

- Own application accounts; ADMIN creates/manages accounts. No self-registration or existing external login provider.
- ADMIN assigns operators to shipment projects and specific forms.
- Operators may read every form of an assigned shipment and edit only explicitly assigned forms.
- Updated schema/target contracts: architecture/ACCESS-MODEL.md, contracts/accounts-access-v1.yaml, contracts/projects-v2.yaml. Local-account and permission enforcement remain pending Agent 2 implementation and runtime verification.

## Backend verification — resolved on host, 2026-11-12

The owner's full Maven verify run passed on the integrated architecture worktree: 17 unit tests + 77 real PostgreSQL integration tests, zero failures/errors/skips. V1/V2 migrated and the runnable JAR was packaged. Saved XML reports and migration/startup logs were independently inspected. Earlier DNS/Docker/cache blockers above are historical for backend acceptance; sandbox restrictions still apply to agent commands. Frontend/browser integration, actual first-admin bootstrap, a persistent local development database and deployment remain separate pending checks. See `../backend/VERIFICATION-RECHECK-2026-11-12.md`.

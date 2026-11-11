# Agent 2 read-only integration review — 2026-11-11

Reviewed the uncommitted project module, ProjectApiIT, ProjectRequestParserTest and ProjectActorTest in `/run/media/yacine/yassine/DMS/backend-projects`. No Agent 2 files were edited. Findings were sent directly to its session. This is static review; compilation and live execution remain blocked by Maven DNS/Docker access.

1. **P1 — mixed-revision reads:** ProjectService.get/list use default READ COMMITTED transactions. Project entity, parties and transport are loaded by separate statements. A concurrent committed PATCH between those statements can produce an old version with new children, or mismatched party/transport revisions. Use a consistent snapshot for read operations (for example REPEATABLE READ), leaving write/allocation transactions at their intended isolation. Add a reader/writer latch test proving each response's version and children describe one committed state.
2. **P2 — permissive identifier/value parsing:** ProjectRequestParser checks textual type then delegates UUID/date parsing to Jackson. The [Jackson UUID deserializer](https://raw.githubusercontent.com/FasterXML/jackson-databind/2.19/src/main/java/com/fasterxml/jackson/databind/deser/std/UUIDDeserializer.java) accepts 22/24-character Base64 UUIDs, beyond the published UUID text format. Empty/whitespace UUID/date coercion also needs regression coverage to prevent accidental PATCH clearing. Validate canonical UUID/date text before binding; only explicit JSON null clears nullable fields.
3. **Documentation:** PROJECT-IMPLEMENTATION-HANDOFF.md still describes the pre-contract blocked scaffold; rewrite it around the actual implementation before delivery.

The reviewed write path otherwise appears aligned with V1: nullable initial JPA @Version, project lock before replacing children, aggregate touch for child-only changes, transaction-local actor context, role-qualified party lookups, principal-to-active-user mapping and database roles/access checks. No obvious compilation error was identified by inspection; this is not a successful build or an approval of runtime behavior. JWT tests using jwt() do not exercise actual signature/issuer/audience validation.

At review publication, findings were handed to Agent 2 for correction and verification; fixes are not yet independently verified here.

## Follow-up: corrections checked in source

Agent 2 subsequently changed get/list to explicit REPEATABLE_READ and added a latch-controlled reader/writer integration test that commits a child/port PATCH between root and detail reads. The parser now requires canonical hyphenated UUID text for scalar IDs and producer arrays, plus YYYY-MM-DD text before date binding. Added parser tests reject empty, whitespace and Base64 values. These specific changes were inspected read-only and address findings 1 and 2 at the source level. Their regression tests still have not compiled or executed. Agent 2 reported that the stale implementation handoff was being rewritten; that documentation rewrite was not yet verified during this follow-up.

The completed backend handoff was subsequently inspected and both Agent 2 delivery commits integrated on feat/architecture-phase1. The stale-report finding is resolved. Full runtime acceptance remains open.

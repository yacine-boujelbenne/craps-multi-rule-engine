# Decisions and unresolved requirements — 2026-11-11

This is an additive architecture decision record. Existing shared plans and playbooks were preserved. The user's current instructions and latest owner-confirmed rules take precedence over older contradictory passages.

| Topic | Decision/evidence | Impact |
|---|---|---|
| Initial save | Owner answered: **Reference only; allow incomplete drafts**. | Only reference required; all readiness checks are later. |
| Status name | Backend Agent 2 reports an explicit user requirement for **DRAFT_DATA**; contract uses that value. | Older master plan DRAFT is a terminology conflict, not an extra state; frontend must use the frozen enum. |
| Shipment identity | Exactly one project = one shipment, confirmed in current prompt/master plan. | Project UUID is shipment UUID; transport is an extension, not another independent shipment table. |
| Raw vs finished | Raw catches, finished net and packed gross are distinct. Owner now requests measurement recording without mandatory loss/by-product accounting. | Validate entered line/shipment totals and gross >= net; no raw=finished equality, required yield calculation or loss workflow. See QUANTITY-MODEL.md. |
| Shipping List | Owner-confirmed distinct HC annex (sample pages 16/17/22). | Separate document type; never reuse Packing List as an alias. No original customer samples added. |
| Seal / lock | Distinct identifiers; lock supplied late. | Both nullable for initial draft. Older blanket lock-before-BL rule is deferred until the output-specific requirement is confirmed. |
| Chronology | ETD is estimated, not actual. | Older production <= shipment <= ETD and unconditional three-day blocker are not enforced. Only event-local known chronology is constrained. |
| PDF safety | Latest confirmed rules: completely blank signature/stamp spaces on every signing-ready page; conspicuously invalid illustrative preview. | Metadata separates variants. Renderer and authorization are future work. |
| Output scope | Five confirmed output families only. | Older G6/G7/playbooks that imply mandatory invoice/PL/PS PDFs are not current acceptance scope. |
| Internal accounts | Owner confirmed own application accounts, created/managed by ADMIN; no self-registration. | Replaces the previous external-login assumption; see ACCESS-MODEL.md and accounts-access-v1.yaml. |
| Form permissions | Owner confirmed operators read all forms of assigned shipments but edit only assigned forms. | Project membership grants read access; explicit form assignment grants editing. |
| Commercial parties/invoice | Owner confirmed one supplier/factory, one consignee, one client, one importer and one general factory invoice per shipment. Client/importer/consignee can be different companies. | Keep separate roles; draft omissions still allowed. The factory/supplier issues the general invoice to the consignee; see COMMERCIAL-MODEL.md. |
| Trinity | Owner confirmed it is only a tag. | No special business, access or calculation rules. |
| Shipment quantities and protected defaults | Owner confirmed carton quantities/net kg/gross kg vary per shipment. Box weights, size-grade definitions and price/kg are defaults that only ADMIN may change. | Assigned operators enter shipment quantities/actual weights; no operator override of protected defaults. Preserve applied defaults/prices in history. See QUANTITY-MODEL.md. |
| Catch/CC/HC scope | Owner confirmed no lot/certificate sharing across shipments and one CC for one shipment/HC. | One logical CC and HC per completed shipment; many vessels/lots and document revisions remain possible within it. |
| Agent ownership | Current user assigns architect schema/bootstrap/contracts and Agent 2 business REST APIs. | Agent 1 owns migrations/contracts and reviews implementation. All agents use the single canonical checkout and coordinate branch switches; no worktrees or copies (owner reconfirmed 2026-11-14). |

## Still open; not guessed

1. RESOLVED: commercial cardinalities, Trinity meaning and invoice parties are documented in COMMERCIAL-MODEL.md. One factory/supplier issues one general invoice to the consignee. Client/importer/consignee can differ; do not collapse those roles. Subsequent contract/schema implementation remains to be coordinated.
2. RESOLVED for current manual workflow: three supplied grade labels and quantity/price structure; owner requires no loss/by-product entry or yield/tolerance gate. See QUANTITY-MODEL.md. Vessel date/delay semantics and invoice rounding policy still need definition when their fields are contracted; do not block draft entry with a processing mass-balance requirement.
3. Lock source and the individual outputs that require it. No draft creation blocker is invented.
4. Destination countries, approved layouts, external issuers, all signing roles and rights to company illustration assets. PDF templates are not implemented here.
5. Early invoice deadline calendar/business days and warning vs blocker; invoice/packing PDF generation and Processing Statement/CS meaning.
6. Organization/port directory CRUD/search implementation remains pending. Account ownership and project/form visibility are resolved in ACCESS-MODEL.md; Agent 2 must implement the updated internal-login and assignment contract.
7. RESOLVED: lots and CCs are not shared between shipments; one shipment has one logical CC and one HC. Source/issuer reference uniqueness scope still belongs to the later identifier contract; do not confuse document revisions with additional certificates.
8. Document artifact storage, retention and legitimate external-issued originals. Private artifact keys/snapshot metadata are reserved; no public storage, approval claim or import implementation is provided.

Only items needed by later workflows stay blocked; they do not prevent the owner-confirmed reference-only project creation slice. No unresolved rule was converted into a default tolerance, template, signature authorization, fixed sample grade or mandatory draft field.

# Commercial parties and factory invoice — owner decisions, 2026-11-12

Each project represents one export shipment. The owner confirmed:

| Role/record | Completed shipment cardinality | Meaning established so far |
|---|---|---|
| Supplier/factory | One | The factory supplies the shipment and issues its general invoice. |
| Consignee | One | The billed party on the factory/supplier invoice; not automatically the client or importer. |
| Client | One | A separate party role; the invoice is billed to the consignee. |
| Importer | One | Its own party role; not automatically the client or consignee. |
| General factory invoice | One | Issued by the factory/supplier and billed to the shipment consignee. |

Client, importer and consignee **can be different companies**. The organization directory should allow the same organization to hold several roles too; do not require either equality or distinct IDs across those roles. Exporter remains its own existing role; the owner has not equated exporter with factory.

“Trinity” is only a tag. It creates no special party cardinality, calculation, workflow, tenant or access rule.

## Consequences for the next contract/schema slice

- Represent supplier, client, importer and consignee as separate named selections, with at most one company for each role within a shipment.
- Reference-only project creation remains allowed. A saved draft may omit parties and the invoice; the completed commercial workflow requires its one factory invoice. Do not make those facts mandatory merely to create a draft.
- A factory invoice's later corrections/revisions do not introduce multiple independent invoices for the shipment. Generated document versioning stays separate from commercial invoice identity.
- The canonical invoice issuer is the shipment supplier/factory; the canonical bill-to party is the shipment consignee. Reuse those project party records, rather than introducing independent bill-to data that can diverge. Client and importer remain separate role selections.
- The confirmed commercial supplier rule does not determine raw-catch lot reuse, upstream fishing-vessel ownership or processing mass balance. Those traceability questions remain open.
- Existing projects-v2.yaml has consigneeId and producerIds but no dedicated supplierId/clientId/importerId. Existing directory role checks lack CLIENT/IMPORTER. The current API/database do not yet enforce this newly confirmed commercial model. Publish the coordinated next contract and use a new Flyway migration for these confirmed party/billing rules; preserve applied V1/V2 and existing clients.
- Do not silently map CLIENT to the earlier BUYER role, collapse importer into consignee, or reinterpret existing factory/producer arrays without an explicit contract mapping.

## Invoice-party clarification resolved

The owner explicitly confirmed that the consignee is billed on the factory's general invoice. The issuer/billed-party relationship is therefore supplier/factory -> consignee. Invoice/Packing List PDF generation is still separately unconfirmed; OCR/imports remain outside this milestone.

This record documents business requirements only. No controllers, schemas or frontend files were changed for this clarification, and the previously passing backend code remains unchanged.

# Quantity, grades and certificate scope — owner confirmation, 2026-11-12

This record supersedes earlier proposals that required loss/by-product entry or processing mass-balance approval. It records the owner's answers and supplied invoice-table image. Source imagery and customer pricing are not copied into the repository; the image's arithmetic was checked separately with Decimal.

## Grade and carton entry

The current product is organized into three size grades shown as 50–80, 80–100 and 100+ in the image's Size (g) column. Preserve these grade labels and units. They are separate from carton net mass: the supplied example shows 12 kg net per carton in every grade. Do not interpret the grade numbers as carton kilograms or assume the example establishes a universal carton weight for all shipments/products.

For each grade, record carton count, net kg per carton when supplied, total finished net kg, total packed gross kg and price per kg. Record invoice currency explicitly; the image uses USD. Carton quantities, total net kg and total gross kg vary independently by shipment and must never be fixed to the supplied example. Box-weight defaults, size grades and price/kg are configurable by ADMIN only. An assigned operator may enter shipment quantities and actual net/gross totals, but may not change those defaults or override the protected box weight, grade definition or price/kg through an invoice/packing payload. Copy the applied grade label, box-weight default and price into the shipment/invoice snapshot so later administrative default changes do not rewrite existing data/history. Do not hardcode source-image prices into application constants or fixtures.

When the relevant values exist:

- When all cartons on a line use a declared uniform net kg/carton, compare its line net kg with cartons × net kg/carton. A default is not a substitute for a missing measured shipment total; never silently overwrite an entered measurement or change an admin-controlled box-weight setting to make the numbers match.
- Line amount = total line net kg × the line price per kg, using exact decimal arithmetic and the invoice's documented rounding/currency policy.
- Total cartons, net kg, gross kg and invoice amount are sums of their corresponding lines, without joining raw catches into those aggregates.
- Gross kg must be at least net kg. Reuse the same canonical values across consuming documents; do not ask operators to re-enter independent document totals.
- Do not derive gross kg from a guessed tare/default ratio. The supplied sample is not a universal weight recipe.

Draft values may be absent. Validate relationships when their inputs exist; show incomplete totals as incomplete rather than silently treating missing inputs as zero/complete. Document-specific readiness remains separate from draft saving.

## Field permissions — explicitly confirmed by owner

| Field group | Shipment behavior | Who may change it |
|---|---|---|
| Carton quantity, total net kg, total gross kg | Shipment-specific values; sample totals are never constants. | ADMIN or OPERATOR assigned to the canonical owning form. |
| Standard/default box weight | Configurable default, preserved as applied for the shipment. | ADMIN only. |
| Size-grade definitions/labels | Configurable product defaults. | ADMIN only. Operators select existing configured grades as allowed by their form. |
| Price per kg | Configurable default, preserved as applied on invoice lines. | ADMIN only. No operator price override. |

Form edit assignment does not override these field restrictions. Enforce them on the server as well as through read-only controls/source badges in Angular; reject tampered writes to protected configuration fields. Administrative changes must be explicit and audited, and must not silently recalculate previous invoices/documents. This supersedes the earlier generic phrase “editable price defaults” where it could imply operator access.

## Record measurements; no required loss workflow

The owner explicitly does not want loss/by-product accounting for this workflow. Record the supplied net/gross measurements and keep them consistent within the shipment. Any entered raw catch amount remains its own measurement and must not be equated with finished exported net or packed gross weight. The gross-minus-net difference is not evidence of processing loss.

Do not require operators to classify losses, enter by-products, supply a processing-yield equation, meet an invented yield tolerance or create a processing batch simply to save invoice/packing quantities. Earlier sample 22,000 -> 19,200 + 2,800 balance is only historical illustrative math; it is not an acceptance condition or a required explanation from the operator.

Existing V1 processing tables/views and tests remain preserved. Their optional adjustment records/diagnostic calculations must not become mandatory UI steps or generation blockers. Subsequent commercial schema/API must support direct grade/weight entry without forcing an artificial processing record.

## Shipment-scoped traceability and certificate cardinality

- A catch lot is not shared between shipment projects.
- A catch certificate is not shared between shipments.
- Each completed shipment has one CC and one HC: one project/shipment -> one CC -> one HC dossier relationship.
- One CC may still contain multiple fishing vessels and catch lots within that shipment; no single-vessel assumption is introduced.
- Revisions and the unsigned/client-preview variants are versions of that one logical CC/HC, not additional independent certificates.
- The Shipping List remains a separate annex/document tied to the same shipment's HC and catch records.

Draft projects can exist before a CC/HC record is created. Enforce at most one logical CC and HC while entering data, with the required records present when their later workflow needs them. Do not manufacture an official certificate, number, signature or issuance merely because a draft record exists.

Future services must reject linking an existing physical lot/certificate to another shipment. Do not invent globally unique textual certificate/lot reference rules: authority/source identifiers and their uniqueness scope must follow the issuing/source identity rules when that API is defined.

## Implementation boundary

This is a confirmed business contract in prose, not a claim that the existing code already enforces it. V1 allowed multiple catch_certificate rows and multiple CC/HC logical keys and required processing_batch_id for finished quantities. Prepared V3 narrows that cardinality and makes the batch link optional; its host PostgreSQL execution and 12 schema checks now pass. New commercial API acceptance remains pending a corrected CSRF test fixture and host rerun. The first commercial API contract covers admin grade defaults and parties. Invoice/packing entry and applied-default snapshots require the separate slice 2 contract before implementation. Preserve applied V1/V2 and the passing backend suite. Angular may retain the grade layout as reference, but must not invent uncontracted persistence endpoints.

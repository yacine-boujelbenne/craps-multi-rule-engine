# Context

## Goal
One platform that manages the whole export documentation file per shipment. Each document is a form filled in chronological order. Documents are linked: a value entered or imported once is reused and checked everywhere.

Owner-confirmed main objective (2026-11-14): build the usable app step by step until complete. First, ADMIN enters and maintains all partner companies. Then ADMIN creates a project/deal (one shipment), selects those companies, and reuses their stored information to fill its forms. Those forms later produce documents that can be generated and printed. Complete and verify each user-visible step before moving to the next; company information must not require repeated entry, and later company edits must preserve previously generated document snapshots.

## Documents
1. Invoice (facture)
2. Packing list (linked to the invoice)
3. Catch certificates + list of active fishing vessels, with the quantity per vessel and a final total of all quantities
4. BL (Bill of Lading): travel details, container number, seal number. Lock number comes from the packing list and is the last detail added before the BL draft is printed
5. HC: health certificate, interconnects all previous documents, signed by the health authority
6. CO: certificate of origin, contains all previous information
7. Processing statement (PS)
8. Early draft invoice, produced at least 3 days before ETD

All drafts are generated from the platform.

## Actors
Exporter and producer are not necessarily the same party. The consignee is the importer; the party notified on arrival is not necessarily the consignee. "Trinity" model (to confirm): several suppliers, one consignee.

## Two phases
- Phase 1: drafts. A version with stamp for the client, a version without stamp for printing.
- Phase 2: after stamp and confirmation by the health authority, add health code, CO code, PS code and CS, then generate final documents.

## Data sources
- Invoice and packing list: scrape all information.
- Carrier/forwarder BL: vessel name, voyage number, BL number. Each carrier (Maersk, CMA CGM, ...) has its own BL model. The forwarder sends it by email.
- ETD drives reminders.

## Stack
Java/Spring Boot, Angular, PostgreSQL (Supabase only as managed Postgres/realtime), OCR engine to be determined (bake-off in Phase 3), Apache POI / Commons CSV / Tika / PDFBox for import.

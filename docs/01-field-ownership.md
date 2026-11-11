# Field ownership contract

Each field has ONE owner document. Others consume it read-only. Overrides need an explicit logged action.

| Data | Owner | Consumers |
|---|---|---|
| Exporter, producer, consignee, notify party | Project setup | Invoice, PL, BL, HC, CO, PS |
| Products, species, HS code, quantities, prices | Invoice | PL, HC, CO, PS |
| Cartons, net/gross weights, lots | Packing list | BL, HC, CO |
| Lock number | Packing list (last field before BL print) | BL |
| Container number, seal number | Packing list / BL | HC, CO |
| Vessel list, quantity per vessel, total | Catch certificate | HC, CO, PS |
| Vessel name, voyage number, BL number | Carrier BL (imported) | HC, CO |
| ETD | Project | Reminders, early draft invoice |
| Health code, CO code, PS code, CS | Phase 2 | Final documents |

## Cross-document validation rules
- Catch certificate total = sum of vessel quantities; compared with invoice and packing list net quantity (tolerance configurable).
- Container and seal numbers identical across PL, BL, HC, CO; container number passes ISO 6346 check digit.
- Lock number present before BL draft generation.
- Product names, species and HS codes consistent across documents.
- Dates: production <= shipment <= ETD; early draft invoice created at least 3 days before ETD.
- All Phase 2 codes present before a project can be FINALIZED.

## Processing and annex facts (confirmed)
- Raw caught kg is separate from finished exported net kg. Processing batch holds input mass, output mass, attributed losses/byproducts, and category allocations.
- Category = box-size grade, not necessarily biological species; treat size grades as configurable master data.
- Shipping List is a separate HC annex aggregating vessel/lot/catch amounts and relevant dates; not an alias of Packing List.
- All ready-for-signing PDFs must have completely empty signature/stamp spaces. The client preview is illustrative and prominently non-official.

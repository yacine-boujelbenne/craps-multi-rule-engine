-- Additive commercial setup. Existing applied V1/V2 and Project v2 DTOs stay intact.
ALTER TABLE organization ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0);
ALTER TABLE organization_role DROP CONSTRAINT organization_role_role_check;
ALTER TABLE organization_role ADD CONSTRAINT organization_role_role_check CHECK (role IN (
    'EXPORTER','PRODUCER','SUPPLIER','BUYER','CONSIGNEE','NOTIFY_PARTY',
    'FREIGHT_FORWARDER','SHIPPING_LINE','AUTHORITY','CLIENT','IMPORTER'));

-- No equality/distinctness requirement between different roles.
-- Fail on contradictory legacy data rather than silently deleting party records.
CREATE UNIQUE INDEX project_one_commercial_party_idx ON project_party(project_id,role)
    WHERE role IN ('SUPPLIER','CLIENT','IMPORTER','CONSIGNEE');

ALTER TABLE product ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0);
ALTER TABLE product_category
    ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    ADD COLUMN display_order integer NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    ADD COLUMN default_carton_net_kg numeric(18,3) CHECK (default_carton_net_kg > 0),
    ADD COLUMN default_price_per_kg numeric(18,4) CHECK (default_price_per_kg >= 0),
    ADD COLUMN default_currency varchar(3) CHECK (default_currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT product_category_price_currency_pair CHECK (
        (default_price_per_kg IS NULL AND default_currency IS NULL) OR
        (default_price_per_kg IS NOT NULL AND default_currency IS NOT NULL));

-- Certificate revisions/variants remain in document_version, not additional instances.
ALTER TABLE catch_certificate ADD CONSTRAINT catch_certificate_one_per_project UNIQUE (project_id);
CREATE UNIQUE INDEX document_one_hc_cc_per_project_idx ON document_instance(project_id,document_type_code)
    WHERE document_type_code IN ('HC','CC');

-- Shipment packing entry is valid without a processing/loss-accounting workflow.
ALTER TABLE finished_product_quantity ALTER COLUMN processing_batch_id DROP NOT NULL;

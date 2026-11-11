-- UUIDs and exact decimal kg are canonical; original catch units remain available.
CREATE TABLE app_user (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    external_subject varchar(255) NOT NULL UNIQUE CHECK (btrim(external_subject) <> ''),
    display_name varchar(200) NOT NULL CHECK (btrim(display_name) <> ''),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE role_assignment (
    user_id uuid NOT NULL REFERENCES app_user(id),
    role varchar(30) NOT NULL CHECK (role IN ('ADMIN','OPERATOR','REVIEWER','VIEWER')),
    PRIMARY KEY (user_id, role)
);
CREATE TABLE organization (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name varchar(250) NOT NULL CHECK (btrim(legal_name) <> ''),
    country_code varchar(2) CHECK (country_code ~ '^[A-Z]{2}$'),
    address_text text,
    registration_number varchar(100),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE organization_role (
    organization_id uuid NOT NULL REFERENCES organization(id),
    role varchar(30) NOT NULL CHECK (role IN ('EXPORTER','PRODUCER','SUPPLIER','BUYER','CONSIGNEE','NOTIFY_PARTY','FREIGHT_FORWARDER','SHIPPING_LINE','AUTHORITY')),
    PRIMARY KEY (organization_id, role)
);
CREATE TABLE port (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(200) NOT NULL CHECK (btrim(name) <> ''),
    country_code varchar(2) NOT NULL CHECK (country_code ~ '^[A-Z]{2}$'),
    un_locode varchar(5) UNIQUE CHECK (un_locode ~ '^[A-Z]{2}[A-Z0-9]{3}$'),
    active boolean NOT NULL DEFAULT true
);
CREATE TABLE product (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(80) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name varchar(250) NOT NULL CHECK (btrim(name) <> ''),
    scientific_name varchar(250),
    hs_code varchar(10) CHECK (hs_code ~ '^[0-9]{6,10}$'),
    active boolean NOT NULL DEFAULT true
);
-- Category means a configurable box-size grade, not a species or a fixed sample size.
CREATE TABLE product_category (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES product(id),
    code varchar(80) NOT NULL CHECK (btrim(code) <> ''),
    name varchar(200) NOT NULL CHECK (btrim(name) <> ''),
    active boolean NOT NULL DEFAULT true,
    UNIQUE (product_id, code),
    UNIQUE (id, product_id)
);
CREATE TABLE project (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    reference varchar(80) NOT NULL UNIQUE CHECK (reference ~ '^[A-Z0-9][A-Z0-9._/-]{0,79}$'),
    status varchar(40) NOT NULL DEFAULT 'DRAFT_DATA' CHECK (status IN (
        'DRAFT_DATA','IN_PROGRESS','READY_FOR_DRAFT_REVIEW','DRAFT_REVIEWED',
        'AWAITING_EXTERNAL_ISSUANCE','OFFICIAL_DOCUMENTS_RECEIVED','COMPLETE','ARCHIVED')),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    destination_country_code varchar(2) CHECK (destination_country_code ~ '^[A-Z]{2}$'),
    expected_departure_date date,
    notes varchar(2000),
    created_by uuid REFERENCES app_user(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX project_listing_idx ON project (created_at DESC, id DESC);
CREATE INDEX project_status_listing_idx ON project (status, created_at DESC, id DESC);
CREATE INDEX project_reference_prefix_idx ON project (reference varchar_pattern_ops);
CREATE TABLE project_access (
    project_id uuid NOT NULL REFERENCES project(id),
    user_id uuid NOT NULL REFERENCES app_user(id),
    PRIMARY KEY (project_id, user_id)
);
CREATE INDEX project_access_user_idx ON project_access (user_id, project_id);
CREATE TABLE project_party (
    project_id uuid NOT NULL REFERENCES project(id),
    organization_id uuid NOT NULL,
    role varchar(30) NOT NULL,
    PRIMARY KEY (project_id, organization_id, role),
    FOREIGN KEY (organization_id, role) REFERENCES organization_role(organization_id, role)
);
CREATE INDEX project_party_organization_idx ON project_party (organization_id, role);
-- The project itself is the shipment identity; no second independent shipment entity.
CREATE TABLE shipment_transport (
    project_id uuid PRIMARY KEY REFERENCES project(id),
    port_of_loading_id uuid REFERENCES port(id),
    port_of_discharge_id uuid REFERENCES port(id),
    shipping_vessel_name varchar(200),
    voyage_number varchar(100),
    carrier_bl_reference varchar(150),
    expected_arrival_date date,
    actual_departure_date date,
    carrier_verified_at timestamptz,
    carrier_verified_by uuid REFERENCES app_user(id)
);
CREATE TABLE container (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    container_number varchar(11) CHECK (container_number ~ '^[A-Z]{3}[UJZ][0-9]{7}$'),
    seal_number varchar(100) CHECK (btrim(seal_number) <> ''),
    lock_number varchar(100) CHECK (btrim(lock_number) <> ''),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (project_id, container_number),
    UNIQUE (id, project_id)
);
CREATE TABLE fishing_vessel (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(200) NOT NULL CHECK (btrim(name) <> ''),
    registration_number varchar(100) NOT NULL CHECK (btrim(registration_number) <> ''),
    flag_country_code varchar(2) NOT NULL CHECK (flag_country_code ~ '^[A-Z]{2}$'),
    active boolean NOT NULL DEFAULT true,
    UNIQUE (flag_country_code, registration_number)
);
CREATE TABLE catch_certificate (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    internal_reference varchar(100) NOT NULL CHECK (btrim(internal_reference) <> ''),
    external_reference varchar(150),
    issuer_organization_id uuid REFERENCES organization(id),
    issued_on date,
    status varchar(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','VERIFIED')),
    evidence_key text,
    verified_by uuid REFERENCES app_user(id),
    verified_at timestamptz,
    CHECK (status <> 'VERIFIED' OR (external_reference IS NOT NULL AND btrim(external_reference) <> ''
        AND issuer_organization_id IS NOT NULL AND issued_on IS NOT NULL
        AND evidence_key IS NOT NULL AND btrim(evidence_key) <> '' AND verified_by IS NOT NULL AND verified_at IS NOT NULL)),
    UNIQUE (project_id, internal_reference),
    UNIQUE (id, project_id)
);
CREATE TABLE catch_lot (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    catch_certificate_id uuid NOT NULL,
    fishing_vessel_id uuid NOT NULL REFERENCES fishing_vessel(id),
    product_id uuid NOT NULL REFERENCES product(id),
    supplier_id uuid REFERENCES organization(id),
    lot_reference varchar(100) NOT NULL CHECK (btrim(lot_reference) <> ''),
    caught_on date,
    landed_on date,
    raw_weight_kg numeric(18,3) NOT NULL CHECK (raw_weight_kg > 0),
    original_quantity numeric(18,3),
    original_unit varchar(20),
    CHECK ((original_quantity IS NULL AND original_unit IS NULL) OR
        (original_quantity IS NOT NULL AND original_quantity > 0 AND original_unit IS NOT NULL AND btrim(original_unit) <> '')),
    CHECK (caught_on IS NULL OR landed_on IS NULL OR caught_on <= landed_on),
    FOREIGN KEY (catch_certificate_id, project_id) REFERENCES catch_certificate(id, project_id),
    UNIQUE (project_id, lot_reference),
    UNIQUE (id, project_id)
);
CREATE INDEX catch_lot_certificate_idx ON catch_lot (catch_certificate_id, project_id);
CREATE INDEX catch_lot_vessel_idx ON catch_lot (fishing_vessel_id);
CREATE TABLE processing_batch (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    batch_reference varchar(100) NOT NULL CHECK (btrim(batch_reference) <> ''),
    processor_id uuid REFERENCES organization(id),
    processed_on date,
    notes text,
    UNIQUE (project_id, batch_reference),
    UNIQUE (id, project_id)
);
-- Many catch lots may feed many batches; each allocation is counted exactly once.
CREATE TABLE raw_material_allocation (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    catch_lot_id uuid NOT NULL,
    processing_batch_id uuid NOT NULL,
    raw_weight_kg numeric(18,3) NOT NULL CHECK (raw_weight_kg > 0),
    FOREIGN KEY (catch_lot_id, project_id) REFERENCES catch_lot(id, project_id),
    FOREIGN KEY (processing_batch_id, project_id) REFERENCES processing_batch(id, project_id),
    UNIQUE (catch_lot_id, processing_batch_id)
);
CREATE INDEX raw_allocation_batch_idx ON raw_material_allocation (processing_batch_id, project_id);
CREATE TABLE finished_product_quantity (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    processing_batch_id uuid NOT NULL,
    product_id uuid NOT NULL REFERENCES product(id),
    product_category_id uuid NOT NULL,
    container_id uuid,
    carton_count integer NOT NULL CHECK (carton_count > 0),
    net_weight_kg numeric(18,3) NOT NULL CHECK (net_weight_kg > 0),
    gross_weight_kg numeric(18,3) CHECK (gross_weight_kg >= net_weight_kg),
    production_date date,
    expiry_date date,
    CHECK (production_date IS NULL OR expiry_date IS NULL OR production_date <= expiry_date),
    FOREIGN KEY (processing_batch_id, project_id) REFERENCES processing_batch(id, project_id),
    FOREIGN KEY (product_category_id, product_id) REFERENCES product_category(id, product_id),
    FOREIGN KEY (container_id, project_id) REFERENCES container(id, project_id)
);
CREATE INDEX finished_quantity_batch_idx ON finished_product_quantity (processing_batch_id, project_id);
CREATE INDEX finished_quantity_container_idx ON finished_product_quantity (container_id, project_id);
CREATE TABLE processing_output_adjustment (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    processing_batch_id uuid NOT NULL,
    kind varchar(20) NOT NULL CHECK (kind IN ('LOSS','BY_PRODUCT','OTHER_OUTPUT')),
    description varchar(500) NOT NULL CHECK (btrim(description) <> ''),
    weight_kg numeric(18,3) NOT NULL CHECK (weight_kg > 0),
    FOREIGN KEY (processing_batch_id, project_id) REFERENCES processing_batch(id, project_id)
);
CREATE INDEX processing_adjustment_batch_idx ON processing_output_adjustment (processing_batch_id, project_id);

CREATE VIEW catch_certificate_totals AS
SELECT c.id AS catch_certificate_id, c.project_id,
       coalesce(sum(l.raw_weight_kg), 0)::numeric(18,3) AS raw_weight_kg
FROM catch_certificate c LEFT JOIN catch_lot l ON l.catch_certificate_id = c.id
GROUP BY c.id, c.project_id;

-- Aggregate each side independently so multiple allocations/categories never multiply rows.
CREATE VIEW processing_batch_balance AS
SELECT b.id AS processing_batch_id, b.project_id,
    coalesce(i.kg,0)::numeric(18,3) AS input_raw_kg,
    coalesce(f.kg,0)::numeric(18,3) AS finished_net_kg,
    coalesce(a.kg,0)::numeric(18,3) AS accounted_other_output_kg,
    (coalesce(i.kg,0)-coalesce(f.kg,0)-coalesce(a.kg,0))::numeric(18,3) AS unaccounted_kg,
    round(100 * f.kg / nullif(i.kg,0), 4) AS yield_percent
FROM processing_batch b
LEFT JOIN (SELECT processing_batch_id, sum(raw_weight_kg) kg FROM raw_material_allocation GROUP BY processing_batch_id) i ON i.processing_batch_id=b.id
LEFT JOIN (SELECT processing_batch_id, sum(net_weight_kg) kg FROM finished_product_quantity GROUP BY processing_batch_id) f ON f.processing_batch_id=b.id
LEFT JOIN (SELECT processing_batch_id, sum(weight_kg) kg FROM processing_output_adjustment GROUP BY processing_batch_id) a ON a.processing_batch_id=b.id;

-- Lock the parent lot before checking the sum, serializing competing allocations.
CREATE FUNCTION enforce_catch_allocation_capacity() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE available numeric(18,3); used numeric(18,3);
BEGIN
    IF TG_TABLE_NAME = 'raw_material_allocation' THEN
        SELECT raw_weight_kg INTO available FROM catch_lot WHERE id = NEW.catch_lot_id FOR UPDATE;
        SELECT coalesce(sum(raw_weight_kg),0) INTO used FROM raw_material_allocation
            WHERE catch_lot_id = NEW.catch_lot_id AND id <> NEW.id;
        IF used + NEW.raw_weight_kg > available THEN
            RAISE EXCEPTION 'Catch allocation exceeds raw catch quantity' USING ERRCODE='23514';
        END IF;
    ELSE
        SELECT coalesce(sum(raw_weight_kg),0) INTO used FROM raw_material_allocation WHERE catch_lot_id=NEW.id;
        IF used > NEW.raw_weight_kg THEN
            RAISE EXCEPTION 'Raw catch quantity is below existing allocations' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER allocation_capacity BEFORE INSERT OR UPDATE ON raw_material_allocation
    FOR EACH ROW EXECUTE FUNCTION enforce_catch_allocation_capacity();
CREATE TRIGGER catch_capacity BEFORE UPDATE ON catch_lot
    FOR EACH ROW EXECUTE FUNCTION enforce_catch_allocation_capacity();

CREATE TABLE document_type (
    code varchar(30) PRIMARY KEY,
    name varchar(120) NOT NULL
);
INSERT INTO document_type (code, name) VALUES
    ('HC','Health Certificate'),('BL_DRAFT','Draft Bill of Lading'),
    ('CC','Catch Certificate'),('CO','Certificate of Origin'),('SHIPPING_LIST','Shipping List (HC annex)');
CREATE TABLE document_template_version (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type_code varchar(30) NOT NULL REFERENCES document_type(code),
    template_key varchar(150) NOT NULL CHECK (btrim(template_key) <> ''),
    revision integer NOT NULL CHECK (revision > 0),
    content_sha256 varchar(64) NOT NULL CHECK (content_sha256 ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (document_type_code, template_key, revision),
    UNIQUE (id, document_type_code)
);
CREATE TABLE document_instance (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    document_type_code varchar(30) NOT NULL REFERENCES document_type(code),
    logical_key varchar(100) NOT NULL DEFAULT 'default' CHECK (btrim(logical_key) <> ''),
    parent_document_id uuid,
    status varchar(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED','IN_PROGRESS','READY','DRAFT','APPROVED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (parent_document_id, project_id) REFERENCES document_instance(id, project_id),
    CHECK (parent_document_id IS NULL OR parent_document_id <> id),
    UNIQUE (project_id, document_type_code, logical_key),
    UNIQUE (id, project_id),
    UNIQUE (id, document_type_code)
);
CREATE TABLE document_version (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    document_instance_id uuid NOT NULL,
    document_type_code varchar(30) NOT NULL,
    template_version_id uuid NOT NULL,
    revision integer NOT NULL CHECK (revision > 0),
    variant varchar(30) NOT NULL CHECK (variant IN ('UNSIGNED_FOR_SIGNATURE','CLIENT_PREVIEW')),
    source_revision bigint NOT NULL CHECK (source_revision >= 0),
    snapshot_json jsonb NOT NULL CHECK (jsonb_typeof(snapshot_json) = 'object'),
    content_sha256 varchar(64) NOT NULL CHECK (content_sha256 ~ '^[0-9a-f]{64}$'),
    artifact_key text NOT NULL CHECK (btrim(artifact_key) <> ''),
    generated_by uuid REFERENCES app_user(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (document_instance_id, document_type_code) REFERENCES document_instance(id, document_type_code),
    FOREIGN KEY (template_version_id, document_type_code) REFERENCES document_template_version(id, document_type_code),
    UNIQUE (document_instance_id, revision, variant)
);
CREATE INDEX document_version_latest_idx ON document_version (document_instance_id, variant, revision DESC);
CREATE VIEW document_version_freshness AS
SELECT v.id AS document_version_id, d.project_id, v.source_revision,
       p.version AS current_project_revision, (v.source_revision <> p.version) AS stale
FROM document_version v JOIN document_instance d ON d.id=v.document_instance_id JOIN project p ON p.id=d.project_id;

CREATE TABLE audit_log (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id uuid REFERENCES project(id),
    entity_table varchar(80) NOT NULL,
    entity_id text NOT NULL,
    operation varchar(10) NOT NULL CHECK (operation IN ('INSERT','UPDATE','DELETE')),
    actor_id uuid REFERENCES app_user(id),
    actor_kind varchar(10) NOT NULL CHECK (actor_kind IN ('USER','SYSTEM')),
    request_id varchar(150),
    before_json jsonb,
    after_json jsonb,
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CHECK ((actor_kind='USER' AND actor_id IS NOT NULL) OR (actor_kind='SYSTEM' AND actor_id IS NULL))
);
CREATE INDEX audit_log_project_idx ON audit_log (project_id, occurred_at DESC, id DESC);
CREATE INDEX audit_log_entity_idx ON audit_log (entity_table, entity_id, occurred_at DESC);

CREATE FUNCTION reject_immutable_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME USING ERRCODE='23514';
END $$;
CREATE TRIGGER immutable_document_version BEFORE UPDATE OR DELETE ON document_version
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_mutation();
CREATE TRIGGER immutable_template_version BEFORE UPDATE OR DELETE ON document_template_version
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_mutation();
CREATE TRIGGER immutable_audit_log BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_mutation();

CREATE FUNCTION audit_business_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE before_row jsonb; after_row jsonb; entity jsonb; actor uuid; shipment uuid;
BEGIN
    IF TG_OP <> 'INSERT' THEN before_row := to_jsonb(OLD); END IF;
    IF TG_OP <> 'DELETE' THEN after_row := to_jsonb(NEW); END IF;
    entity := coalesce(after_row,before_row);
    actor := nullif(current_setting('app.actor_id', true),'')::uuid;
    IF TG_TABLE_NAME = 'project' THEN shipment := (entity->>'id')::uuid;
    ELSIF TG_TABLE_NAME = 'document_version' THEN
        SELECT project_id INTO shipment FROM document_instance WHERE id=(entity->>'document_instance_id')::uuid;
    ELSE shipment := (entity->>'project_id')::uuid;
    END IF;
    INSERT INTO audit_log(project_id,entity_table,entity_id,operation,actor_id,actor_kind,request_id,before_json,after_json)
    VALUES (shipment,TG_TABLE_NAME,coalesce(entity->>'id',entity::text),TG_OP,actor,
        CASE WHEN actor IS NULL THEN 'SYSTEM' ELSE 'USER' END,
        nullif(current_setting('app.request_id', true),''),before_row,after_row);
    RETURN NULL;
END $$;
DO $$
DECLARE entity_table text;
BEGIN
    FOREACH entity_table IN ARRAY ARRAY[
        'app_user','role_assignment','organization','organization_role','port','product','product_category',
        'project','project_access','project_party','shipment_transport','container','fishing_vessel',
        'catch_certificate','catch_lot','processing_batch','raw_material_allocation','finished_product_quantity',
        'processing_output_adjustment','document_type','document_template_version','document_instance','document_version']
    LOOP
        EXECUTE format('CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION audit_business_change()', entity_table);
    END LOOP;
END $$;

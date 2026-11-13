-- Phase 2 master data. V1-V3 records and legacy address_text values are preserved.
-- ISO country names/codes are seeded from the host's iso-codes 3166-1 snapshot
-- (249 alpha-2 entries). Future ISO additions/removals require explicit additive
-- reference-data migrations; never remove a code referenced by stored records.
CREATE TABLE country (
    code varchar(2) PRIMARY KEY CHECK (code ~ '^[A-Z]{2}$'),
    name varchar(150) NOT NULL CHECK (btrim(name) <> ''),
    active boolean NOT NULL DEFAULT true
);
INSERT INTO country(code,name) VALUES
    ('AD','Andorra'),
    ('AE','United Arab Emirates'),
    ('AF','Afghanistan'),
    ('AG','Antigua and Barbuda'),
    ('AI','Anguilla'),
    ('AL','Albania'),
    ('AM','Armenia'),
    ('AO','Angola'),
    ('AQ','Antarctica'),
    ('AR','Argentina'),
    ('AS','American Samoa'),
    ('AT','Austria'),
    ('AU','Australia'),
    ('AW','Aruba'),
    ('AX','Åland Islands'),
    ('AZ','Azerbaijan'),
    ('BA','Bosnia and Herzegovina'),
    ('BB','Barbados'),
    ('BD','Bangladesh'),
    ('BE','Belgium'),
    ('BF','Burkina Faso'),
    ('BG','Bulgaria'),
    ('BH','Bahrain'),
    ('BI','Burundi'),
    ('BJ','Benin'),
    ('BL','Saint Barthélemy'),
    ('BM','Bermuda'),
    ('BN','Brunei Darussalam'),
    ('BO','Bolivia, Plurinational State of'),
    ('BQ','Bonaire, Sint Eustatius and Saba'),
    ('BR','Brazil'),
    ('BS','Bahamas'),
    ('BT','Bhutan'),
    ('BV','Bouvet Island'),
    ('BW','Botswana'),
    ('BY','Belarus'),
    ('BZ','Belize'),
    ('CA','Canada'),
    ('CC','Cocos (Keeling) Islands'),
    ('CD','Congo, The Democratic Republic of the'),
    ('CF','Central African Republic'),
    ('CG','Congo'),
    ('CH','Switzerland'),
    ('CI','Côte d''Ivoire'),
    ('CK','Cook Islands'),
    ('CL','Chile'),
    ('CM','Cameroon'),
    ('CN','China'),
    ('CO','Colombia'),
    ('CR','Costa Rica'),
    ('CU','Cuba'),
    ('CV','Cabo Verde'),
    ('CW','Curaçao'),
    ('CX','Christmas Island'),
    ('CY','Cyprus'),
    ('CZ','Czechia'),
    ('DE','Germany'),
    ('DJ','Djibouti'),
    ('DK','Denmark'),
    ('DM','Dominica'),
    ('DO','Dominican Republic'),
    ('DZ','Algeria'),
    ('EC','Ecuador'),
    ('EE','Estonia'),
    ('EG','Egypt'),
    ('EH','Western Sahara'),
    ('ER','Eritrea'),
    ('ES','Spain'),
    ('ET','Ethiopia'),
    ('FI','Finland'),
    ('FJ','Fiji'),
    ('FK','Falkland Islands (Malvinas)'),
    ('FM','Micronesia, Federated States of'),
    ('FO','Faroe Islands'),
    ('FR','France'),
    ('GA','Gabon'),
    ('GB','United Kingdom'),
    ('GD','Grenada'),
    ('GE','Georgia'),
    ('GF','French Guiana'),
    ('GG','Guernsey'),
    ('GH','Ghana'),
    ('GI','Gibraltar'),
    ('GL','Greenland'),
    ('GM','Gambia'),
    ('GN','Guinea'),
    ('GP','Guadeloupe'),
    ('GQ','Equatorial Guinea'),
    ('GR','Greece'),
    ('GS','South Georgia and the South Sandwich Islands'),
    ('GT','Guatemala'),
    ('GU','Guam'),
    ('GW','Guinea-Bissau'),
    ('GY','Guyana'),
    ('HK','Hong Kong'),
    ('HM','Heard Island and McDonald Islands'),
    ('HN','Honduras'),
    ('HR','Croatia'),
    ('HT','Haiti'),
    ('HU','Hungary'),
    ('ID','Indonesia'),
    ('IE','Ireland'),
    ('IL','Israel'),
    ('IM','Isle of Man'),
    ('IN','India'),
    ('IO','British Indian Ocean Territory'),
    ('IQ','Iraq'),
    ('IR','Iran, Islamic Republic of'),
    ('IS','Iceland'),
    ('IT','Italy'),
    ('JE','Jersey'),
    ('JM','Jamaica'),
    ('JO','Jordan'),
    ('JP','Japan'),
    ('KE','Kenya'),
    ('KG','Kyrgyzstan'),
    ('KH','Cambodia'),
    ('KI','Kiribati'),
    ('KM','Comoros'),
    ('KN','Saint Kitts and Nevis'),
    ('KP','Korea, Democratic People''s Republic of'),
    ('KR','Korea, Republic of'),
    ('KW','Kuwait'),
    ('KY','Cayman Islands'),
    ('KZ','Kazakhstan'),
    ('LA','Lao People''s Democratic Republic'),
    ('LB','Lebanon'),
    ('LC','Saint Lucia'),
    ('LI','Liechtenstein'),
    ('LK','Sri Lanka'),
    ('LR','Liberia'),
    ('LS','Lesotho'),
    ('LT','Lithuania'),
    ('LU','Luxembourg'),
    ('LV','Latvia'),
    ('LY','Libya'),
    ('MA','Morocco'),
    ('MC','Monaco'),
    ('MD','Moldova, Republic of'),
    ('ME','Montenegro'),
    ('MF','Saint Martin (French part)'),
    ('MG','Madagascar'),
    ('MH','Marshall Islands'),
    ('MK','North Macedonia'),
    ('ML','Mali'),
    ('MM','Myanmar'),
    ('MN','Mongolia'),
    ('MO','Macao'),
    ('MP','Northern Mariana Islands'),
    ('MQ','Martinique'),
    ('MR','Mauritania'),
    ('MS','Montserrat'),
    ('MT','Malta'),
    ('MU','Mauritius'),
    ('MV','Maldives'),
    ('MW','Malawi'),
    ('MX','Mexico'),
    ('MY','Malaysia'),
    ('MZ','Mozambique'),
    ('NA','Namibia'),
    ('NC','New Caledonia'),
    ('NE','Niger'),
    ('NF','Norfolk Island'),
    ('NG','Nigeria'),
    ('NI','Nicaragua'),
    ('NL','Netherlands'),
    ('NO','Norway'),
    ('NP','Nepal'),
    ('NR','Nauru'),
    ('NU','Niue'),
    ('NZ','New Zealand'),
    ('OM','Oman'),
    ('PA','Panama'),
    ('PE','Peru'),
    ('PF','French Polynesia'),
    ('PG','Papua New Guinea'),
    ('PH','Philippines'),
    ('PK','Pakistan'),
    ('PL','Poland'),
    ('PM','Saint Pierre and Miquelon'),
    ('PN','Pitcairn'),
    ('PR','Puerto Rico'),
    ('PS','Palestine, State of'),
    ('PT','Portugal'),
    ('PW','Palau'),
    ('PY','Paraguay'),
    ('QA','Qatar'),
    ('RE','Réunion'),
    ('RO','Romania'),
    ('RS','Serbia'),
    ('RU','Russian Federation'),
    ('RW','Rwanda'),
    ('SA','Saudi Arabia'),
    ('SB','Solomon Islands'),
    ('SC','Seychelles'),
    ('SD','Sudan'),
    ('SE','Sweden'),
    ('SG','Singapore'),
    ('SH','Saint Helena, Ascension and Tristan da Cunha'),
    ('SI','Slovenia'),
    ('SJ','Svalbard and Jan Mayen'),
    ('SK','Slovakia'),
    ('SL','Sierra Leone'),
    ('SM','San Marino'),
    ('SN','Senegal'),
    ('SO','Somalia'),
    ('SR','Suriname'),
    ('SS','South Sudan'),
    ('ST','Sao Tome and Principe'),
    ('SV','El Salvador'),
    ('SX','Sint Maarten (Dutch part)'),
    ('SY','Syrian Arab Republic'),
    ('SZ','Eswatini'),
    ('TC','Turks and Caicos Islands'),
    ('TD','Chad'),
    ('TF','French Southern Territories'),
    ('TG','Togo'),
    ('TH','Thailand'),
    ('TJ','Tajikistan'),
    ('TK','Tokelau'),
    ('TL','Timor-Leste'),
    ('TM','Turkmenistan'),
    ('TN','Tunisia'),
    ('TO','Tonga'),
    ('TR','Türkiye'),
    ('TT','Trinidad and Tobago'),
    ('TV','Tuvalu'),
    ('TW','Taiwan, Province of China'),
    ('TZ','Tanzania, United Republic of'),
    ('UA','Ukraine'),
    ('UG','Uganda'),
    ('UM','United States Minor Outlying Islands'),
    ('US','United States'),
    ('UY','Uruguay'),
    ('UZ','Uzbekistan'),
    ('VA','Holy See (Vatican City State)'),
    ('VC','Saint Vincent and the Grenadines'),
    ('VE','Venezuela, Bolivarian Republic of'),
    ('VG','Virgin Islands, British'),
    ('VI','Virgin Islands, U.S.'),
    ('VN','Viet Nam'),
    ('VU','Vanuatu'),
    ('WF','Wallis and Futuna'),
    ('WS','Samoa'),
    ('YE','Yemen'),
    ('YT','Mayotte'),
    ('ZA','South Africa'),
    ('ZM','Zambia'),
    ('ZW','Zimbabwe');

ALTER TABLE organization_role DROP CONSTRAINT organization_role_role_check;
ALTER TABLE organization_role ADD CONSTRAINT organization_role_role_check CHECK (role IN (
    'EXPORTER','PRODUCER','SUPPLIER','BUYER','CONSIGNEE','NOTIFY_PARTY',
    'FREIGHT_FORWARDER','SHIPPING_LINE','AUTHORITY','CLIENT','IMPORTER','CARRIER'));

ALTER TABLE organization
    ADD COLUMN trade_name varchar(250),
    ADD COLUMN tax_identifier varchar(100),
    ADD COLUMN email varchar(254),
    ADD COLUMN phone varchar(50);

CREATE TABLE city (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code varchar(2) NOT NULL REFERENCES country(code),
    name varchar(200) NOT NULL CHECK (btrim(name) <> ''),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, country_code),
    UNIQUE (country_code, name)
);
CREATE INDEX city_country_name_idx ON city(country_code, lower(name), id) WHERE active;

CREATE TABLE organization_address (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organization(id),
    address_kind varchar(20) NOT NULL DEFAULT 'BUSINESS'
        CHECK (address_kind IN ('BUSINESS','FACTORY','BILLING','OTHER')),
    country_code varchar(2) NOT NULL REFERENCES country(code),
    city_id uuid,
    manual_city varchar(200) CHECK (manual_city IS NULL OR btrim(manual_city) <> ''),
    address_line1 varchar(250) NOT NULL CHECK (btrim(address_line1) <> ''),
    address_line2 varchar(250),
    postal_code varchar(40),
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((city_id IS NOT NULL AND manual_city IS NULL) OR (city_id IS NULL AND manual_city IS NOT NULL)),
    FOREIGN KEY (city_id, country_code) REFERENCES city(id, country_code),
    UNIQUE (id, organization_id)
);
CREATE INDEX organization_address_org_idx ON organization_address(organization_id, active, address_kind);

CREATE TABLE factory (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id uuid NOT NULL REFERENCES organization(id),
    name varchar(250) NOT NULL CHECK (btrim(name) <> ''),
    address_id uuid,
    establishment_details text,
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (address_id, company_id) REFERENCES organization_address(id, organization_id),
    UNIQUE (id, company_id)
);
CREATE INDEX factory_company_idx ON factory(company_id, active, lower(name), id);

-- Approval references are immutable revisions. Insert a successor with the prior
-- expiry captured at reissue time; never UPDATE/DELETE a recorded identifier. Only
-- one open-ended reference is allowed per factory at any point in this history.
CREATE TABLE factory_approval (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    factory_id uuid NOT NULL REFERENCES factory(id),
    issuing_authority varchar(250) NOT NULL CHECK (btrim(issuing_authority) <> ''),
    numbering_system varchar(120) NOT NULL CHECK (btrim(numbering_system) <> ''),
    issuing_country_code varchar(2) NOT NULL REFERENCES country(code),
    approval_reference varchar(150) NOT NULL CHECK (btrim(approval_reference) <> ''),
    valid_from date,
    valid_until date,
    recorded_at timestamptz NOT NULL DEFAULT now(),
    recorded_by uuid REFERENCES app_user(id),
    CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from <= valid_until),
    UNIQUE (issuing_authority, numbering_system, issuing_country_code, approval_reference),
    UNIQUE (id, factory_id)
);
CREATE UNIQUE INDEX factory_one_open_approval_idx ON factory_approval(factory_id) WHERE valid_until IS NULL;
CREATE INDEX factory_approval_lookup_idx ON factory_approval(factory_id, recorded_at DESC, id);

CREATE TRIGGER immutable_factory_approval BEFORE UPDATE OR DELETE ON factory_approval
    FOR EACH ROW EXECUTE FUNCTION reject_immutable_mutation();

ALTER TABLE product
    ADD COLUMN description text,
    ADD COLUMN processing_method varchar(150),
    ADD COLUMN storage_requirements varchar(500),
    ADD COLUMN default_measurement_unit varchar(30) NOT NULL DEFAULT 'KG'
        CHECK (btrim(default_measurement_unit) <> '');

-- Product taxonomy is distinct from product_category, which existing shipment
-- quantities use as a per-product size/packing grade.
CREATE TABLE product_taxonomy_category (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(80) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name varchar(200) NOT NULL CHECK (btrim(name) <> ''),
    description varchar(500),
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE product_taxonomy_assignment (
    product_id uuid NOT NULL REFERENCES product(id),
    category_id uuid NOT NULL REFERENCES product_taxonomy_category(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (product_id, category_id)
);
CREATE INDEX product_taxonomy_category_idx ON product_taxonomy_assignment(category_id, product_id);

-- Project links select a reusable product and configurable grade while keeping
-- variable shipment quantities and commercial facts separate from master data.
CREATE TABLE project_product (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    product_id uuid NOT NULL REFERENCES product(id),
    product_category_id uuid,
    carton_count integer CHECK (carton_count IS NULL OR carton_count > 0),
    quantity numeric(18,3) CHECK (quantity IS NULL OR quantity > 0),
    quantity_unit varchar(30),
    net_weight_kg numeric(18,3) CHECK (net_weight_kg IS NULL OR net_weight_kg > 0),
    gross_weight_kg numeric(18,3) CHECK (gross_weight_kg IS NULL OR gross_weight_kg > 0),
    unit_price numeric(18,4) CHECK (unit_price IS NULL OR unit_price >= 0),
    currency varchar(3) CHECK (currency IS NULL OR currency ~ '^[A-Z]{3}$'),
    lot_reference varchar(150),
    processing_information text,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((quantity IS NULL AND quantity_unit IS NULL) OR
           (quantity IS NOT NULL AND quantity_unit IS NOT NULL AND btrim(quantity_unit) <> '')),
    CHECK ((unit_price IS NULL AND currency IS NULL) OR (unit_price IS NOT NULL AND currency IS NOT NULL)),
    CHECK (net_weight_kg IS NULL OR gross_weight_kg IS NULL OR gross_weight_kg >= net_weight_kg),
    FOREIGN KEY (product_category_id, product_id) REFERENCES product_category(id, product_id)
);
CREATE INDEX project_product_project_idx ON project_product(project_id, product_id, product_category_id);

-- Link the selected factory to the producer company through an explicit role and
-- composite FK. A shipment may omit this while it remains an incomplete draft.
CREATE TABLE project_factory (
    project_id uuid PRIMARY KEY REFERENCES project(id),
    factory_id uuid NOT NULL,
    producer_company_id uuid NOT NULL,
    role varchar(20) NOT NULL DEFAULT 'PRODUCER' CHECK (role = 'PRODUCER'),
    FOREIGN KEY (factory_id, producer_company_id) REFERENCES factory(id, company_id),
    FOREIGN KEY (project_id, producer_company_id, role)
        REFERENCES project_party(project_id, organization_id, role)
);
CREATE INDEX project_factory_factory_idx ON project_factory(factory_id, project_id);

DO $$
DECLARE entity_table text;
BEGIN
    FOREACH entity_table IN ARRAY ARRAY[
        'city','organization_address','factory','factory_approval','product_taxonomy_category',
        'product_taxonomy_assignment','project_product','project_factory'
    ] LOOP
        EXECUTE format('CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION audit_business_change()', entity_table);
    END LOOP;
END $$;


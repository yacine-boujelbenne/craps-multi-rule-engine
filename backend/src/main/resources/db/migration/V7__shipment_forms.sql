CREATE TABLE shipment_form (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id uuid NOT NULL REFERENCES project(id),
    form_key varchar(40) NOT NULL CHECK (form_key IN ('INVOICE','PACKING_LIST','CATCH_CERTIFICATES','PROCESSING','TRANSPORT','HEALTH_CERTIFICATE','BILL_OF_LADING','CERTIFICATE_OF_ORIGIN','SHIPPING_LIST')),
    data jsonb NOT NULL CHECK (jsonb_typeof(data) = 'array'),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE(project_id, form_key)
);
CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON shipment_form
FOR EACH ROW EXECUTE FUNCTION audit_business_change();

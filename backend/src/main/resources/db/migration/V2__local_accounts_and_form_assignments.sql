-- Local accounts do not require a third-party identity; preserve existing mappings.
ALTER TABLE app_user ALTER COLUMN external_subject DROP NOT NULL;
ALTER TABLE app_user ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0);

-- Keep secrets out of app_user and its generic full-row audit trigger.
CREATE TABLE local_account (
    user_id uuid PRIMARY KEY REFERENCES app_user(id),
    username varchar(254) NOT NULL UNIQUE
        CHECK (username ~ '^[a-z0-9][a-z0-9._@+-]{2,253}$'),
    password_hash text NOT NULL CHECK (btrim(password_hash) <> ''),
    must_change_password boolean NOT NULL DEFAULT true,
    credential_version bigint NOT NULL DEFAULT 0 CHECK (credential_version >= 0),
    password_changed_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE project_form_assignment (
    project_id uuid NOT NULL,
    user_id uuid NOT NULL,
    form_key varchar(40) NOT NULL CHECK (form_key IN (
        'PROJECT_SETUP','INVOICE','PACKING_LIST','CATCH_CERTIFICATES','PROCESSING',
        'TRANSPORT','HEALTH_CERTIFICATE','BILL_OF_LADING','CERTIFICATE_OF_ORIGIN','SHIPPING_LIST')),
    assigned_by uuid NOT NULL REFERENCES app_user(id),
    assigned_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id,user_id,form_key),
    FOREIGN KEY (project_id,user_id) REFERENCES project_access(project_id,user_id)
);
CREATE INDEX project_form_assignment_user_idx ON project_form_assignment(user_id,project_id);
CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON project_form_assignment
    FOR EACH ROW EXECUTE FUNCTION audit_business_change();

CREATE FUNCTION audit_local_account_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE before_row jsonb; after_row jsonb; entity jsonb; actor uuid; changed boolean;
BEGIN
    IF TG_OP <> 'INSERT' THEN before_row := to_jsonb(OLD) - 'password_hash'; END IF;
    IF TG_OP <> 'DELETE' THEN after_row := to_jsonb(NEW) - 'password_hash'; END IF;
    changed := TG_OP <> 'UPDATE';
    IF TG_OP = 'UPDATE' THEN changed := NEW.password_hash IS DISTINCT FROM OLD.password_hash; END IF;
    IF after_row IS NOT NULL THEN
        after_row := after_row || jsonb_build_object('password_changed',changed);
    END IF;
    entity := coalesce(after_row,before_row);
    actor := nullif(current_setting('app.actor_id',true),'')::uuid;
    INSERT INTO audit_log(entity_table,entity_id,operation,actor_id,actor_kind,request_id,before_json,after_json)
    VALUES ('local_account',entity->>'user_id',TG_OP,actor,
        CASE WHEN actor IS NULL THEN 'SYSTEM' ELSE 'USER' END,
        nullif(current_setting('app.request_id',true),''),before_row,after_row);
    RETURN NULL;
END $$;
CREATE TRIGGER audit_local_account AFTER INSERT OR UPDATE OR DELETE ON local_account
    FOR EACH ROW EXECUTE FUNCTION audit_local_account_change();

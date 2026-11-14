-- Keep V4 identifiers immutable; effective expiry is derived from a successor.
ALTER TABLE factory_approval
    ADD COLUMN supersedes_approval_id uuid,
    ADD CONSTRAINT factory_approval_predecessor_fk
        FOREIGN KEY (supersedes_approval_id, factory_id) REFERENCES factory_approval(id, factory_id),
    ADD CONSTRAINT factory_approval_one_successor UNIQUE (supersedes_approval_id),
    ADD CONSTRAINT factory_approval_successor_start CHECK
        (supersedes_approval_id IS NULL OR (supersedes_approval_id <> id AND valid_from IS NOT NULL));

DROP INDEX factory_one_open_approval_idx;

CREATE VIEW factory_approval_effective AS
SELECT approval.*,
       CASE WHEN successor.valid_from IS NULL THEN approval.valid_until
            ELSE LEAST(approval.valid_until, successor.valid_from - 1)
       END AS effective_valid_until
FROM factory_approval approval
LEFT JOIN factory_approval successor ON successor.supersedes_approval_id = approval.id;

-- Approval writes use READ COMMITTED so each statement after the lock sees
-- committed competing inserts. Historical overlapping rows remain readable;
-- selection must reject ambiguity rather than choose an arbitrary reference.
CREATE FUNCTION validate_factory_approval_insert() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE predecessor factory_approval%ROWTYPE;
BEGIN
    PERFORM 1 FROM factory WHERE id = NEW.factory_id FOR UPDATE;
    IF NEW.valid_from IS NOT NULL AND NEW.valid_until IS NOT NULL AND NEW.valid_until < NEW.valid_from THEN
        RAISE EXCEPTION 'Approval validity end precedes its start' USING ERRCODE = '23514';
    END IF;
    IF NEW.supersedes_approval_id IS NOT NULL THEN
        SELECT * INTO predecessor FROM factory_approval
        WHERE id = NEW.supersedes_approval_id AND factory_id = NEW.factory_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Approval predecessor must belong to this factory' USING ERRCODE = '23503';
        END IF;
        IF EXISTS (SELECT 1 FROM factory_approval WHERE supersedes_approval_id = predecessor.id) THEN
            RAISE EXCEPTION 'Approval already has a successor' USING ERRCODE = '23505';
        END IF;
        IF NEW.valid_from IS NULL OR (predecessor.valid_from IS NOT NULL AND NEW.valid_from <= predecessor.valid_from) THEN
            RAISE EXCEPTION 'Successor must start after its predecessor' USING ERRCODE = '23514';
        END IF;
    END IF;
    IF EXISTS (
        SELECT 1 FROM factory_approval_effective existing
        WHERE existing.factory_id = NEW.factory_id
          AND (NEW.supersedes_approval_id IS NULL OR existing.id <> NEW.supersedes_approval_id)
          AND daterange(existing.valid_from, existing.effective_valid_until, '[]')
              && daterange(NEW.valid_from, NEW.valid_until, '[]')
    ) THEN
        RAISE EXCEPTION 'Approval effective validity overlaps another reference' USING ERRCODE = '23P01';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER validate_factory_approval BEFORE INSERT ON factory_approval
    FOR EACH ROW EXECUTE FUNCTION validate_factory_approval_insert();

-- Preserve the configured values actually applied to shipment lines. Existing
-- rows stay NULL: today's directory values cannot reconstruct historical facts.
ALTER TABLE project_product
    ADD COLUMN applied_grade_version bigint CHECK (applied_grade_version >= 0),
    ADD COLUMN applied_grade_code varchar(80),
    ADD COLUMN applied_grade_name varchar(200),
    ADD COLUMN applied_carton_net_kg numeric(18,3) CHECK (applied_carton_net_kg > 0);

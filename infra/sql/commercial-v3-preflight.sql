-- Read-only diagnostics before V3 on an existing V1/V2 database.
-- No rows means these known duplicate-cardinality conflicts were not found.
-- V3 still enforces the constraints if data changes after this check.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY;

SELECT 'commercial_party' AS conflict_kind,
       project_id,
       role::text AS record_type,
       count(*) AS conflicting_records
FROM project_party
WHERE role IN ('SUPPLIER','CLIENT','IMPORTER','CONSIGNEE')
GROUP BY project_id,role
HAVING count(*) > 1
UNION ALL
SELECT 'catch_certificate',
       project_id,
       'CC',
       count(*)
FROM catch_certificate
GROUP BY project_id
HAVING count(*) > 1
UNION ALL
SELECT 'logical_document',
       project_id,
       document_type_code::text,
       count(*)
FROM document_instance
WHERE document_type_code IN ('HC','CC')
GROUP BY project_id,document_type_code
HAVING count(*) > 1
ORDER BY conflict_kind,project_id,record_type;

ROLLBACK;

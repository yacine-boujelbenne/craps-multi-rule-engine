package com.exportdocs.infrastructure;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Savepoint;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class FoundationsIT {
    private static final String PROJECT = "00000000-0000-0000-0000-000000000100";
    private static final String OTHER_PROJECT = "00000000-0000-0000-0000-000000000101";
    private static final String BATCH = "00000000-0000-0000-0000-000000000060";
    private static final String LOT = "00000000-0000-0000-0000-000000000050";
    private static final String USER = "00000000-0000-0000-0000-000000000001";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired MockMvc mvc;
    @Autowired DataSource dataSource;

    @Test
    void flywayValidatesAndSecondMigrationRunDoesNothing() {
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void referenceOnlyDraftAndUniqueReferenceAreEnforced() {
        jdbc.update("INSERT INTO project(reference) VALUES ('ONLY-REFERENCE')");
        assertThat(jdbc.queryForMap("SELECT status, version, destination_country_code FROM project WHERE reference='ONLY-REFERENCE'"))
                .containsEntry("status", "DRAFT_DATA").containsEntry("version", 0L)
                .containsEntry("destination_country_code", null);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO project(reference) VALUES ('ONLY-REFERENCE')"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void unknownStatusIsRejected() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO project(reference,status) VALUES ('BAD-STATUS','FINALIZED')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void lowercaseReferenceCannotBypassCanonicalUniqueness() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO project(reference) VALUES ('lowercase')"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void transportHasTheSameIdentityAsItsProject() {
        jdbc.update("INSERT INTO project(id,reference) VALUES (?::uuid,'TRANSPORT-TEST')", PROJECT);
        jdbc.update("INSERT INTO shipment_transport(project_id) VALUES (?::uuid)", PROJECT);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO shipment_transport(project_id) VALUES (?::uuid)", PROJECT))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void rawAndFinishedQuantitiesReconcileWithoutEqualityAndWithoutJoinMultiplication() {
        var balance = jdbc.queryForMap("SELECT * FROM processing_batch_balance WHERE processing_batch_id=?::uuid", BATCH);
        assertThat((BigDecimal) balance.get("input_raw_kg")).isEqualByComparingTo("22000");
        assertThat((BigDecimal) balance.get("finished_net_kg")).isEqualByComparingTo("19200");
        assertThat((BigDecimal) balance.get("accounted_other_output_kg")).isEqualByComparingTo("2800");
        assertThat((BigDecimal) balance.get("unaccounted_kg")).isEqualByComparingTo("0");
        assertThat((BigDecimal) balance.get("yield_percent")).isEqualByComparingTo("87.2727");
        assertThat(jdbc.queryForObject("SELECT raw_weight_kg FROM catch_certificate_totals", BigDecimal.class))
                .isEqualByComparingTo("22000");
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void allocationCannotCrossProjects() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO raw_material_allocation(project_id,catch_lot_id,processing_batch_id,raw_weight_kg)
                VALUES (?::uuid,?::uuid,'00000000-0000-0000-0000-000000000061',1)
                """, OTHER_PROJECT, LOT)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void catchCannotBeOverallocatedToAnotherBatch() {
        jdbc.update("INSERT INTO processing_batch(id,project_id,batch_reference) VALUES ('00000000-0000-0000-0000-000000000062',?::uuid,'SECOND-BATCH')", PROJECT);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO raw_material_allocation(project_id,catch_lot_id,processing_batch_id,raw_weight_kg)
                VALUES (?::uuid,?::uuid,'00000000-0000-0000-0000-000000000062',0.001)
                """, PROJECT, LOT)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void allocatedCatchCannotBeReducedBelowItsAllocatedQuantity() {
        assertThatThrownBy(() -> jdbc.update("UPDATE catch_lot SET raw_weight_kg=11999 WHERE id=?::uuid", LOT))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void grossWeightCannotBeLessThanFinishedNetWeight() {
        assertThatThrownBy(() -> jdbc.update("UPDATE finished_product_quantity SET gross_weight_kg=net_weight_kg-1"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void partyMustHaveTheAssignedDirectoryRole() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO project_party(project_id,organization_id,role)
                VALUES (?::uuid,'00000000-0000-0000-0000-000000000010','CONSIGNEE')
                """, PROJECT)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void sealAndLateLockRemainIndependent() {
        jdbc.update("INSERT INTO container(id,project_id,seal_number) VALUES (?::uuid,?::uuid,'SEAL-ABC')", LOT, PROJECT);
        assertThat(jdbc.queryForMap("SELECT seal_number,lock_number FROM container WHERE id=?::uuid", LOT))
                .containsEntry("seal_number", "SEAL-ABC").containsEntry("lock_number", null);
        jdbc.update("UPDATE container SET lock_number='LOCK-XYZ' WHERE id=?::uuid", LOT);
        assertThat(jdbc.queryForMap("SELECT seal_number,lock_number FROM container WHERE id=?::uuid", LOT))
                .containsEntry("seal_number", "SEAL-ABC").containsEntry("lock_number", "LOCK-XYZ");
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void auditRecordsActorAndRollsBackTogetherWithBusinessData() throws Exception {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        Savepoint savepoint = connection.setSavepoint();
        jdbc.update("UPDATE project SET notes='changed',version=version+1 WHERE id=?::uuid", PROJECT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE project_id=?::uuid AND operation='UPDATE' AND actor_id=?::uuid", Long.class, PROJECT, USER)).isEqualTo(1);
        connection.rollback(savepoint);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE project_id=?::uuid AND operation='UPDATE'", Long.class, PROJECT)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM project WHERE id=?::uuid", Long.class, PROJECT)).isZero();
        connection.releaseSavepoint(savepoint);
    }

    @Test
    @Sql("/sql/foundation-fixture.sql")
    void documentSnapshotBecomesStaleWithoutMutation() {
        jdbc.update("""
                INSERT INTO document_template_version(id,document_type_code,template_key,revision,content_sha256)
                VALUES (?::uuid,'HC','fictional-template',1,repeat('a',64))
                """, LOT);
        jdbc.update("INSERT INTO document_instance(id,project_id,document_type_code) VALUES (?::uuid,?::uuid,'HC')", BATCH, PROJECT);
        jdbc.update("""
                INSERT INTO document_version(id,document_instance_id,document_type_code,template_version_id,revision,variant,source_revision,snapshot_json,content_sha256,artifact_key)
                VALUES (?::uuid,?::uuid,'HC',?::uuid,1,'UNSIGNED_FOR_SIGNATURE',0,'{"reference":"FIXTURE-001"}',repeat('b',64),'fictional/hc.pdf')
                """, PROJECT, BATCH, LOT);
        assertThat(jdbc.queryForObject("SELECT stale FROM document_version_freshness", Boolean.class)).isFalse();
        jdbc.update("UPDATE project SET version=version+1 WHERE id=?::uuid", PROJECT);
        assertThat(jdbc.queryForObject("SELECT stale FROM document_version_freshness", Boolean.class)).isTrue();
        assertThatThrownBy(() -> jdbc.update("UPDATE document_version SET artifact_key='replacement.pdf'"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void auditHistoryCannotBeChanged() {
        jdbc.update("INSERT INTO project(reference) VALUES ('AUDIT-TEST')");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_log"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void healthIsPublicButBusinessRoutesAreDenied() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/projects")).andExpect(status().is4xxClientError());
    }
}

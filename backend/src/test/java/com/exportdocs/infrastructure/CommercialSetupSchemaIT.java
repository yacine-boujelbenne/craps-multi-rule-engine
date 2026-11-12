package com.exportdocs.infrastructure;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
class CommercialSetupSchemaIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {"SUPPLIER", "CLIENT", "IMPORTER", "CONSIGNEE"})
    void shipmentHasAtMostOneCompanyForEachCommercialRole(String role) {
        UUID project = project();
        UUID first = organization(role);
        UUID second = organization(role);
        jdbc.update("INSERT INTO project_party VALUES (?,?,?)", project, first, role);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO project_party VALUES (?,?,?)", project, second, role))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void oneCompanyMayHaveSeveralIndependentCommercialRoles() {
        UUID project = project();
        UUID company = organization("CLIENT");
        for (String role : new String[]{"IMPORTER", "CONSIGNEE"})
            jdbc.update("INSERT INTO organization_role VALUES (?,?)", company, role);
        for (String role : new String[]{"CLIENT", "IMPORTER", "CONSIGNEE"})
            jdbc.update("INSERT INTO project_party VALUES (?,?,?)", project, company, role);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_party WHERE project_id=?", Long.class, project)).isEqualTo(3);
    }

    @Test
    void gradeDefaultsPreserveExactDecimalsAndHaveTheirOwnVersion() {
        UUID product = product();
        UUID grade = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product_category(id,product_id,code,name,default_carton_net_kg,default_price_per_kg,default_currency)
                VALUES (?,?,'TEST-GRADE','Fictional grade',12.345,4.1234,'USD')
                """, grade, product);
        assertThat(jdbc.queryForObject("SELECT default_carton_net_kg FROM product_category WHERE id=?", BigDecimal.class, grade))
                .isEqualByComparingTo("12.345");
        assertThat(jdbc.queryForObject("SELECT default_price_per_kg FROM product_category WHERE id=?", BigDecimal.class, grade))
                .isEqualByComparingTo("4.1234");
        assertThat(jdbc.queryForObject("SELECT version FROM product_category WHERE id=?", Long.class, grade)).isZero();
    }

    @Test
    void priceDefaultRequiresItsCurrency() {
        UUID product = product();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO product_category(product_id,code,name,default_price_per_kg) VALUES (?,'BAD','Fictional grade',4.1234)", product))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void boxWeightDefaultMustBePositive() {
        UUID product = product();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO product_category(product_id,code,name,default_carton_net_kg) VALUES (?,'BAD','Fictional grade',0)", product))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void aShipmentCannotHaveTwoIndependentCatchCertificates() {
        UUID project = project();
        jdbc.update("INSERT INTO catch_certificate(project_id,internal_reference) VALUES (?,'CC-A')", project);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO catch_certificate(project_id,internal_reference) VALUES (?,'CC-B')", project))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"HC", "CC"})
    void logicalCertificateCardinalityDoesNotDependOnItsLabel(String kind) {
        UUID project = project();
        jdbc.update("INSERT INTO document_instance(project_id,document_type_code,logical_key) VALUES (?,?,'first')", project, kind);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO document_instance(project_id,document_type_code,logical_key) VALUES (?,?,'second')", project, kind))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void directPackedQuantitiesDoNotRequireAnArtificialProcessingBatch() {
        UUID project = project();
        UUID product = product();
        UUID grade = UUID.randomUUID();
        jdbc.update("INSERT INTO product_category(id,product_id,code,name) VALUES (?,?,'TEST','Fictional grade')", grade, product);
        jdbc.update("INSERT INTO finished_product_quantity(project_id,product_id,product_category_id,carton_count,net_weight_kg,gross_weight_kg) VALUES (?,?,?,7,84,90)", project, product, grade);
        assertThat(jdbc.queryForObject("SELECT processing_batch_id FROM finished_product_quantity WHERE project_id=?", UUID.class, project)).isNull();
    }

    private UUID project() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO project(id,reference) VALUES (?,?)", id, "TEST-" + id.toString().toUpperCase(java.util.Locale.ROOT));
        return id;
    }
    private UUID organization(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO organization(id,legal_name) VALUES (?,'Fictional partner')", id);
        jdbc.update("INSERT INTO organization_role VALUES (?,?)", id, role);
        return id;
    }
    private UUID product() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO product(id,code,name) VALUES (?,?,'Fictional product')", id, "TEST-" + id);
        return id;
    }
}

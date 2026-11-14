package com.exportdocs.infrastructure;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
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
class MasterDataSchemaIT {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    @Test void earlierCompanyProjectDataAndLegacyAddressTextSurvive() throws Exception {
        String schema = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("3").load().migrate();
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                connection.setSchema(schema);
                statement.executeUpdate("INSERT INTO organization(id,legal_name,address_text) VALUES ('00000000-0000-0000-0000-000000000001','Legacy company','Original address')");
                statement.executeUpdate("INSERT INTO organization_role(organization_id,role) VALUES ('00000000-0000-0000-0000-000000000001','EXPORTER')");
                statement.executeUpdate("INSERT INTO project(id,reference) VALUES ('00000000-0000-0000-0000-000000000002','LEGACY-UPGRADE')");
                statement.executeUpdate("INSERT INTO project_party(project_id,organization_id,role) VALUES ('00000000-0000-0000-0000-000000000002','00000000-0000-0000-0000-000000000001','EXPORTER')");
                Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
                try (var result = statement.executeQuery("SELECT o.address_text,p.reference FROM organization o JOIN project_party pp ON pp.organization_id=o.id JOIN project p ON p.id=pp.project_id")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString("address_text")).isEqualTo("Original address");
                    assertThat(result.getString("reference")).isEqualTo("LEGACY-UPGRADE");
                    assertThat(result.next()).isFalse();
                }
            }
        } finally {
            try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    @Test void countriesAreCompleteAndAddressCityCountryAndTextPostalCodeAreConstrained() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM country", Integer.class)).isGreaterThanOrEqualTo(249);
        UUID company = company(), city = UUID.randomUUID();
        jdbc.update("INSERT INTO city(id,country_code,name) VALUES (?,'TN','Example City')", city);
        jdbc.update("INSERT INTO organization_address(organization_id,country_code,city_id,address_line1,postal_code) VALUES (?,'TN',?,'Road 1','0010')", company, city);
        assertThat(jdbc.queryForObject("SELECT postal_code FROM organization_address WHERE organization_id=?", String.class, company)).isEqualTo("0010");
        jdbc.update("INSERT INTO organization_address(organization_id,country_code,manual_city,address_line1) VALUES (?,'TN','Manual City','Road 2')", company);
        rejects(() -> jdbc.update("INSERT INTO city(country_code,name) VALUES ('ZZ','Invalid Country City')"));
        rejects(() -> jdbc.update("INSERT INTO organization_address(organization_id,country_code,city_id,address_line1) VALUES (?,'FR',?,'Road')", company, city));
    }

    @Test void companyCanOwnSeveralFactoriesAndApprovalNumberIsUniqueInItsNamespace() {
        UUID company = company(), one = factory(company), two = factory(company);
        approval(one, "Authority", "System", "TN", "APP-001");
        approval(two, "Authority", "System", "TN", "APP-002");
        rejects(() -> approval(two, "Authority", "System", "TN", "APP-001"));
    }

    @Test void factoryApprovalSupersessionPreservesHistoryAndRejectsOverlaps() {
        UUID factory = factory(company());
        UUID approval = approval(factory, "Authority", "System", "TN", "APP-003");
        rejects(() -> jdbc.update("UPDATE factory_approval SET approval_reference='CHANGED' WHERE id=?", approval));
        rejects(() -> approval(factory, "Authority", "System", "TN", "APP-004"));
        rejects(() -> jdbc.update("INSERT INTO factory_approval(factory_id,issuing_authority,numbering_system,issuing_country_code,approval_reference,supersedes_approval_id) VALUES (?,'Authority','System','TN','NO-START',?)", factory, approval));
        UUID other = factory(company());
        rejects(() -> jdbc.update("INSERT INTO factory_approval(factory_id,issuing_authority,numbering_system,issuing_country_code,approval_reference,supersedes_approval_id,valid_from) VALUES (?,'Authority','System','TN','WRONG-FACTORY',?,DATE '2026-11-14')", other, approval));
        UUID successor = jdbc.queryForObject("INSERT INTO factory_approval(factory_id,issuing_authority,numbering_system,issuing_country_code,approval_reference,supersedes_approval_id,valid_from) VALUES (?,'Authority','System','TN','APP-004',?,DATE '2026-11-14') RETURNING id", UUID.class, factory, approval);
        assertThat(jdbc.queryForObject("SELECT valid_until FROM factory_approval WHERE id=?", java.sql.Date.class, approval)).isNull();
        assertThat(jdbc.queryForObject("SELECT effective_valid_until FROM factory_approval_effective WHERE id=?", java.sql.Date.class, approval)).isEqualTo(java.sql.Date.valueOf("2026-11-13"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM factory_approval_effective WHERE factory_id=? AND (valid_from IS NULL OR valid_from<=DATE '2026-11-14') AND (effective_valid_until IS NULL OR effective_valid_until>=DATE '2026-11-14')", Long.class, factory)).isEqualTo(1);
        rejects(() -> jdbc.update("INSERT INTO factory_approval(factory_id,issuing_authority,numbering_system,issuing_country_code,approval_reference,supersedes_approval_id,valid_from) VALUES (?,'Authority','System','TN','BRANCH',?,DATE '2026-11-15')", factory, approval));
        rejects(() -> jdbc.update("INSERT INTO factory_approval(factory_id,issuing_authority,numbering_system,issuing_country_code,approval_reference,supersedes_approval_id,valid_from) VALUES (?,'Authority','System','TN','BACKWARDS',?,DATE '2026-11-14')", factory, successor));
    }

    @Test void projectsKeepClientAndConsigneeSeparateAndProductSelectionIsShipmentSpecific() {
        UUID project = project(), client = company(), consignee = company(), product = product();
        jdbc.update("INSERT INTO organization_role(organization_id,role) VALUES (?,'CLIENT'),(?,'CONSIGNEE')", client, consignee);
        jdbc.update("INSERT INTO project_party(project_id,organization_id,role) VALUES (?,?,'CLIENT'),(?,?,'CONSIGNEE')", project, client, project, consignee);
        UUID category = jdbc.queryForObject("INSERT INTO product_category(product_id,code,name) VALUES (?,'CUSTOM','Custom grade') RETURNING id", UUID.class, product);
        jdbc.update("INSERT INTO project_product(project_id,product_id,product_category_id,carton_count,net_weight_kg,gross_weight_kg,unit_price,currency) VALUES (?,?,?,10,120,132,3.2500,'USD')", project, product, category);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT organization_id) FROM project_party WHERE project_id=? AND role IN ('CLIENT','CONSIGNEE')", Long.class, project)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT carton_count FROM project_product WHERE project_id=?", Integer.class, project)).isEqualTo(10);
        jdbc.update("UPDATE project_product SET applied_grade_version=0,applied_grade_code='CUSTOM',applied_grade_name='Custom grade',applied_carton_net_kg=12 WHERE project_id=?", project);
        jdbc.update("UPDATE product_category SET name='New default',default_carton_net_kg=15,default_price_per_kg=9,default_currency='EUR',version=version+1 WHERE id=?", category);
        var applied = jdbc.queryForMap("SELECT applied_grade_name,applied_carton_net_kg,unit_price,currency FROM project_product WHERE project_id=?", project);
        assertThat(applied.get("applied_grade_name")).isEqualTo("Custom grade");
        assertThat((java.math.BigDecimal) applied.get("applied_carton_net_kg")).isEqualByComparingTo("12");
        assertThat((java.math.BigDecimal) applied.get("unit_price")).isEqualByComparingTo("3.2500");
        assertThat(applied.get("currency")).isEqualTo("USD");
    }

    private void rejects(Runnable write) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            var savepoint = connection.setSavepoint();
            try {
                assertThatThrownBy(write::run).isInstanceOf(DataIntegrityViolationException.class);
            } finally {
                connection.rollback(savepoint);
                connection.releaseSavepoint(savepoint);
            }
            return null;
        });
    }

    private UUID company() { return jdbc.queryForObject("INSERT INTO organization(legal_name) VALUES (?) RETURNING id", UUID.class, "Fictional " + UUID.randomUUID()); }
    private UUID project() { return jdbc.queryForObject("INSERT INTO project(reference) VALUES (?) RETURNING id", UUID.class, "MD-" + UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT)); }
    private UUID product() { return jdbc.queryForObject("INSERT INTO product(code,name) VALUES (?,?) RETURNING id", UUID.class, "P-" + UUID.randomUUID(), "Fictional product"); }
    private UUID factory(UUID company) { return jdbc.queryForObject("INSERT INTO factory(company_id,name) VALUES (?,?) RETURNING id", UUID.class, company, "Factory " + UUID.randomUUID()); }
    private UUID approval(UUID factory, String authority, String system, String country, String reference) {
        return jdbc.queryForObject("INSERT INTO factory_approval(factory_id,issuing_authority,numbering_system,issuing_country_code,approval_reference) VALUES (?,?,?,?,?) RETURNING id", UUID.class, factory, authority, system, country, reference);
    }
}

package com.exportdocs.infrastructure;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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
class MasterDataSchemaIT {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void database(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        p.add("spring.datasource.username", POSTGRES::getUsername);
        p.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired JdbcTemplate jdbc;

    @Test void earlierCompanyProjectDataAndLegacyAddressTextSurvive() {
        UUID company = company(), project = project();
        jdbc.update("INSERT INTO organization_role(organization_id,role) VALUES (?,'EXPORTER')", company);
        jdbc.update("INSERT INTO project_party(project_id,organization_id,role) VALUES (?,?,'EXPORTER')", project, company);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM organization WHERE id=?", Long.class, company)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_party WHERE project_id=? AND organization_id=?", Long.class, project, company)).isEqualTo(1);
    }

    @Test void countriesAreCompleteAndAddressCityCountryAndTextPostalCodeAreConstrained() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM country", Integer.class)).isGreaterThanOrEqualTo(249);
        UUID company = company(), city = UUID.randomUUID();
        jdbc.update("INSERT INTO city(id,country_code,name) VALUES (?,'TN','Example City')", city);
        jdbc.update("INSERT INTO organization_address(organization_id,country_code,city_id,address_line1,postal_code) VALUES (?,'TN',?,'Road 1','0010')", company, city);
        assertThat(jdbc.queryForObject("SELECT postal_code FROM organization_address WHERE organization_id=?", String.class, company)).isEqualTo("0010");
        jdbc.update("INSERT INTO organization_address(organization_id,country_code,manual_city,address_line1) VALUES (?,'TN','Manual City','Road 2')", company);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO city(country_code,name) VALUES ('ZZ','Invalid Country City')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO organization_address(organization_id,country_code,city_id,address_line1) VALUES (?,'FR',?,'Road')", company, city))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void companyCanOwnSeveralFactoriesAndApprovalNumberIsUniqueInItsNamespace() {
        UUID company = company(), one = factory(company), two = factory(company);
        approval(one, "Authority", "System", "TN", "APP-001");
        approval(two, "Authority", "System", "TN", "APP-002");
        assertThatThrownBy(() -> approval(two, "Authority", "System", "TN", "APP-001"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void factoryApprovalRowsAreImmutableAndOnlyOneOpenApprovalIsAllowed() {
        UUID factory = factory(company());
        UUID approval = approval(factory, "Authority", "System", "TN", "APP-003");
        assertThatThrownBy(() -> jdbc.update("UPDATE factory_approval SET approval_reference='CHANGED' WHERE id=?", approval))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> approval(factory, "Authority", "System", "TN", "APP-004"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void projectsKeepClientAndConsigneeSeparateAndProductSelectionIsShipmentSpecific() {
        UUID project = project(), client = company(), consignee = company(), product = product();
        jdbc.update("INSERT INTO organization_role(organization_id,role) VALUES (?,'CLIENT'),(?,'CONSIGNEE')", client, consignee);
        jdbc.update("INSERT INTO project_party(project_id,organization_id,role) VALUES (?,?,'CLIENT'),(?,?,'CONSIGNEE')", project, client, project, consignee);
        UUID category = jdbc.queryForObject("INSERT INTO product_category(product_id,code,name) VALUES (?,'CUSTOM','Custom grade') RETURNING id", UUID.class, product);
        jdbc.update("INSERT INTO project_product(project_id,product_id,product_category_id,carton_count,net_weight_kg,gross_weight_kg,unit_price,currency) VALUES (?,?,?,10,120,132,3.2500,'USD')", project, product, category);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT organization_id) FROM project_party WHERE project_id=? AND role IN ('CLIENT','CONSIGNEE')", Long.class, project)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT carton_count FROM project_product WHERE project_id=?", Integer.class, project)).isEqualTo(10);
    }

    private UUID company() { return jdbc.queryForObject("INSERT INTO organization(legal_name) VALUES (?) RETURNING id", UUID.class, "Fictional " + UUID.randomUUID()); }
    private UUID project() { return jdbc.queryForObject("INSERT INTO project(reference) VALUES (?) RETURNING id", UUID.class, "MD-" + UUID.randomUUID()); }
    private UUID product() { return jdbc.queryForObject("INSERT INTO product(code,name) VALUES (?,?) RETURNING id", UUID.class, "P-" + UUID.randomUUID(), "Fictional product"); }
    private UUID factory(UUID company) { return jdbc.queryForObject("INSERT INTO factory(company_id,name) VALUES (?,?) RETURNING id", UUID.class, company, "Factory " + UUID.randomUUID()); }
    private UUID approval(UUID factory, String authority, String system, String country, String reference) {
        return jdbc.queryForObject("INSERT INTO factory_approval(factory_id,issuing_authority,numbering_system,issuing_country_code,approval_reference) VALUES (?,?,?,?,?) RETURNING id", UUID.class, factory, authority, system, country, reference);
    }
}

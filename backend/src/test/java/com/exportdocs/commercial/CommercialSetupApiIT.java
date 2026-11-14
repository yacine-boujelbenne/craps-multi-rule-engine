package com.exportdocs.commercial;

import com.exportdocs.account.application.AccountPrincipal;
import com.exportdocs.party.infrastructure.OrganizationSelectionStore;
import com.exportdocs.party.infrastructure.OrganizationStore;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.anyMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CommercialSetupApiIT {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean OrganizationSelectionStore selections;
    @MockitoSpyBean OrganizationStore organizationStore;
    @MockitoSpyBean ProjectDetailsStore projectDetails;
    UUID admin, operator;
    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE project, organization, product, product_taxonomy_category, app_user, city, audit_log CASCADE");
        admin = user("admin", "ADMIN");
        operator = user("operator", "OPERATOR");
        user("viewer", "VIEWER");
        user("reviewer", "REVIEWER");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void geographyReadsFilterCountryAndActiveRowsWithStableLiteralPrefixPagesWithoutWrites() throws Exception {
        UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID second = UUID.fromString("22222222-2222-2222-2222-222222222222");
        jdbc.update("INSERT INTO city(id,country_code,name) VALUES (?,'TN','Alpha'),(?,'TN','alpha'),"
            + "(?,'TN','Beta'),(?,'TN','A_!%'),(?,'FR','Alpha')", first, second,
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("INSERT INTO city(country_code,name,active) VALUES ('TN','A inactive',false)");
        var project = create("/api/projects", "{\"reference\":\"geography-read\"}");
        UUID projectId = UUID.fromString(project.path("id").asText());
        long version = project.path("version").asLong();
        long auditCount = audits();
        var countries = read("/api/countries", "viewer");
        assertThat(countries).hasSize(249);
        List<String> expectedCodes = jdbc.queryForList(
            "SELECT code FROM country WHERE active ORDER BY lower(name),code", String.class);
        assertThat(countries.findValuesAsText("code")).containsExactlyElementsOf(expectedCodes);
        for (String role : List.of("admin", "operator", "viewer", "reviewer")) {
            var page = read("/api/countries/TN/cities?q=AL&page=0&size=1", role);
            assertThat(page.path("items").get(0).path("id").asText()).isEqualTo(first.toString());
            assertThat(page.path("items").get(0).path("countryCode").asText()).isEqualTo("TN");
            assertThat(page.path("totalElements").asLong()).isEqualTo(2);
            assertThat(page.path("totalPages").asInt()).isEqualTo(2);
            assertThat(page.path("page").asInt()).isZero();
            assertThat(page.path("size").asInt()).isEqualTo(1);
        }
        var next = read("/api/countries/TN/cities?q=al&page=1&size=1", "operator");
        assertThat(next.path("items").get(0).path("id").asText()).isEqualTo(second.toString());
        var literalResult = mvc.perform(get("/api/countries/TN/cities").with(account("operator"))
            .queryParam("q", " A_!% ")).andReturn();
        status(literalResult, 200);
        assertThat(body(literalResult).path("items")).hasSize(1);
        assertThat(body(literalResult).path("items").get(0).path("name").asText()).isEqualTo("A_!%");
        var all = read("/api/countries/TN/cities", "viewer");
        assertThat(all.path("items")).hasSize(4);
        assertThat(all.path("items").findValuesAsText("name")).containsExactly("A_!%", "Alpha", "alpha", "Beta");
        for (String suffix : List.of("?q=no-match", "?q=Alpha&page=2&size=1")) {
            var empty = read("/api/countries/TN/cities" + suffix, "viewer");
            assertThat(empty.path("items")).isEmpty();
        }
        var noMatches = read("/api/countries/DE/cities", "viewer");
        assertThat(noMatches.path("totalElements").asLong()).isZero();
        assertThat(noMatches.path("totalPages").asInt()).isZero();
        problem(mvc.perform(get("/api/countries/ZZ/cities").with(account("viewer"))).andReturn(),
            404, "countryCode");
        jdbc.update("UPDATE country SET active=false WHERE code='TN'");
        assertThat(read("/api/countries", "viewer").findValuesAsText("code")).doesNotContain("TN");
        problem(mvc.perform(get("/api/countries/TN/cities").with(account("viewer"))).andReturn(),
            404, "countryCode");
        assertThat(audits()).isEqualTo(auditCount);
        assertThat(jdbc.queryForObject("SELECT version FROM project WHERE id=?", Long.class, projectId)).isEqualTo(version);
    }

    @Test
    void minimalOrganizationAndProductExposeNullsDefaultsAndLocation() throws Exception {
        var result = write(post("/api/organizations"), "{\"legalName\":\"  Fictional Co  \",\"roles\":[\"CLIENT\",\"IMPORTER\"]}", "admin");
        status(result, 201);
        var org = body(result);
        assertThat(org.path("legalName").asText()).isEqualTo("Fictional Co");
        assertThat(org.path("version").asLong()).isZero();
        assertThat(org.path("active").asBoolean()).isTrue();
        for (String field : List.of("countryCode", "addressText",
            "registrationNumber")) assertThat(org.get(field).isNull()).isTrue();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/organizations/" + org.path("id").asText());
        var product = product("MiXeD");
        assertThat(product.path("code").asText()).isEqualTo("MiXeD");
        assertThat(product.get("scientificName").isNull()).isTrue();
        assertThat(product.get("hsCode").isNull()).isTrue();
        var grade = create("/api/products/" + product.path("id").asText() + "/grades",
            "{\"code\":\"50-80\",\"name\":\"Grade\"}");
        assertThat(grade.path("displayOrder").asInt()).isZero();
        assertThat(grade.get("defaultCartonNetKg").isNull()).isTrue();
        assertThat(grade.get("defaultPricePerKg").isNull()).isTrue();
        assertThat(grade.get("defaultCurrency").isNull()).isTrue();
    }

    @Test
    void exactDefaultsPatchMergeNoopAndParentVersion() throws Exception {
        var product = product("P");
        String grades = "/api/products/" + product.path("id").asText() + "/grades";
        var grade = create(grades, "{\"code\":\"G\",\"name\":\"Grade\",\"defaultCartonNetKg\":\"999999999999999.123\",\"defaultPricePerKg\":\"99999999999999.1234\",\"defaultCurrency\":\"USD\"}");
        assertThat(grade.path("defaultCartonNetKg").asText()).isEqualTo("999999999999999.123");
        assertThat(grade.path("defaultPricePerKg").asText()).isEqualTo("99999999999999.1234");
        String route = grades + "/" + grade.path("id").asText();
        long audits = audits();
        var same = write(patch(route), "{\"expectedVersion\":0,\"defaultCartonNetKg\":\"999999999999999.123\"}", "admin");
        status(same, 200);
        assertThat(body(same).path("version").asLong()).isZero();
        assertThat(audits()).isEqualTo(audits);
        problem(write(patch(route), "{\"expectedVersion\":0,\"defaultPricePerKg\":null}", "admin"), 400, "defaultCurrency");
        assertThat(audits()).isEqualTo(audits);
        var updated = write(patch(route), "{\"expectedVersion\":0,\"defaultPricePerKg\":null,\"defaultCurrency\":null,\"defaultCartonNetKg\":\"12\"}", "admin");
        status(updated, 200);
        assertThat(body(updated).path("defaultCartonNetKg").asText()).isEqualTo("12.000");
        assertThat(body(updated).path("version").asLong()).isEqualTo(1);
        assertThat(read("/api/products/" + product.path("id").asText(),
            "admin").path("version").asLong()).isZero();
        problem(write(patch(route), "{\"expectedVersion\":0}", "admin"), 409, "expectedVersion");
    }

    @ParameterizedTest @ValueSource(strings = {
        "0", "\"0\"", "\"0.000\"", "\"1.0000\"", "\"1e2\"",
            "\"-1\"", "\"1000000000000000\"", "true"
    }
    )
    void invalidBoxDefaultsNeverWrite(String value) throws Exception {
        var p = product("P");
        long before = audits();
        problem(write(post("/api/products/" + p.path("id").asText() + "/grades"),
            "{\"code\":\"G\",\"name\":\"Grade\",\"defaultCartonNetKg\":" + value + "}", "admin"), 400, "defaultCartonNetKg");
        assertThat(audits()).isEqualTo(before);
        assertThat(count("product_category")).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {
        "1.25", "\"1.00000\"", "\"1e2\"", "\"-1\"", "\"100000000000000\""
    }
    )
    void invalidPricesNeverWrite(String value) throws Exception {
        var p = product("P");
        long before = audits();
        problem(write(post("/api/products/" + p.path("id").asText() + "/grades"),
            "{\"code\":\"G\",\"name\":\"Grade\",\"defaultPricePerKg\":" + value + ",\"defaultCurrency\":\"USD\"}", "admin"), 400, "defaultPricePerKg");
        assertThat(audits()).isEqualTo(before);
    }

    @Test
    void validatesCurrencyCountryHsRoleStringsAndUnknownFields() throws Exception {
        for (String input : List.of("{\"legalName\":\"X\",\"roles\":[1]}", "{\"legalName\":\"X\",\"roles\":[\"CLIENT\",\"CLIENT\"]}", "{\"legalName\":\"X\",\"roles\":[]}", "{\"legalName\":\"X\",\"roles\":[\"CLIENT\"],\"countryCode\":\"XX\"}",
            "{\"legalName\":\"X\",\"roles\":[\"CLIENT\"],\"version\":0}")) {
            status(write(post("/api/organizations"), input, "admin"), 400);
        }
        problem(write(post("/api/products"), "{\"code\":\"P\",\"name\":\"P\",\"hsCode\":\"12\"}", "admin"), 400,
            "hsCode");
        var p = product("P");
        String route = "/api/products/" + p.path("id").asText() + "/grades";
        for (String input : List.of("{\"defaultPricePerKg\":\"0\"}", "{\"defaultPricePerKg\":\"0\",\"defaultCurrency\":\"ZZZ\"}", "{\"defaultCurrency\":\"USD\"}")) {
            status(write(post(route), "{\"code\":\"G\",\"name\":\"G\"," + input.substring(1), "admin"), 400);
        }
        assertThat(count("organization")).isZero();
    }

    @Test
    void readOnlyRolesCannotWriteAndOperatorMetadataAccessDoesNotGrantGradeWrites() throws Exception {
        var org = organization();
        var p = product("P");
        String grades = "/api/products/" + p.path("id").asText() + "/grades";
        var g = create(grades, "{\"code\":\"G\",\"name\":\"G\"}");
        var project = create("/api/projects", "{\"reference\":\"EXP-1\"}");
        grant(project, "INVOICE");
        grantForm(project, "PACKING_LIST");
        long before = audits();
        for (String role : List.of("operator", "viewer", "reviewer")) {
            for (String route : List.of("/api/organizations", "/api/products", grades,
                "/api/organizations/" + org.path("id").asText(),
                "/api/products/" + p.path("id").asText(),
                grades + "/" + g.path("id").asText())) status(mvc.perform(get(route).with(account(role))).andReturn(),
                200);
            if (!role.equals("operator")) {
                problem(write(post("/api/organizations"), "{\"legalName\":\"New\",\"roles\":[\"CLIENT\"]}", role), 403, null);
                problem(write(patch("/api/organizations/" + org.path("id").asText()),
                    "{\"expectedVersion\":0,\"active\":false}", role), 403, null);
                problem(write(post("/api/products"), "{\"code\":\"NEW\",\"name\":\"New\"}", role), 403, null);
                problem(write(patch("/api/products/" + p.path("id").asText()),
                    "{\"expectedVersion\":0,\"active\":false}", role), 403, null);
            }
            problem(write(post(grades), "{\"code\":\"NEW\",\"name\":\"New\"}", role), 403, null);
            problem(write(patch(grades + "/" + g.path("id").asText()),
                "{\"expectedVersion\":0,\"active\":false}", role), 403, null);
        }
        assertThat(audits()).isEqualTo(before);
    }

    @Test
    void operatorCompanyMetadataPersistsListsAndClearsWithoutAffectingProjectVersions() throws Exception {
        String input = """
            {"legalName":"Carrier","countryCode":"TN","addressText":"Legacy","registrationNumber":"REG",
             "roles":["CARRIER","SHIPPING_LINE"],"tradeName":" Trade ","taxIdentifier":" TAX ",
             "email":" Sales@example.test ","phone":" +216 00123456 "}
            """;
        var created = write(post("/api/organizations"), input, "operator");
        status(created, 201);
        var company = body(created);
        String route = "/api/organizations/" + company.path("id").asText();
        var project = create("/api/projects", "{\"reference\":\"company-metadata\"}");
        long before = audits();
        var stored = read(route, "viewer");
        assertThat(stored.path("tradeName").asText()).isEqualTo("Trade");
        assertThat(stored.path("taxIdentifier").asText()).isEqualTo("TAX");
        assertThat(stored.path("email").asText()).isEqualTo("Sales@example.test");
        assertThat(stored.path("phone").asText()).isEqualTo("+216 00123456");
        var page = read("/api/organizations?role=CARRIER", "reviewer");
        assertThat(page.path("totalElements").asInt()).isEqualTo(1);
        assertThat(page.path("items").get(0)).isEqualTo(stored);
        var noop = write(patch(route), "{\"expectedVersion\":0,\"tradeName\":\" Trade \"}", "operator");
        status(noop, 200);
        assertThat(body(noop).path("version").asLong()).isZero();
        assertThat(audits()).isEqualTo(before);
        var changed = write(patch(route), "{\"expectedVersion\":0,\"email\":null,\"phone\":\" \"}", "operator");
        status(changed, 200);
        assertThat(body(changed).path("version").asLong()).isEqualTo(1);
        assertThat(body(changed).path("email").isNull()).isTrue();
        assertThat(body(changed).path("phone").isNull()).isTrue();
        for (String field : List.of("tradeName", "taxIdentifier", "legalName", "countryCode", "addressText", "registrationNumber", "roles")) {
            assertThat(body(changed).get(field)).isEqualTo(stored.get(field));
        }
        assertThat(audits()).isEqualTo(before + 1);
        assertThat(jdbc.queryForObject("SELECT actor_id FROM audit_log WHERE entity_table='organization' AND entity_id=? ORDER BY occurred_at DESC LIMIT 1",
            UUID.class, company.path("id").asText())).isEqualTo(operator);
        problem(write(patch(route), "{\"expectedVersion\":0,\"tradeName\":\"Stale\"}", "operator"), 409, "expectedVersion");
        assertThat(read("/api/projects/" + project.path("id").asText(), "admin").path("version")).isEqualTo(project.path("version"));
        assertThat(audits()).isEqualTo(before + 1);
        String projectRoute = "/api/projects/" + project.path("id").asText();
        var carrierOnly = create("/api/organizations", "{\"legalName\":\"Carrier only\",\"roles\":[\"CARRIER\"]}");
        problem(write(patch(projectRoute), "{\"expectedVersion\":0,\"shippingLineId\":\""
            + carrierOnly.path("id").asText() + "\"}", "admin"), 422, "shippingLineId");
        status(write(patch(projectRoute), "{\"expectedVersion\":0,\"shippingLineId\":\""
            + company.path("id").asText() + "\"}", "admin"), 200);
        long beforeRejectedRemoval = audits();
        problem(write(patch(route), "{\"expectedVersion\":1,\"roles\":[\"CARRIER\"]}", "operator"), 409, "roles");
        assertThat(read(route, "viewer").path("roles")).isEqualTo(stored.path("roles"));
        assertThat(read(route, "viewer").path("version").asLong()).isEqualTo(1);
        assertThat(audits()).isEqualTo(beforeRejectedRemoval);
    }

    @Test
    void structuredAddressesPersistEmbedAndVersionIndependentlyOfCompanyAndProject() throws Exception {
        var company = create("/api/organizations", """
            {"legalName":"Address company","addressText":"Legacy address","roles":["CLIENT"]}
            """);
        String companyRoute = "/api/organizations/" + company.path("id").asText();
        String addresses = companyRoute + "/addresses";
        var project = create("/api/projects", "{\"reference\":\"ADDRESS-TEST\"}");
        UUID city = UUID.randomUUID();
        jdbc.update("INSERT INTO city(id,country_code,name) VALUES (?,'TN',?)", city, "City " + city);
        long before = audits();
        var created = write(post(addresses), """
            {"kind":"BUSINESS","countryCode":"TN","manualCity":" Tunis ","addressLine1":" Road 1 ",
             "postalCode":"00123","expectedOrganizationVersion":0}
            """, "operator");
        status(created, 201);
        var address = body(created);
        String route = addresses + "/" + address.path("id").asText();
        assertThat(created.getResponse().getHeader("Location")).isEqualTo(route);
        assertThat(address.path("version").asLong()).isZero();
        assertThat(address.path("postalCode").asText()).isEqualTo("00123");
        assertThat(address.path("manualCity").asText()).isEqualTo("Tunis");
        for (String role : List.of("admin", "operator", "viewer", "reviewer")) {
            assertThat(read(addresses, role).get(0)).isEqualTo(address);
        }
        assertThat(read(companyRoute, "viewer").path("addresses").get(0)).isEqualTo(address);
        assertThat(read("/api/organizations?role=CLIENT", "reviewer").path("items").get(0).path("addresses").get(0))
            .isEqualTo(address);
        assertThat(audits()).isEqualTo(before + 1);
        var changed = write(patch(route), "{\"expectedVersion\":0,\"addressLine2\":\" Suite 2 \",\"postalCode\":null}", "operator");
        status(changed, 200);
        assertThat(body(changed).path("version").asLong()).isEqualTo(1);
        assertThat(body(changed).path("postalCode").isNull()).isTrue();
        assertThat(body(changed).path("addressLine2").asText()).isEqualTo("Suite 2");
        status(write(patch(route), "{\"expectedVersion\":1,\"addressLine1\":\" Road 1 \"}", "operator"), 200);
        assertThat(audits()).isEqualTo(before + 2);
        problem(write(patch(route), "{\"expectedVersion\":0,\"addressLine1\":\"Stale\"}", "admin"), 409, "expectedVersion");
        problem(write(patch(route), "{\"expectedVersion\":1,\"cityId\":\"" + city + "\"}", "operator"), 400, "manualCity");
        var switched = write(patch(route), "{\"expectedVersion\":1,\"manualCity\":null,\"cityId\":\"" + city + "\"}", "operator");
        status(switched, 200);
        assertThat(body(switched).path("version").asLong()).isEqualTo(2);
        assertThat(body(switched).path("manualCity").isNull()).isTrue();
        status(write(patch(route), "{\"expectedVersion\":2,\"active\":false}", "admin"), 200);
        var billing = write(post(addresses), """
            {"kind":"BILLING","countryCode":"TN","manualCity":"Tunis","addressLine1":"Billing road"}
            """, "admin");
        status(billing, 201);
        var ordered = read(addresses, "viewer");
        assertThat(ordered.findValuesAsText("kind")).containsExactly("BILLING", "BUSINESS");
        assertThat(ordered.get(1).path("active").asBoolean()).isFalse();
        var other = organization();
        problem(write(patch("/api/organizations/" + other.path("id").asText() + "/addresses/" + address.path("id").asText()),
            "{\"expectedVersion\":3}", "admin"), 404, "addressId");
        for (String role : List.of("viewer", "reviewer")) {
            problem(write(post(addresses), "{\"kind\":\"OTHER\",\"countryCode\":\"TN\",\"manualCity\":\"Tunis\",\"addressLine1\":\"Road\"}", role), 403, null);
            problem(write(patch(route), "{\"expectedVersion\":3,\"active\":true}", role), 403, null);
        }
        var parent = read(companyRoute, "admin");
        assertThat(parent.path("version")).isEqualTo(company.path("version"));
        assertThat(parent.path("addressText").asText()).isEqualTo("Legacy address");
        assertThat(read("/api/projects/" + project.path("id").asText(), "admin").path("version")).isEqualTo(project.path("version"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entity_table='organization_address' AND actor_id=? AND actor_kind='USER' AND request_id IS NOT NULL",
            Long.class, operator)).isEqualTo(3);
        problem(mvc.perform(get("/api/organizations/" + UUID.randomUUID() + "/addresses").with(account("viewer"))).andReturn(), 404, null);
    }

    @Test
    void addressLocationChangesRejectInvalidReferencesButPreserveHistoricalInactiveLocations() throws Exception {
        var company = organization();
        String addresses = "/api/organizations/" + company.path("id").asText() + "/addresses";
        UUID city = UUID.randomUUID(), otherCountryCity = UUID.randomUUID(), inactiveCity = UUID.randomUUID();
        jdbc.update("INSERT INTO city(id,country_code,name) VALUES (?,'TN',?),(?,'FR',?)", city, "City " + city,
            otherCountryCity, "City " + otherCountryCity);
        jdbc.update("INSERT INTO city(id,country_code,name,active) VALUES (?,'TN',?,false)", inactiveCity, "City " + inactiveCity);
        long before = audits();
        String input = "{\"kind\":\"BUSINESS\",\"countryCode\":\"TN\",\"cityId\":\"" + city + "\",\"addressLine1\":\"Road\"}";
        for (UUID invalidCity : List.of(otherCountryCity, inactiveCity, UUID.randomUUID())) {
            var invalid = json.readTree(input);
            ((com.fasterxml.jackson.databind.node.ObjectNode) invalid).put("cityId", invalidCity.toString());
            problem(write(post(addresses), invalid.toString(), "admin"), 400, "cityId");
        }
        problem(write(post(addresses), input.replace("TN", "ZZ"), "operator"), 400, "countryCode");
        problem(write(post(addresses), input.substring(0, input.length() - 1) + ",\"manualCity\":\"Both\"}", "admin"), 400, "manualCity");
        problem(write(post(addresses), "{\"kind\":\"OTHER\",\"countryCode\":\"TN\",\"addressLine1\":\"Road\"}", "admin"), 400, "manualCity");
        problem(write(post(addresses), input.substring(0, input.length() - 1) + ",\"expectedOrganizationVersion\":1}", "admin"), 409, "expectedOrganizationVersion");
        assertThat(audits()).isEqualTo(before);
        var result = write(post(addresses), input, "operator");
        status(result, 201);
        String route = addresses + "/" + body(result).path("id").asText();
        jdbc.update("UPDATE city SET active=false WHERE id=?", city);
        jdbc.update("UPDATE country SET active=false WHERE code='TN'");
        try {
            long beforeHistoricalEdit = audits();
            var historical = write(patch(route), "{\"expectedVersion\":0,\"addressLine1\":\"Updated\"}", "operator");
            status(historical, 200);
            assertThat(body(historical).path("cityId").asText()).isEqualTo(city.toString());
            assertThat(body(historical).path("version").asLong()).isEqualTo(1);
            status(write(patch(route), "{\"expectedVersion\":1,\"countryCode\":\"TN\",\"cityId\":\"" + city + "\"}", "operator"), 200);
            assertThat(audits()).isEqualTo(beforeHistoricalEdit + 1);
            problem(write(post(addresses), input, "admin"), 400, "countryCode");
            problem(write(patch(route), "{\"expectedVersion\":1,\"cityId\":null,\"manualCity\":\"New\"}", "operator"), 400, "countryCode");
            jdbc.update("UPDATE country SET active=true WHERE code='TN'");
            problem(write(post(addresses), input, "admin"), 400, "cityId");
            problem(write(patch(route), "{\"expectedVersion\":1,\"countryCode\":\"FR\"}", "operator"), 400, "cityId");
            status(write(patch("/api/organizations/" + company.path("id").asText()), "{\"expectedVersion\":0,\"active\":false}", "admin"), 200);
            status(write(post(addresses), "{\"kind\":\"OTHER\",\"countryCode\":\"TN\",\"manualCity\":\"Manual\",\"addressLine1\":\"Road\"}", "operator"), 201);
        } finally {
            jdbc.update("UPDATE country SET active=true WHERE code='TN'");
        }
    }







    @Test
    void organizationRolesUpdateOnceAndCannotRemoveUsedRole() throws Exception {
        var org = organization();
        String route = "/api/organizations/" + org.path("id").asText();
        long before = audits();
        status(write(patch(route), "{\"expectedVersion\":0,\"roles\":[\"CONSIGNEE\",\"IMPORTER\",\"CLIENT\",\"SUPPLIER\"]}", "admin"), 200);
        assertThat(audits()).isEqualTo(before);
        var changed = write(patch(route), "{\"expectedVersion\":0,\"roles\":[\"SUPPLIER\",\"CLIENT\",\"IMPORTER\",\"CONSIGNEE\",\"EXPORTER\"]}", "admin");
        status(changed, 200);
        assertThat(body(changed).path("version").asLong()).isEqualTo(1);
        assertThat(body(changed).path("updatedAt")).isNotEqualTo(org.path("updatedAt"));
        var project = create("/api/projects",
            "{\"reference\":\"EXP-1\",\"consigneeId\":\"" + org.path("id").asText() + "\"}");
        before = audits();
        var removed = write(patch(route), "{\"expectedVersion\":1,\"roles\":[\"CLIENT\"]}", "admin");
        problem(removed, 409, "roles");
        assertThat(body(removed).path("errors").findValuesAsText("code")).contains("role_in_use");
        assertThat(audits()).isEqualTo(before);
        assertThat(read("/api/projects/" + project.path("id").asText(),
            "admin").path("consigneeId")).isEqualTo(org.path("id"));
    }

    @Test
    void listsUseActiveLiteralPrefixRoleAndStablePages() throws Exception {
        var org = organization();
        var other = create("/api/organizations", "{\"legalName\":\"Fake_One\",\"roles\":[\"CLIENT\",\"IMPORTER\"]}");
        create("/api/organizations", "{\"legalName\":\"FakeXOne\",\"roles\":[\"CLIENT\"]}");
        var page = read("/api/organizations?q=fake_&role=CLIENT&size=1", "operator");
        assertThat(page.path("totalElements").asLong()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("id")).isEqualTo(other.path("id"));
        status(write(patch("/api/organizations/" + org.path("id").asText()), "{\"expectedVersion\":0,\"active\":false}",
            "admin"), 200);
        assertThat(read("/api/organizations", "operator").path("totalElements").asLong()).isEqualTo(2);
        assertThat(read("/api/organizations?includeInactive=true", "admin").path("totalElements").asLong()).isEqualTo(3);
        status(mvc.perform(get("/api/organizations?includeInactive=true").with(account("operator"))).andReturn(), 403);
        assertThat(read("/api/organizations/" + org.path("id").asText(),
            "operator").path("active").asBoolean()).isFalse();
        assertThat(read("/api/organizations?q=missing", "operator").path("totalPages").asInt()).isZero();
        for (String query : List.of("page=0&page=1", "q=", "size=101", "page=2147483647&size=2",
            "includeInactive=1", "role=1",
            "sort=id")) status(mvc.perform(get("/api/organizations?" + query).with(account("admin"))).andReturn(),
            400);
    }

    @Test
    void duplicateCodesAndGradeParentMismatchAreHandled() throws Exception {
        var p = product("P");
        var other = product("p");
        problem(write(post("/api/products"), "{\"code\":\" P \",\"name\":\"Other\"}", "admin"), 409,
            "code");
        String route = "/api/products/" + p.path("id").asText() + "/grades";
        var g = create(route, "{\"code\":\"G\",\"name\":\"G\"}");
        problem(write(post(route), "{\"code\":\"G\",\"name\":\"Another\"}", "admin"), 409, "code");
        String wrong = "/api/products/" + other.path("id").asText() + "/grades/" + g.path("id").asText();
        status(mvc.perform(get(wrong).with(account("admin"))).andReturn(), 404);
        status(write(patch(wrong), "{\"expectedVersion\":0}", "admin"), 404);
    }

    @Test
    void commercialPartiesShareCanonicalConsigneeAndProjectVersion() throws Exception {
        var org = organization();
        var project = create("/api/projects", "{\"reference\":\"EXP-1\"}");
        String route = "/api/projects/" + project.path("id").asText();
        var empty = read(route + "/commercial-parties", "admin");
        for (String field : List.of("supplierId", "clientId", "importerId",
            "consigneeId")) assertThat(empty.get(field).isNull()).isTrue();
        String id = org.path("id").asText();
        var result = write(patch(route + "/commercial-parties"),
            "{\"expectedVersion\":0,\"supplierId\":\"" + id + "\",\"clientId\":\"" + id + "\",\"importerId\":\"" + id + "\",\"consigneeId\":\"" + id + "\"}",
            "admin");
        status(result, 200);
        assertThat(body(result).path("version").asLong()).isEqualTo(1);
        assertThat(count("project_party")).isEqualTo(4);
        assertThat(read(route, "admin").path("consigneeId").asText()).isEqualTo(id);
        assertThat(read(route, "admin").path("version").asLong()).isEqualTo(1);
        long before = audits();
        status(write(patch(route + "/commercial-parties"), "{\"expectedVersion\":1,\"supplierId\":\"" + id + "\"}",
            "admin"), 200);
        assertThat(audits()).isEqualTo(before);
        problem(write(patch(route + "/commercial-parties"), "{\"expectedVersion\":0}", "admin"), 409,
            "expectedVersion");
        status(write(patch(route), "{\"expectedVersion\":1,\"consigneeId\":null}", "admin"), 200);
        var latest = read(route + "/commercial-parties", "admin");
        assertThat(latest.get("consigneeId").isNull()).isTrue();
        assertThat(latest.path("version").asLong()).isEqualTo(2);
    }

    @Test
    void commercialPermissionAndInvalidReferenceFailuresAreAtomic() throws Exception {
        var p = create("/api/projects", "{\"reference\":\"EXP-1\"}");
        String route = "/api/projects/" + p.path("id").asText() + "/commercial-parties";
        status(mvc.perform(get(route).with(account("operator"))).andReturn(), 404);
        grant(p, "INVOICE");
        problem(write(patch(route), "{\"expectedVersion\":0}", "operator"), 403, null);
        grantForm(p, "PROJECT_SETUP");
        var org = create("/api/organizations", "{\"legalName\":\"Client\",\"roles\":[\"CLIENT\"]}");
        long before = audits();
        for (String field : List.of("supplierId", "importerId",
            "consigneeId")) problem(write(patch(route),
            "{\"expectedVersion\":0,\"clientId\":\"" + org.path("id").asText() + "\",\"" + field + "\":\"" + org.path("id").asText() + "\"}",
            "operator"), 422, field);
        assertThat(audits()).isEqualTo(before);
        assertThat(count("project_party")).isZero();
        status(write(patch(route), "{\"expectedVersion\":0,\"clientId\":\"" + org.path("id").asText() + "\"}",
            "operator"), 200);
        status(write(patch("/api/organizations/" + org.path("id").asText()), "{\"expectedVersion\":0,\"active\":false}",
            "admin"), 200);
        status(write(patch(route), "{\"expectedVersion\":1,\"clientId\":null}", "operator"), 200);
        problem(write(patch(route), "{\"expectedVersion\":2,\"clientId\":\"" + org.path("id").asText() + "\"}",
            "operator"), 422, "clientId");
    }

    @Test
    void concurrentCommercialAndV2WritesHaveOneSharedWinner() throws Exception {
        var p = create("/api/projects", "{\"reference\":\"EXP-RACE\"}");
        var org = organization();
        String route = "/api/projects/" + p.path("id").asText();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await(); return write(patch(route + "/commercial-parties"),
                    "{\"expectedVersion\":0,\"supplierId\":\"" + org.path("id").asText() + "\"}",
                    "admin").getResponse().getStatus();
            });
            var second = executor.submit(() -> {
                start.await(); return write(patch(route),
                    "{\"expectedVersion\":0,\"consigneeId\":\"" + org.path("id").asText() + "\"}",
                    "admin").getResponse().getStatus();
            });
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,
                409);
        }
        assertThat(read(route, "admin").path("version").asLong()).isEqualTo(1);
    }

    @Test
    void scalarPatchesClearOptionalValuesAndEquivalentDefaultsDoNotAudit() throws Exception {
        var org = create("/api/organizations", "{\"legalName\":\"Co\",\"countryCode\":\"TN\",\"addressText\":\"Address\",\"registrationNumber\":\"R\",\"roles\":[\"BUYER\"]}");
        String orgRoute = "/api/organizations/" + org.path("id").asText();
        var changed = write(patch(orgRoute), "{\"expectedVersion\":0,\"addressText\":\" \",\"countryCode\":null,\"registrationNumber\":null}", "admin");
        status(changed, 200);
        assertThat(body(changed).path("version").asLong()).isEqualTo(1);
        for (String field : List.of("countryCode", "addressText",
            "registrationNumber"))assertThat(body(changed).get(field).isNull()).isTrue();
        long before = audits();
        status(write(patch(orgRoute), "{\"expectedVersion\":1,\"addressText\":null,\"legalName\":\" Co \"}", "admin"), 200);
        assertThat(audits()).isEqualTo(before);
        var product = create("/api/products", "{\"code\":\"P\",\"name\":\"Product\",\"scientificName\":\"Name\",\"hsCode\":\"030743\"}");
        String productRoute = "/api/products/" + product.path("id").asText();
        var updated = write(patch(productRoute), "{\"expectedVersion\":0,\"scientificName\":\" \",\"hsCode\":null,\"name\":\" Updated \"}", "admin");
        status(updated, 200);
        assertThat(body(updated).path("name").asText()).isEqualTo("Updated");
        assertThat(body(updated).get("scientificName").isNull()).isTrue();
        assertThat(body(updated).get("hsCode").isNull()).isTrue();
        before = audits();
        status(write(patch(productRoute), "{\"expectedVersion\":1,\"name\":\"Updated\"}", "admin"), 200);
        assertThat(audits()).isEqualTo(before);
    }

    @Test
    void catalogListsFilterPrefixAndActiveEntriesAndOrderGrades() throws Exception {
        var product = product("P_One");
        product("PXOne");
        assertThat(read("/api/products?q=p_", "viewer").path("totalElements").asLong()).isEqualTo(1);
        String grades = "/api/products/" + product.path("id").asText() + "/grades";
        var last = create(grades, "{\"code\":\"A\",\"name\":\"Last\",\"displayOrder\":2}");
        var second = create(grades, "{\"code\":\"B\",\"name\":\"Second\",\"displayOrder\":1}");
        var first = create(grades, "{\"code\":\"A_1\",\"name\":\"First\",\"displayOrder\":1}");
        var page = read(grades + "?size=2", "reviewer");
        assertThat(page.path("totalElements").asLong()).isEqualTo(3);
        assertThat(page.path("totalPages").asInt()).isEqualTo(2);
        assertThat(page.path("items").findValuesAsText("id")).containsExactly(first.path("id").asText(),
            second.path("id").asText());
        assertThat(read(grades + "?q=a_",
            "reviewer").path("totalElements").asLong()).isEqualTo(1);
        status(write(patch(grades + "/" + last.path("id").asText()), "{\"expectedVersion\":0,\"active\":false}",
            "admin"), 200);
        assertThat(read(grades, "viewer").path("totalElements").asLong()).isEqualTo(2);
        assertThat(read(grades + "?includeInactive=true",
            "admin").path("totalElements").asLong()).isEqualTo(3);
        status(mvc.perform(get(grades + "?includeInactive=true").with(account("viewer"))).andReturn(), 403);
        assertThat(read(grades + "/" + last.path("id").asText(),
            "viewer").path("active").asBoolean()).isFalse();
        status(write(patch("/api/products/" + product.path("id").asText()), "{\"expectedVersion\":0,\"active\":false}",
            "admin"), 200);
        assertThat(read("/api/products", "viewer").path("totalElements").asLong()).isEqualTo(1);
        status(mvc.perform(get("/api/products?includeInactive=true").with(account("operator"))).andReturn(), 403);
        status(mvc.perform(get("/api/products/" + UUID.randomUUID() + "/grades").with(account("admin"))).andReturn(),
            404);
    }

    @Test
    void referencesDoNotInferNewRolesAndHistoricalInactiveLinksStayReadable() throws Exception {
        UUID legacy = UUID.randomUUID();
        jdbc.update("INSERT INTO organization(id,legal_name) VALUES (?,?)", legacy, "Legacy metadata");
        assertThat(read("/api/organizations/" + legacy, "viewer").path("roles").isEmpty()).isTrue();
        var producer = create("/api/organizations", "{\"legalName\":\"Producer\",\"roles\":[\"PRODUCER\",\"BUYER\"]}");
        var p = create("/api/projects",
            "{\"reference\":\"EXP-LEGACY\",\"producerIds\":[\"" + producer.path("id").asText() + "\"]}");
        String route = "/api/projects/" + p.path("id").asText() + "/commercial-parties";
        var commercial = read(route, "admin");
        assertThat(commercial.get("supplierId").isNull()).isTrue();
        assertThat(commercial.get("clientId").isNull()).isTrue();
        problem(write(patch(route),
            "{\"expectedVersion\":0,\"clientId\":\"" + producer.path("id").asText() + "\"}", "admin"),
            422, "clientId");
        var org = organization();
        String id = org.path("id").asText();
        status(write(patch(route), "{\"expectedVersion\":0,\"supplierId\":\"" + id + "\"}", "admin"), 200);
        status(write(patch("/api/organizations/" + id), "{\"expectedVersion\":0,\"active\":false}", "admin"), 200);
        assertThat(read(route, "admin").path("supplierId").asText()).isEqualTo(id);
        long before = audits();
        status(write(patch(route), "{\"expectedVersion\":1,\"supplierId\":\"" + id + "\"}", "admin"), 200);
        assertThat(audits()).isEqualTo(before);
    }

    @Test
    void rejectsMalformedIdsQueriesNullsAndScalarRoleArraysWithoutWrites() throws Exception {
        var p = product("P");
        var org = organization();
        var project = create("/api/projects", "{\"reference\":\"EXP-STRICT\"}");
        String commercial = "/api/projects/" + project.path("id").asText() + "/commercial-parties";
        long before = audits();
        for (String input : List.of("{\"expectedVersion\":0,\"supplierId\":[\"" + org.path("id").asText() + "\"]}",
            "{\"expectedVersion\":0,\"clientId\":1}", "{\"expectedVersion\":0,\"consigneeId\":\"1-1-1-1-1\"}", "{\"expectedVersion\":0,\"version\":1}")) status(write(patch(commercial), input,
            "admin"), 400);
        for (String route : List.of("/api/organizations/bad-id", "/api/products/1-1-1-1-1",
            "/api/products/" + p.path("id").asText() + "/grades/bad-id", commercial + "?q=unknown",
            "/api/organizations/" + org.path("id").asText() + "?page=0")) status(mvc.perform(get(route).with(account("admin"))).andReturn(),
            400);
        status(write(patch("/api/organizations/" + org.path("id").asText()), "{\"expectedVersion\":0,\"roles\":null}",
            "admin"), 400);
        status(write(patch("/api/products/" + p.path("id").asText()), "{\"expectedVersion\":0,\"code\":null}",
            "admin"), 400);
        assertThat(audits()).isEqualTo(before);
    }

    @Test
    void newRoutesRequireSessionCsrfAndReadyAccount() throws Exception {
        var p = product("P");
        for (String route : List.of("/api/organizations", "/api/products",
            "/api/products/" + p.path("id").asText() + "/grades")) {
            status(mvc.perform(get(route)).andReturn(), 401);
            status(mvc.perform(post(route).with(account("admin")).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn(),
                403);
        }
        jdbc.update("UPDATE local_account SET must_change_password=true WHERE user_id=?", operator);
        status(mvc.perform(get("/api/organizations").with(account("operator"))).andReturn(), 403);
    }

    @Test
    void auditContextCoversMasterAndPartyWritesAndAuditFailureRollsBack() throws Exception {
        long before = audits();
        var org = organization();
        var p = product("P");
        create("/api/products/" + p.path("id").asText() + "/grades", "{\"code\":\"G\",\"name\":\"G\"}");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entity_table IN ('organization','organization_role','product','product_category') AND actor_id=? AND actor_kind='USER' AND request_id IS NOT NULL", Long.class, admin)).isEqualTo(audits()-before);
        jdbc.execute("CREATE FUNCTION test_reject_commercial_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private test detail'; END $$");
        jdbc.execute("CREATE TRIGGER test_reject_commercial_audit BEFORE INSERT ON audit_log FOR EACH ROW WHEN (NEW.entity_table='organization_role') EXECUTE FUNCTION test_reject_commercial_audit()");
        try {
            before = audits();
            problem(write(patch("/api/organizations/" + org.path("id").asText()), "{\"expectedVersion\":0,\"legalName\":\"Rollback\",\"roles\":[\"CLIENT\",\"EXPORTER\"]}",
                "admin"), 500, null);
            assertThat(audits()).isEqualTo(before);
            var unchanged = read("/api/organizations/" + org.path("id").asText(), "admin");
            assertThat(unchanged.path("version").asLong()).isZero();
            assertThat(unchanged.path("legalName")).isEqualTo(org.path("legalName"));
            assertThat(unchanged.path("roles")).isEqualTo(org.path("roles"));
        } finally {
            jdbc.execute("DROP TRIGGER test_reject_commercial_audit ON audit_log");
            jdbc.execute("DROP FUNCTION test_reject_commercial_audit()");
        }
    }

    @Test
    void v2CompanySelectionSerializesWithDirectoryDeactivation() throws Exception {
        var organization = organization();
        UUID id = UUID.fromString(organization.path("id").asText());
        var selected = new CountDownLatch(1);
        var releaseSelection = new CountDownLatch(1);
        var deactivationEntered = new CountDownLatch(1);
        var backendPid = new AtomicInteger();
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (Thread.currentThread().getName().equals("v2-selection")) {
                selected.countDown();
                if (!releaseSelection.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Selection not released");
            }
            return result;
        }
        ).when(selections).validate(anyMap(), anyMap());
        doAnswer(invocation -> {
            if (Thread.currentThread().getName().equals("directory-deactivation")) {
                backendPid.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));deactivationEntered.countDown();
            }
            return invocation.callRealMethod();
        }
        ).when(organizationStore).find(id, true);
        try (var selector = Executors.newSingleThreadExecutor(task -> new Thread(task, "v2-selection"));
        var deactivator = Executors.newSingleThreadExecutor(task -> new Thread(task, "directory-deactivation"))) {
            var selection = selector.submit(() -> write(post("/api/projects"),
                "{\"reference\":\"EXP-SELECT\",\"consigneeId\":\"" + id + "\"}", "admin"));
            try {
                assertThat(selected.await(15, TimeUnit.SECONDS)).isTrue();
                var deactivation = deactivator.submit(() -> write(patch("/api/organizations/" + id), "{\"expectedVersion\":0,\"active\":false}",
                    "admin"));
                assertThat(deactivationEntered.await(15, TimeUnit.SECONDS)).isTrue();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                boolean blocked = false;
                while (System.nanoTime()<deadline && !blocked) {
                    blocked = Boolean.TRUE.equals(jdbc.queryForObject("SELECT cardinality(pg_blocking_pids(?)) > 0", Boolean.class, backendPid.get()));
                    if (!blocked)Thread.sleep(10);
                }
                assertThat(blocked).as("Directory writer waits for v2's company share lock").isTrue();
                releaseSelection.countDown();
                status(selection.get(15, TimeUnit.SECONDS), 201);
                status(deactivation.get(15, TimeUnit.SECONDS), 200);
            } finally {
                releaseSelection.countDown();
            }
        }
        assertThat(count("project_party")).isEqualTo(1);
    }

    @ParameterizedTest @ValueSource(strings = {
        "", "/commercial-parties"
    }
    )
    void setupGrantIsRecheckedAfterWaitingForProjectLock(String suffix) throws Exception {
        var project = create("/api/projects", "{\"reference\":\"EXP-GRANT\"}");
        grant(project, "PROJECT_SETUP");
        UUID id = UUID.fromString(project.path("id").asText());
        var visible = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            if (Thread.currentThread().getName().equals("waiting-setup-writer"))visible.countDown();
            return result;
        }
        ).when(projectDetails).visible(id, new com.exportdocs.project.application.ProjectActor(operator,
            java.util.Set.of("OPERATOR")));
        try (var executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "waiting-setup-writer"))) {
            var waiting = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<MvcResult>>();
            new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
                jdbc.queryForObject("SELECT id FROM project WHERE id=? FOR UPDATE", UUID.class, id);
                waiting.set(executor.submit(() -> write(patch("/api/projects/" + id + suffix), "{\"expectedVersion\":0}",
                    "operator")));
                try {
                    assertThat(visible.await(15, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();throw new IllegalStateException(exception);
                }
                jdbc.update("DELETE FROM project_form_assignment WHERE project_id=? AND user_id=? AND form_key='PROJECT_SETUP'", id, operator);
            });
            status(waiting.get().get(15, TimeUnit.SECONDS), 403);
        }
        assertThat(read("/api/projects/" + id, "admin").path("version").asLong()).isZero();
    }

    private JsonNode organization() throws Exception {
        return create("/api/organizations", "{\"legalName\":\"Fictional Co\",\"roles\":[\"SUPPLIER\",\"CLIENT\",\"IMPORTER\",\"CONSIGNEE\"]}");
    }

    private JsonNode product(String code) throws Exception {
        return create("/api/products", "{\"code\":\"" + code + "\",\"name\":\"Fictional product\"}");
    }

    @Test
    void operatorProductMetadataKeepsNameAliasesAndGradesRemainAdminOnly() throws Exception {
        var result = write(post("/api/products"), """
            {"code":"META","commercialName":" Product ","description":" Description ",
             "processingMethod":" Frozen ","storageRequirements":" -18 C "}
            """, "operator");
        status(result, 201);
        var product = body(result);
        String route = "/api/products/" + product.path("id").asText();
        assertThat(product.path("name").asText()).isEqualTo("Product");
        assertThat(product.path("commercialName")).isEqualTo(product.path("name"));
        assertThat(product.path("defaultMeasurementUnit").asText()).isEqualTo("KG");
        assertThat(product.path("categories")).isEmpty();
        assertThat(product.path("grades")).isEmpty();
        var grade = create(route + "/grades", "{\"code\":\"G\",\"name\":\"Grade\",\"defaultPricePerKg\":\"2.5000\",\"defaultCurrency\":\"USD\"}");
        assertThat(read(route, "viewer").path("grades").get(0).path("id")).isEqualTo(grade.path("id"));
        long before = audits();
        status(write(patch(route), "{\"expectedVersion\":0,\"name\":\" Product \",\"commercialName\":\"Product\"}", "operator"), 200);
        assertThat(audits()).isEqualTo(before);
        problem(write(patch(route), "{\"expectedVersion\":1,\"description\":\"Stale\"}", "operator"), 409, "expectedVersion");
        var changed = write(patch(route), "{\"expectedVersion\":0,\"commercialName\":\"Updated\",\"description\":null,\"defaultMeasurementUnit\":\" BOX \"}", "operator");
        status(changed, 200);
        assertThat(body(changed).path("version").asLong()).isEqualTo(1);
        assertThat(body(changed).path("name").asText()).isEqualTo("Updated");
        assertThat(body(changed).path("commercialName")).isEqualTo(body(changed).path("name"));
        assertThat(body(changed).path("description").isNull()).isTrue();
        assertThat(body(changed).path("processingMethod").asText()).isEqualTo("Frozen");
        assertThat(body(changed).path("storageRequirements").asText()).isEqualTo("-18 C");
        assertThat(body(changed).path("defaultMeasurementUnit").asText()).isEqualTo("BOX");
        problem(write(patch(route), "{\"expectedVersion\":1,\"unitPrice\":\"2.0000\"}", "operator"), 400, "unitPrice");
        problem(write(post(route + "/grades"), "{\"code\":\"OP\",\"name\":\"Forbidden\"}", "operator"), 403, null);
        problem(write(patch(route + "/grades/" + grade.path("id").asText()), "{\"expectedVersion\":0,\"defaultPricePerKg\":\"9.0000\"}", "operator"), 403, null);
        status(write(patch(route + "/grades/" + grade.path("id").asText()), "{\"expectedVersion\":0,\"active\":false}", "admin"), 200);
        assertThat(read(route, "reviewer").path("grades")).isEmpty();
        assertThat(read(route, "reviewer").path("version").asLong()).isEqualTo(1);
        var legacy = create("/api/products", "{\"code\":\"LEGACY\",\"name\":\"Legacy\"}");
        assertThat(legacy.path("commercialName").asText()).isEqualTo("Legacy");
    }

    @Test
    void taxonomyAssignmentsVersionOnlyEffectiveSetChangesAndRetainInactiveHistory() throws Exception {
        String categories = "/api/product-taxonomy-categories";
        var first = write(post(categories), "{\"code\":\"F_1\",\"name\":\"Beta\",\"description\":\" \"}", "operator");
        status(first, 201);
        var beta = body(first);
        var alpha = create(categories, "{\"code\":\"FX1\",\"name\":\"alpha\"}");
        String betaRoute = categories + "/" + beta.path("id").asText();
        problem(write(post(categories), "{\"code\":\"F_1\",\"name\":\"Duplicate\"}", "operator"), 409, "code");
        assertThat(read(categories + "?q=f_", "viewer").path("totalElements").asLong()).isEqualTo(1);
        assertThat(read(categories, "reviewer").path("items").findValuesAsText("name")).containsExactly("alpha", "Beta");
        var product = product("TAX");
        var project = create("/api/projects", "{\"reference\":\"TAXONOMY-TEST\"}");
        String productRoute = "/api/products/" + product.path("id").asText();
        String assignmentRoute = productRoute + "/taxonomy-categories";
        String ids = "[\"" + beta.path("id").asText() + "\",\"" + alpha.path("id").asText() + "\"]";
        status(write(put(assignmentRoute), "{\"expectedProductVersion\":0,\"categoryIds\":" + ids + "}", "operator"), 200);
        assertThat(read(productRoute, "viewer").path("version").asLong()).isEqualTo(1);
        assertThat(read(productRoute, "viewer").path("categories").findValuesAsText("name")).containsExactly("alpha", "Beta");
        assertThat(read("/api/products?categoryId=" + beta.path("id").asText(), "viewer").path("items").get(0).path("id"))
            .isEqualTo(product.path("id"));
        assertThat(read("/api/products?categoryId=" + UUID.randomUUID(), "viewer").path("items")).isEmpty();
        long before = audits();
        String reversed = "[\"" + alpha.path("id").asText() + "\",\"" + beta.path("id").asText() + "\"]";
        status(write(put(assignmentRoute), "{\"expectedProductVersion\":1,\"categoryIds\":" + reversed + "}", "operator"), 200);
        assertThat(audits()).isEqualTo(before);
        problem(write(put(assignmentRoute), "{\"expectedProductVersion\":0,\"categoryIds\":[]}", "operator"), 409, "expectedProductVersion");
        status(write(patch(betaRoute), "{\"expectedVersion\":0,\"active\":false}", "operator"), 200);
        assertThat(read(productRoute, "reviewer").path("version").asLong()).isEqualTo(1);
        assertThat(read(categories, "viewer").path("totalElements").asLong()).isEqualTo(1);
        assertThat(read(categories + "?includeInactive=true", "admin").path("totalElements").asLong()).isEqualTo(2);
        problem(mvc.perform(get(categories).queryParam("includeInactive", "true").with(account("operator"))).andReturn(), 403, null);
        before = audits();
        status(write(put(assignmentRoute), "{\"expectedProductVersion\":1,\"categoryIds\":" + ids + "}", "operator"), 200);
        status(write(patch(betaRoute), "{\"expectedVersion\":1,\"description\":null}", "operator"), 200);
        assertThat(audits()).isEqualTo(before);
        problem(write(patch(betaRoute), "{\"expectedVersion\":0,\"name\":\"Stale\"}", "operator"), 409, "expectedVersion");
        status(write(put(assignmentRoute), "{\"expectedProductVersion\":1,\"categoryIds\":[]}", "operator"), 200);
        assertThat(read(productRoute, "reviewer").path("version").asLong()).isEqualTo(2);
        before = audits();
        problem(write(put(assignmentRoute), "{\"expectedProductVersion\":2,\"categoryIds\":[\"" + beta.path("id").asText() + "\"]}", "operator"), 400, "categoryIds");
        problem(write(put(assignmentRoute), "{\"expectedProductVersion\":2,\"categoryIds\":[\"" + UUID.randomUUID() + "\"]}", "operator"), 400, "categoryIds");
        for (String role : List.of("viewer", "reviewer")) {
            problem(write(post(categories), "{\"code\":\"DENIED\",\"name\":\"Denied\"}", role), 403, null);
            problem(write(patch(betaRoute), "{\"expectedVersion\":1,\"active\":true}", role), 403, null);
            problem(write(put(assignmentRoute), "{\"expectedProductVersion\":2,\"categoryIds\":[]}", role), 403, null);
        }
        assertThat(audits()).isEqualTo(before);
        assertThat(read(assignmentRoute, "viewer")).isEmpty();
        assertThat(read("/api/projects/" + project.path("id").asText(), "admin").path("version").asLong()).isZero();
        problem(write(put("/api/products/" + UUID.randomUUID() + "/taxonomy-categories"),
            "{\"expectedProductVersion\":0,\"categoryIds\":[]}", "operator"), 404, null);
    }

    @Test
    void shipmentProductsSnapshotGradeDefaultsAndVersionOnlyEffectiveChanges() throws Exception {
        var product = product("SHIPMENT-PRODUCT");
        var grade = create("/api/products/" + product.path("id").asText() + "/grades",
            "{\"code\":\"G\",\"name\":\"Grade\",\"defaultCartonNetKg\":\"10.25\",\"defaultPricePerKg\":\"2.5000\",\"defaultCurrency\":\"USD\"}");
        var project = create("/api/projects", "{\"reference\":\"SHIPMENT-PRODUCTS\"}");
        String route = "/api/projects/" + project.path("id").asText() + "/products";
        String productId = product.path("id").asText();
        String gradeId = grade.path("id").asText();
        grant(project, "INVOICE");
        String createBody = "{\"productId\":\"" + productId + "\",\"productCategoryId\":\"" + gradeId
            + "\",\"expectedProjectVersion\":0,\"cartonCount\":10,\"quantity\":\"25\",\"quantityUnit\":\"KG\","
            + "\"netWeightKg\":\"20.125\",\"grossWeightKg\":\"22.5\"}";
        problem(write(post(route), createBody, "operator"), 403, null);
        grantForm(project, "PACKING_LIST");
        problem(write(post(route), createBody.replace("}", ",\"unitPrice\":null,\"currency\":null}"), "operator"), 403, null);
        var created = write(post(route), createBody, "operator");
        status(created, 201);
        var line = body(created);
        assertThat(line.path("version").asLong()).isZero();
        assertThat(line.path("projectVersion").asLong()).isEqualTo(1);
        assertThat(line.path("quantity").asText()).isEqualTo("25.000");
        assertThat(line.path("netWeightKg").asText()).isEqualTo("20.125");
        assertThat(line.path("appliedGradeVersion").asLong()).isZero();
        assertThat(line.path("appliedGradeName").asText()).isEqualTo("Grade");
        assertThat(line.path("appliedCartonNetKg").asText()).isEqualTo("10.250");
        assertThat(line.path("unitPrice").asText()).isEqualTo("2.5000");
        assertThat(line.path("currency").asText()).isEqualTo("USD");
        status(write(patch("/api/products/" + productId + "/grades/" + gradeId),
            "{\"expectedVersion\":0,\"defaultCartonNetKg\":\"15\",\"defaultPricePerKg\":\"9\",\"defaultCurrency\":\"EUR\"}", "admin"), 200);
        assertThat(read(route, "operator")).hasSize(1);

        String id = line.path("id").asText();
        String lineRoute = route + "/" + id;
        long before = audits();
        var noop = write(patch(lineRoute), "{\"expectedVersion\":0,\"expectedProjectVersion\":1,\"quantity\":\"25.000\"}", "operator");
        status(noop, 200);
        assertThat(audits()).isEqualTo(before);
        assertThat(body(noop).path("version").asLong()).isZero();
        var changed = write(patch(lineRoute), "{\"expectedVersion\":0,\"expectedProjectVersion\":1,\"quantity\":\"26\"}", "operator");
        status(changed, 200);
        assertThat(body(changed).path("version").asLong()).isEqualTo(1);
        assertThat(body(changed).path("projectVersion").asLong()).isEqualTo(2);
        assertThat(body(changed).path("appliedCartonNetKg").asText()).isEqualTo("10.250");
        assertThat(body(changed).path("unitPrice").asText()).isEqualTo("2.5000");
        problem(write(patch(lineRoute), "{\"expectedVersion\":0,\"expectedProjectVersion\":2}", "operator"), 409, "expectedVersion");
        problem(write(patch(lineRoute), "{\"expectedVersion\":1,\"expectedProjectVersion\":1}", "operator"), 409, "expectedProjectVersion");
        problem(write(patch(lineRoute), "{\"expectedVersion\":1,\"expectedProjectVersion\":2,\"unitPrice\":null,\"currency\":null}", "operator"), 403, null);

        var cleared = write(patch(lineRoute), "{\"expectedVersion\":1,\"expectedProjectVersion\":2,\"productCategoryId\":null}", "admin");
        status(cleared, 200);
        assertThat(body(cleared).get("appliedGradeVersion").isNull()).isTrue();
        assertThat(body(cleared).get("unitPrice").isNull()).isTrue();
        assertThat(body(cleared).get("quantity").asText()).isEqualTo("26.000");
        assertThat(body(cleared).path("projectVersion").asLong()).isEqualTo(3);
        problem(write(patch(lineRoute), "{\"expectedVersion\":2,\"expectedProjectVersion\":3,\"unitPrice\":\"1.23456\",\"currency\":\"USD\"}", "admin"), 400, "unitPrice");

        var priced = write(post(route), "{\"productId\":\"" + productId + "\",\"productCategoryId\":\"" + gradeId
            + "\",\"expectedProjectVersion\":3,\"unitPrice\":\"3.2500\",\"currency\":\"EUR\"}", "admin");
        status(priced, 201);
        assertThat(body(priced).path("unitPrice").asText()).isEqualTo("3.2500");
        assertThat(body(priced).path("currency").asText()).isEqualTo("EUR");
        assertThat(body(priced).path("projectVersion").asLong()).isEqualTo(4);
        assertThat(read("/api/projects/" + project.path("id").asText(), "admin").path("version").asLong()).isEqualTo(4);
    }

    @Test
    void shipmentFormsPersistWithAssignmentsRevisionAndAudit() throws Exception {
        var project=create("/api/projects", "{\"reference\":\"FORMS-1\"}");
        String route="/api/projects/"+project.path("id").asText()+"/forms";
        String input="{\"expectedProjectVersion\":0,\"data\":[{\"number\":\"INV-1\",\"date\":\"2026-11-14\"}]}";
        status(write(put(route+"/INVOICE"),input,"operator"),404);
        grant(project,"PACKING_LIST");
        status(write(put(route+"/INVOICE"),input,"operator"),403);
        grantForm(project,"INVOICE");
        var saved=write(put(route+"/INVOICE"),input,"operator");
        status(saved,200);
        assertThat(body(saved).path("projectVersion").asLong()).isEqualTo(1);
        assertThat(read(route,"operator").path("data").path("INVOICE").get(0).path("number").asText()).isEqualTo("INV-1");
        long audits=audits();
        status(write(put(route+"/INVOICE"),input,"operator"),409);
        status(write(put(route+"/INVOICE"),input.replace("Version\":0","Version\":1"),"operator"),200);
        assertThat(audits()).isEqualTo(audits);
        status(write(put(route+"/INVOICE"),"{\"expectedProjectVersion\":1,\"data\":[{\"unitPrice\":\"5\"}]}","operator"),400);
        status(write(put(route+"/INVOICE"),"{\"expectedProjectVersion\":1,\"data\":[{\"date\":\"2026-02-30\"}]}","operator"),400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entity_table='shipment_form' AND actor_id=?",Long.class,operator)).isEqualTo(1);
    }

    private JsonNode create(String route, String input) throws Exception {
        var r = write(post(route), input, "admin");
        status(r, 201);
        return body(r);
    }

    private JsonNode read(String route, String subject) throws Exception {
        var r = mvc.perform(get(route).with(account(subject))).andReturn();
        status(r, 200);
        return body(r);
    }

    private MvcResult write(MockHttpServletRequestBuilder request, String input, String subject) throws Exception {
        return mvc.perform(CommercialTestRequests.write(request.with(account(subject)), input)).andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void status(MvcResult result, int expected) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(expected);
    }

    private void problem(MvcResult result, int expected, String field) throws Exception {
        status(result, expected);
        assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
        if (field != null)assertThat(body(result).path("errors").findValuesAsText("field")).contains(field);
    }

    private long count(String table) {
        if (!List.of("product_category", "organization",
            "project_party").contains(table)) throw new IllegalArgumentException();
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private long audits() {
        return jdbc.queryForObject("SELECT count(*) FROM audit_log", Long.class);
    }

    private UUID user(String name, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,external_subject,display_name) VALUES (?,?,?)", id, name, name);
        jdbc.update("INSERT INTO role_assignment(user_id,role) VALUES (?,?)", id, role);
        jdbc.update("INSERT INTO local_account(user_id,username,password_hash,must_change_password) VALUES (?,?,?,false)", id, name, passwords.encode("FictionalTestPass1!"));
        return id;
    }

    private RequestPostProcessor account(String subject) {
        UUID id = jdbc.queryForObject("SELECT user_id FROM local_account WHERE username=?", UUID.class, subject);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(new AccountPrincipal(id, 0), null,
            List.of()));
    }

    private void grant(JsonNode project, String form) {
        UUID id = UUID.fromString(project.path("id").asText());
        jdbc.update("INSERT INTO project_access(project_id,user_id) VALUES (?,?)", id, operator);
        grantForm(project, form);
    }

    private void grantForm(JsonNode project, String form) {
        jdbc.update("INSERT INTO project_form_assignment(project_id,user_id,form_key,assigned_by) VALUES (?,?,?,?)", UUID.fromString(project.path("id").asText()), operator, form,
            admin);
    }
}

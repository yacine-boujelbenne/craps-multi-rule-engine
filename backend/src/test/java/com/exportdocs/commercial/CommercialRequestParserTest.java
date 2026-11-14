package com.exportdocs.commercial;

import com.exportdocs.catalog.api.CatalogRequestParser;
import com.exportdocs.party.api.OrganizationRequestParser;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.party.domain.OrganizationRole;
import com.exportdocs.party.domain.OrganizationAddress;
import com.exportdocs.project.api.CommercialPartiesRequestParser;
import com.exportdocs.project.application.ProjectProblem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.LinkedMultiValueMap;
import static org.assertj.core.api.Assertions.*;

class CommercialRequestParserTest {
    static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    final ObjectMapper json = new ObjectMapper();
    final OrganizationRequestParser organizations = new OrganizationRequestParser(FACTORY.getValidator());
    final CatalogRequestParser catalog = new CatalogRequestParser(FACTORY.getValidator());
    final CommercialPartiesRequestParser commercial = new CommercialPartiesRequestParser();
    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Test
    void preservesPatchPresenceAndNormalizesOnlyOptionalBlankText() throws Exception {
        var result = organizations.parse(json.readTree("{\"expectedVersion\":7,\"legalName\":\" Fictional Co \",\"addressText\":\"  \",\"registrationNumber\":null}"), true);
        assertThat(result.expectedVersion()).isEqualTo(7);
        assertThat(result.supplied()).containsExactlyInAnyOrder("legalName", "addressText",
            "registrationNumber");
        assertThat(result.fields().legalName()).isEqualTo("Fictional Co");
        assertThat(result.fields().addressText()).isNull();
        assertThat(result.fields().registrationNumber()).isNull();
        assertThat(result.fields().roles()).isNull();
        assertThat(result.fields().active()).isNull();
        assertThat(result.fields().tradeName()).isNull();
        assertThat(result.fields().taxIdentifier()).isNull();
        assertThat(result.fields().email()).isNull();
        assertThat(result.fields().phone()).isNull();
    }

    @Test
    void companyMetadataSupportsTrimmedNullableTextAndCarrierRole() throws Exception {
        var result = organizations.parse(json.readTree("""
            {"legalName":"Company","roles":["CARRIER","SHIPPING_LINE"],"tradeName":" Trade ",
             "taxIdentifier":" TAX-001 ","email":" Sales@example.test ","phone":" +216 00123456 "}
            """), false);
        assertThat(result.fields().tradeName()).isEqualTo("Trade");
        assertThat(result.fields().taxIdentifier()).isEqualTo("TAX-001");
        assertThat(result.fields().email()).isEqualTo("Sales@example.test");
        assertThat(result.fields().phone()).isEqualTo("+216 00123456");
        assertThat(result.fields().roles()).containsExactlyInAnyOrder(OrganizationRole.CARRIER, OrganizationRole.SHIPPING_LINE);
        var cleared = organizations.parse(json.readTree("""
            {"expectedVersion":3,"tradeName":null,"taxIdentifier":" ","email":" ","phone":null}
            """), true);
        assertThat(cleared.supplied()).containsExactlyInAnyOrder("tradeName", "taxIdentifier", "email", "phone");
        assertThat(cleared.fields().tradeName()).isNull();
        assertThat(cleared.fields().taxIdentifier()).isNull();
        assertThat(cleared.fields().email()).isNull();
        assertThat(cleared.fields().phone()).isNull();
        var query = new LinkedMultiValueMap<String, String>();
        query.add("role", "CARRIER");
        assertThat(SetupListQuery.parse(query, true).role()).isEqualTo("CARRIER");
    }

    @ParameterizedTest
    @ValueSource(strings = {"tradeName", "taxIdentifier", "email", "phone"})
    void companyMetadataRejectsWrongTypesAndOverlongText(String field) throws Exception {
        int max = switch (field) {
            case "tradeName" -> 250;
            case "taxIdentifier" -> 100;
            case "email" -> 254;
            default -> 50;
        };
        String boundary = field.equals("email") ? "a@" + "b".repeat(63) + "." + "c".repeat(63)
            + "." + "d".repeat(63) + "." + "e".repeat(60) : "x".repeat(max);
        assertThat(boundary).hasSize(max);
        var body = json.createObjectNode().put("expectedVersion", 0).put(field, boundary);
        organizations.parse(body, true);
        body.put(field, boundary + "x");
        assertThatThrownBy(() -> organizations.parse(body, true)).isInstanceOfSatisfying(ProjectProblem.class,
            problem -> assertThat(problem.errors()).extracting(ProjectProblem.FieldError::field).contains(field));
        for (String value : List.of("42", "true", "[]", "{}")) {
            var invalid = json.createObjectNode().put("expectedVersion", 0).set(field, json.readTree(value));
            assertThatThrownBy(() -> organizations.parse(invalid, true)).isInstanceOfSatisfying(ProjectProblem.class,
                problem -> assertThat(problem.errors()).extracting(ProjectProblem.FieldError::field).contains(field));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "a@@example.test", "a b@example.test"})
    void companyEmailRequiresValidEmailText(String email) {
        var body = json.createObjectNode().put("expectedVersion", 0).put("email", email);
        assertThatThrownBy(() -> organizations.parse(body, true)).isInstanceOfSatisfying(ProjectProblem.class,
            problem -> assertThat(problem.errors()).extracting(ProjectProblem.FieldError::field).contains("email"));
    }

    @Test
    void addressParserPreservesPostalZerosAndExplicitPatchPresence() throws Exception {
        var created = organizations.address(json.readTree("""
            {"kind":"BUSINESS","countryCode":"TN","manualCity":" Tunis ","addressLine1":" Road 1 ",
             "addressLine2":" ","postalCode":" 00123 ","expectedOrganizationVersion":3}
            """), false);
        assertThat(created.fields().kind()).isEqualTo(OrganizationAddress.Kind.BUSINESS);
        assertThat(created.fields().countryCode()).isEqualTo("TN");
        assertThat(created.fields().manualCity()).isEqualTo("Tunis");
        assertThat(created.fields().addressLine1()).isEqualTo("Road 1");
        assertThat(created.fields().addressLine2()).isNull();
        assertThat(created.fields().postalCode()).isEqualTo("00123");
        assertThat(created.fields().active()).isTrue();
        assertThat(created.expectedOrganizationVersion()).isEqualTo(3);
        var patched = organizations.address(json.readTree("""
            {"expectedVersion":2,"cityId":null,"manualCity":" New city ","postalCode":null,"active":false}
            """), true);
        assertThat(patched.expectedVersion()).isEqualTo(2);
        assertThat(patched.supplied()).containsExactlyInAnyOrder("cityId", "manualCity", "postalCode", "active");
        assertThat(patched.fields().cityId()).isNull();
        assertThat(patched.fields().postalCode()).isNull();
        assertThat(patched.fields().manualCity()).isEqualTo("New city");
        assertThat(patched.fields().kind()).isNull();
        assertThat(patched.fields().countryCode()).isNull();
        assertThat(patched.fields().active()).isFalse();
        var sparse = organizations.address(json.readTree("{\"expectedVersion\":0,\"addressLine1\":\"Road\"}"), true);
        assertThat(sparse.fields().active()).isNull();
        assertThat(sparse.supplied()).containsExactly("addressLine1");
        UUID city = UUID.randomUUID();
        var cityBody = json.createObjectNode().put("kind", "OTHER").put("countryCode", "TN")
            .put("addressLine1", "Road").put("cityId", city.toString());
        assertThat(organizations.address(cityBody, false).fields().cityId()).isEqualTo(city);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"0\"", "0.0", "-1", "9223372036854775808"})
    void addressCreateVersionGuardRequiresANonnegativeLongWhenPresent(String token) throws Exception {
        var body = json.readTree("{\"kind\":\"OTHER\",\"countryCode\":\"TN\",\"addressLine1\":\"Road\","
            + "\"manualCity\":\"Tunis\",\"expectedOrganizationVersion\":" + token + "}");
        assertThatThrownBy(() -> organizations.address(body, false)).isInstanceOfSatisfying(ProjectProblem.class,
            problem -> assertThat(problem.errors()).extracting(ProjectProblem.FieldError::field).contains("expectedOrganizationVersion"));
    }

    @Test
    void addressParserRejectsReadOnlyFieldsWrongTypesNullRequiredFieldsAndOversizedText() throws Exception {
        for (String field : List.of("kind", "countryCode", "addressLine1")) {
            var create = json.createObjectNode().put("kind", "BUSINESS").put("countryCode", "TN")
                .put("addressLine1", "Road");
            create.remove(field);
            assertThatThrownBy(() -> organizations.address(create, false)).isInstanceOf(ProjectProblem.class);
            for (String token : List.of("null", "42", "\" \"")) {
                var patch = json.createObjectNode().put("expectedVersion", 0).set(field, json.readTree(token));
                assertThatThrownBy(() -> organizations.address(patch, true)).isInstanceOf(ProjectProblem.class);
            }
        }
        for (String field : List.of("id", "organizationId", "version", "createdAt", "expectedOrganizationVersion", "addressText")) {
            var patch = json.createObjectNode().put("expectedVersion", 0).put(field, "unexpected");
            assertThatThrownBy(() -> organizations.address(patch, true)).isInstanceOf(ProjectProblem.class);
        }
        for (String[] invalid : List.of(new String[]{"kind", "\"UNKNOWN\""}, new String[]{"countryCode", "\"tn\""},
                new String[]{"cityId", "\"1-1-1-1-1\""}, new String[]{"postalCode", "123"},
                new String[]{"manualCity", "[]"}, new String[]{"addressLine2", "true"}, new String[]{"active", "null"})) {
            var patch = json.createObjectNode().put("expectedVersion", 0).set(invalid[0], json.readTree(invalid[1]));
            assertThatThrownBy(() -> organizations.address(patch, true)).isInstanceOf(ProjectProblem.class);
        }
        for (String field : List.of("manualCity", "addressLine1", "addressLine2", "postalCode")) {
            int max = field.equals("manualCity") ? 200 : field.equals("postalCode") ? 40 : 250;
            var patch = json.createObjectNode().put("expectedVersion", 0).put(field, "x".repeat(max));
            organizations.address(patch, true);
            patch.put(field, "x".repeat(max + 1));
            assertThatThrownBy(() -> organizations.address(patch, true)).isInstanceOf(ProjectProblem.class);
        }
        var create = json.createObjectNode().put("kind", "OTHER").put("countryCode", "TN").put("addressLine1", "Road")
            .put("active", false);
        assertThatThrownBy(() -> organizations.address(create, false)).isInstanceOf(ProjectProblem.class);
        assertThatThrownBy(() -> organizations.address(json.createObjectNode(), true)).isInstanceOf(ProjectProblem.class);
    }







    @Test
    void productAliasesNormalizeToOneNameAndPreserveSparseMetadataPatches() throws Exception {
        var created = catalog.product(json.readTree("""
            {"code":"P","commercialName":" Product ","description":" Description ",
             "processingMethod":" Frozen ","storageRequirements":" -18 C "}
            """), false);
        assertThat(created.fields().name()).isEqualTo("Product");
        assertThat(created.fields().defaultMeasurementUnit()).isEqualTo("KG");
        assertThat(created.fields().processingMethod()).isEqualTo("Frozen");
        assertThat(created.supplied()).contains("name").doesNotContain("commercialName");
        catalog.product(json.readTree("{\"code\":\"P\",\"name\":\" Product \",\"commercialName\":\"Product\"}"), false);
        var patch = catalog.product(json.readTree("{\"expectedVersion\":3,\"commercialName\":\" New \",\"description\":null}"), true);
        assertThat(patch.supplied()).containsExactlyInAnyOrder("name", "description");
        assertThat(patch.fields().name()).isEqualTo("New");
        assertThat(patch.fields().description()).isNull();
        assertThat(patch.fields().defaultMeasurementUnit()).isNull();
        assertThat(patch.fields().active()).isNull();
        for (String input : List.of("{\"code\":\"P\"}", "{\"code\":\"P\",\"name\":\"A\",\"commercialName\":\"B\"}")) {
            assertThatThrownBy(() -> catalog.product(json.readTree(input), false)).isInstanceOf(ProjectProblem.class);
        }
        for (String token : List.of("null", "\" \"", "42", "\"" + "x".repeat(31) + "\"")) {
            var invalid = json.createObjectNode().put("expectedVersion", 0).set("defaultMeasurementUnit", json.readTree(token));
            assertThatThrownBy(() -> catalog.product(invalid, true)).isInstanceOf(ProjectProblem.class);
        }
        for (String field : List.of("categories", "grades", "defaultPricePerKg", "unitPrice")) {
            var invalid = json.createObjectNode().put("expectedVersion", 0).put(field, "unexpected");
            assertThatThrownBy(() -> catalog.product(invalid, true)).isInstanceOf(ProjectProblem.class);
        }
        for (String field : List.of("processingMethod", "storageRequirements")) {
            int max = field.equals("processingMethod") ? 150 : 500;
            var invalid = json.createObjectNode().put("expectedVersion", 0).put(field, "x".repeat(max + 1));
            assertThatThrownBy(() -> catalog.product(invalid, true)).isInstanceOf(ProjectProblem.class);
        }
    }

    @Test
    void taxonomyParserKeepsPatchPresenceAndStrictUniqueAssignmentIds() throws Exception {
        var created = catalog.taxonomy(json.readTree("{\"code\":\" T \",\"name\":\" Taxonomy \",\"description\":\" \"}"), false);
        assertThat(created.fields().code()).isEqualTo("T");
        assertThat(created.fields().description()).isNull();
        assertThat(created.fields().active()).isTrue();
        var patch = catalog.taxonomy(json.readTree("{\"expectedVersion\":0,\"description\":null}"), true);
        assertThat(patch.supplied()).containsExactly("description");
        assertThat(patch.fields().active()).isNull();
        UUID category = UUID.randomUUID();
        var body = json.createObjectNode().put("expectedProductVersion", 2);
        body.putArray("categoryIds").add(category.toString());
        assertThat(catalog.assignment(body).categoryIds()).containsExactly(category);
        body.putArray("categoryIds");
        assertThat(catalog.assignment(body).categoryIds()).isEmpty();
        for (String token : List.of("null", "{}", "[null]", "[42]", "[\"1-1-1-1-1\"]",
                "[\"" + category + "\",\"" + category.toString().toUpperCase(java.util.Locale.ROOT) + "\"]")) {
            body.set("categoryIds", json.readTree(token));
            assertThatThrownBy(() -> catalog.assignment(body)).isInstanceOf(ProjectProblem.class);
        }
        body.putArray("categoryIds");
        for (String token : List.of("null", "\"0\"", "0.0", "-1", "9223372036854775808")) {
            body.set("expectedProductVersion", json.readTree(token));
            assertThatThrownBy(() -> catalog.assignment(body)).isInstanceOf(ProjectProblem.class);
        }
        for (String field : List.of("code", "name")) {
            var invalid = json.createObjectNode().put("expectedVersion", 0).putNull(field);
            assertThatThrownBy(() -> catalog.taxonomy(invalid, true)).isInstanceOf(ProjectProblem.class);
        }
        for (String field : List.of("id", "version", "categoryIds")) {
            var invalid = json.createObjectNode().put("expectedVersion", 0).put(field, "unexpected");
            assertThatThrownBy(() -> catalog.taxonomy(invalid, true)).isInstanceOf(ProjectProblem.class);
        }
    }

    @Test
    void exactDecimalStringsCanonicalizeWithoutLosingDigits() throws Exception {
        var result = catalog.grade(json.readTree("{\"code\":\" G \",\"name\":\" Grade \",\"defaultCartonNetKg\":\"999999999999999.123\",\"defaultPricePerKg\":\"99999999999999.1234\",\"defaultCurrency\":\"USD\"}"), false);
        assertThat(result.fields().defaultCartonNetKg().toPlainString()).isEqualTo("999999999999999.123");
        assertThat(result.fields().defaultPricePerKg().toPlainString()).isEqualTo("99999999999999.1234");
        assertThat(result.fields().code()).isEqualTo("G");
        var integer = catalog.grade(json.readTree("{\"code\":\"G\",\"name\":\"G\",\"defaultCartonNetKg\":\"12\",\"defaultPricePerKg\":\"0\",\"defaultCurrency\":\"USD\"}"), false);
        assertThat(integer.fields().defaultCartonNetKg().toPlainString()).isEqualTo("12.000");
        assertThat(integer.fields().defaultPricePerKg().toPlainString()).isEqualTo("0.0000");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0", "12.5", "\"0\"", "\"0.000\"", "\"1.0000\"",
            "\"1e2\"", "\"-1\"", "\"01\"", "\"+1\"", "\" 12 \"",
            "\"1000000000000000\"", "true", "[]"
    }
    )
    void rejectsInvalidWeightTokensBeforeDatabaseRounding(String token) {
        assertThatThrownBy(() -> catalog.grade(json.readTree("{\"code\":\"G\",\"name\":\"G\",\"defaultCartonNetKg\":" + token + "}"), false))
        .isInstanceOfSatisfying(ProjectProblem.class, problem -> {
            assertThat(problem.status()).isEqualTo(400);
            assertThat(problem.errors()).extracting(ProjectProblem.FieldError::field).contains("defaultCartonNetKg");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.2", "\"1.00000\"", "\"-0.1\"", "\"1e2\"", "\"100000000000000\""
    }
    )
    void rejectsInvalidPriceTokens(String token) {
        assertThatThrownBy(() -> catalog.grade(json.readTree("{\"code\":\"G\",\"name\":\"G\",\"defaultPricePerKg\":" + token + "}"), false))
        .isInstanceOf(ProjectProblem.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"expectedVersion\":null}", "{\"expectedVersion\":\"0\"}", "{\"expectedVersion\":0.0}", "{\"expectedVersion\":-1}",
            "{\"expectedVersion\":9223372036854775808}"
    }
    )
    void requiresActualNonnegativeLongVersion(String input) {
        assertThatThrownBy(() -> commercial.parse(json.readTree(input))).isInstanceOfSatisfying(ProjectProblem.class,
            problem -> assertThat(problem.errors()).extracting(ProjectProblem.FieldError::field).contains("expectedVersion"));
    }

    @Test
    void rejectsNullRequiredFieldsWrongTypesAndReadOnlyFields() {
        for (String input : List.of("{\"legalName\":null,\"roles\":[\"CLIENT\"]}", "{\"legalName\":\" \" ,\"roles\":[\"CLIENT\"]}", "{\"legalName\":7,\"roles\":[\"CLIENT\"]}", "{\"legalName\":\"X\",\"roles\":[1]}",
            "{\"legalName\":\"X\",\"roles\":[\"CLIENT\",\"CLIENT\"]}", "{\"legalName\":\"X\",\"roles\":null}", "{\"legalName\":\"X\",\"roles\":[]}", "{\"legalName\":\"X\",\"roles\":[\"CLIENT\"],\"active\":null}", "{\"legalName\":\"X\",\"roles\":[\"CLIENT\"],\"version\":0}")) {
            assertThatThrownBy(() -> organizations.parse(json.readTree(input),
                false)).as(input).isInstanceOf(ProjectProblem.class);
        }
        for (String input : List.of("{\"expectedVersion\":0,\"code\":null}", "{\"expectedVersion\":0,\"displayOrder\":null}", "{\"expectedVersion\":0,\"displayOrder\":1.0}", "{\"expectedVersion\":0,\"name\":null}",
            "{\"expectedVersion\":0,\"active\":0}", "{\"expectedVersion\":0,\"productId\":null}")) {
            assertThatThrownBy(() -> catalog.grade(json.readTree(input), true)).as(input).isInstanceOf(ProjectProblem.class);
        }
    }

    @Test
    void scalarUuidRolesAreIndependentAndNullClears() throws Exception {
        var id = UUID.randomUUID();
        var result = commercial.parse(json.readTree("{\"expectedVersion\":0,\"supplierId\":\"" + id + "\",\"clientId\":\"" + id + "\",\"consigneeId\":null}"));
        assertThat(result.fields().supplierId()).isEqualTo(result.fields().clientId());
        assertThat(result.supplied()).containsExactlyInAnyOrder("supplierId", "clientId",
            "consigneeId");
        for (String value : List.of("[]", "42", "\"1-1-1-1-1\"",
            "\" " + id + " \"")) {
            assertThatThrownBy(() -> commercial.parse(json.readTree("{\"expectedVersion\":0,\"supplierId\":" + value + "}"))).isInstanceOf(ProjectProblem.class);
        }
    }

    @Test
    void listingEscapesLiteralWildcardsAndRejectsUnknownRepeatedOrOverflow() {
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("q", "  A_!%  ");
        parameters.add("role", "CLIENT");
        var result = SetupListQuery.parse(parameters, true);
        assertThat(result.prefix()).isEqualTo("a!_!!!%%");
        assertThat(result.role()).isEqualTo("CLIENT");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        parameters.add("q", "again");
        assertThatThrownBy(() -> SetupListQuery.parse(parameters, true)).isInstanceOf(ProjectProblem.class);
        for (String key : List.of("role", "sort", "unknown")) {
            var invalid = new LinkedMultiValueMap<String, String>();
            invalid.add(key, "CLIENT");
            assertThatThrownBy(() -> SetupListQuery.parse(invalid, false)).isInstanceOf(ProjectProblem.class);
        }
        var overflow = new LinkedMultiValueMap<String, String>();
        overflow.add("page", "2147483647");
        overflow.add("size", "2");
        assertThatThrownBy(() -> SetupListQuery.parse(overflow, false)).isInstanceOf(ProjectProblem.class);
    }
}

package com.exportdocs.commercial;

import com.exportdocs.catalog.api.CatalogRequestParser;
import com.exportdocs.party.api.OrganizationRequestParser;
import com.exportdocs.party.api.SetupListQuery;
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

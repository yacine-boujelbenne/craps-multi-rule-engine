package com.exportdocs.project.api;

import com.exportdocs.project.application.ProjectProblem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectRequestParserTest {
    private final ObjectMapper json = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    private final ProjectRequestParser parser = new ProjectRequestParser(json,
            Validation.buildDefaultValidatorFactory().getValidator());

    @Test
    void referenceOnlyCreationNormalizesBeforeValidation() throws Exception {
        var request = parser.create(json.readTree("{\"reference\":\"  exp-2026/1  \"}"));
        assertThat(request.fields().reference()).isEqualTo("EXP-2026/1");
        assertThat(request.fields().producerIds()).isNull();
    }

    @Test
    void missingOrBlankReferenceIsRejected() throws Exception {
        for (String input : new String[]{"{}", "{\"reference\":null}", "{\"reference\":\"   \"}"}) {
            var body = json.readTree(input);
            assertThatThrownBy(() -> parser.create(body)).isInstanceOf(ProjectProblem.class);
        }
    }

    @Test
    void immutableAndUnknownPropertiesAreRejected() throws Exception {
        for (String field : new String[]{"id", "status", "createdAt", "version", "shipmentIds"}) {
            var body = json.readTree("{\"reference\":\"EXP-1\",\"" + field + "\":null}");
            assertThatThrownBy(() -> parser.create(body)).isInstanceOf(ProjectProblem.class);
        }
    }

    @Test
    void patchPreservesTheDifferenceBetweenOmissionAndNull() throws Exception {
        var omitted = parser.patch(json.readTree("{\"expectedVersion\":0}"));
        var cleared = parser.patch(json.readTree("{\"expectedVersion\":0,\"consigneeId\":null}"));
        assertThat(omitted.supplied()).doesNotContain("consigneeId");
        assertThat(cleared.supplied()).contains("consigneeId");
        assertThat(cleared.fields().consigneeId()).isNull();
    }

    @Test
    void patchRequiresAnIntegralNonnegativeVersion() throws Exception {
        for (String value : new String[]{"null", "-1", "0.5", "\"0\"", "9223372036854775808"}) {
            var body = json.readTree("{\"expectedVersion\":" + value + "}");
            assertThatThrownBy(() -> parser.patch(body)).isInstanceOf(ProjectProblem.class);
        }
        assertThatThrownBy(() -> parser.patch(json.createObjectNode())).isInstanceOf(ProjectProblem.class);
    }

    @Test
    void patchRejectsNullReferenceAndProducerList() throws Exception {
        for (String field : new String[]{"reference", "producerIds"}) {
            var body = json.readTree("{\"expectedVersion\":0,\"" + field + "\":null}");
            assertThatThrownBy(() -> parser.patch(body)).isInstanceOf(ProjectProblem.class);
        }
    }

    @Test
    void rejectsWrongTypesInvalidDatesAndDuplicateProducers() throws Exception {
        for (String fields : new String[]{
                "\"reference\":42", "\"notes\":42", "\"expectedDepartureDate\":\"2026-02-30\"",
                "\"exporterId\":\"not-a-uuid\"", "\"producerIds\":[null]",
                "\"producerIds\":[\"11111111-1111-1111-1111-111111111111\",\"11111111-1111-1111-1111-111111111111\"]"}) {
            var body = json.readTree("{\"reference\":\"EXP-1\"," + fields + "}");
            assertThatThrownBy(() -> parser.create(body)).isInstanceOf(ProjectProblem.class);
        }
    }

    @Test
    void acceptsAnEmptyProducerReplacementAndHistoricalDepartureDate() throws Exception {
        var patch = parser.patch(json.readTree("{\"expectedVersion\":2,\"producerIds\":[],\"expectedDepartureDate\":\"2000-01-01\"}"));
        assertThat(patch.fields().producerIds()).isEmpty();
        assertThat(patch.fields().expectedDepartureDate()).hasToString("2000-01-01");
    }

    @Test
    void emptyOrEncodedReferencesAndDatesNeverSilentlyClearFields() throws Exception {
        for (String field : new String[]{"consigneeId", "portOfLoadingId", "expectedDepartureDate"}) {
            for (String value : new String[]{"", "  ", "EREREREREREREREREREREQ=="}) {
                var body = json.createObjectNode().put("expectedVersion", 0).put(field, value);
                assertThatThrownBy(() -> parser.patch(body)).isInstanceOf(ProjectProblem.class);
            }
        }
        var producers = json.readTree("{\"expectedVersion\":0,\"producerIds\":[\"EREREREREREREREREREREQ==\"]}");
        assertThatThrownBy(() -> parser.patch(producers)).isInstanceOf(ProjectProblem.class);
    }
}

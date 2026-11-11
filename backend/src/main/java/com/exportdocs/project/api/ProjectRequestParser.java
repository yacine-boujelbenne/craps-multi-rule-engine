package com.exportdocs.project.api;

import com.exportdocs.project.application.ProjectFields;
import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.application.ProjectWrite;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProjectRequestParser {
    private static final Set<String> FIELDS = Set.of("reference", "exporterId", "producerIds",
            "consigneeId", "notifyPartyId", "freightForwarderId", "shippingLineId",
            "destinationCountryCode", "portOfLoadingId", "portOfDischargeId", "expectedDepartureDate", "notes");
    private static final Set<String> STRINGS = Set.of("reference", "exporterId", "consigneeId",
            "notifyPartyId", "freightForwarderId", "shippingLineId", "destinationCountryCode",
            "portOfLoadingId", "portOfDischargeId", "expectedDepartureDate", "notes");
    private static final Set<String> UUID_FIELDS = Set.of("exporterId", "consigneeId", "notifyPartyId",
            "freightForwarderId", "shippingLineId", "portOfLoadingId", "portOfDischargeId");
    private static final java.util.regex.Pattern UUID_TEXT = java.util.regex.Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private final ObjectMapper json;
    private final Validator validator;

    public ProjectRequestParser(ObjectMapper json, Validator validator) {
        this.json = json;
        this.validator = validator;
    }

    public ProjectWrite create(JsonNode body) { return parse(body, false); }
    public ProjectWrite patch(JsonNode body) { return parse(body, true); }

    private ProjectWrite parse(JsonNode body, boolean patch) {
        if (!(body instanceof ObjectNode object)) throw invalid("body", "Expected a JSON object.");
        Set<String> supplied = new HashSet<>();
        object.fieldNames().forEachRemaining(supplied::add);
        for (String field : supplied) {
            if (!FIELDS.contains(field) && !(patch && field.equals("expectedVersion"))) {
                throw invalid(field, "Unknown or read-only field.");
            }
        }
        Long expectedVersion = null;
        if (patch) {
            JsonNode version = object.get("expectedVersion");
            if (version == null || !version.isIntegralNumber() || !version.canConvertToLong() || version.longValue() < 0) {
                throw invalid("expectedVersion", "A nonnegative integer version is required.");
            }
            expectedVersion = version.longValue();
        }
        if ((!patch || supplied.contains("reference")) &&
                (!object.hasNonNull("reference") || !object.get("reference").isTextual())) {
            throw invalid("reference", "A project reference is required.");
        }
        for (String field : STRINGS) {
            if (object.hasNonNull(field) && !object.get(field).isTextual()) {
                throw invalid(field, "Expected a string or null.");
            }
        }
        for (String field : UUID_FIELDS) {
            if (object.hasNonNull(field) && !UUID_TEXT.matcher(object.get(field).textValue()).matches()) {
                throw invalid(field, "Expected a UUID in hyphenated text format.");
            }
        }
        if (object.hasNonNull("expectedDepartureDate") &&
                !object.get("expectedDepartureDate").textValue().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw invalid("expectedDepartureDate", "Expected a valid date in YYYY-MM-DD format.");
        }
        if (object.has("producerIds")) {
            JsonNode producers = object.get("producerIds");
            if (!producers.isArray()) throw invalid("producerIds", "Expected an array; use [] to clear producers.");
            for (JsonNode producer : producers) {
                if (!producer.isTextual()) throw invalid("producerIds", "Every producer must be a UUID string.");
                if (!UUID_TEXT.matcher(producer.textValue()).matches()) {
                    throw invalid("producerIds", "Every producer must be a UUID in hyphenated text format.");
                }
            }
        }
        ObjectNode fields = object.deepCopy();
        fields.remove("expectedVersion");
        ProjectFields values;
        try {
            values = json.treeToValue(fields, ProjectFields.class);
        } catch (JsonProcessingException exception) {
            String field = exception instanceof com.fasterxml.jackson.databind.JsonMappingException mapping && !mapping.getPath().isEmpty()
                    ? mapping.getPath().getFirst().getFieldName() : "body";
            throw invalid(field == null ? "body" : field, "Invalid field format or value.");
        }
        var errors = new ArrayList<>(validator.validate(values).stream()
                .map(v -> new ProjectProblem.FieldError(v.getPropertyPath().toString(), "invalid", v.getMessage())).toList());
        if (values.producerIds() != null && new HashSet<>(values.producerIds()).size() != values.producerIds().size()) {
            errors.add(new ProjectProblem.FieldError("producerIds", "duplicate", "Producer IDs must be unique."));
        }
        if (!errors.isEmpty()) throw new ProjectProblem(400, "Request validation failed.", errors);
        supplied.remove("expectedVersion");
        return new ProjectWrite(values, supplied, expectedVersion);
    }

    private ProjectProblem invalid(String field, String message) {
        return ProjectProblem.field(400, field, "invalid", message);
    }
}

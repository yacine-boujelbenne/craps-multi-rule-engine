package com.exportdocs.party.api;

import com.exportdocs.project.application.ProjectProblem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Strict JSON primitives shared by the directory and catalog request parsers. */
public final class SetupJson {
    private final ObjectNode body;
    private final Set<String> supplied = new HashSet<>();
    private final boolean patch;

    public SetupJson(JsonNode input, Set<String> fields, boolean patch) {
        if (!(input instanceof ObjectNode object)) throw invalid("body", "Expected a JSON object.");
        this.body = object;
        this.patch = patch;
        object.fieldNames().forEachRemaining(supplied::add);
        for (String field : supplied) if (!fields.contains(field) && !(patch && field.equals("expectedVersion"))) throw invalid(field,
            "Unknown or read-only field.");
    }

    public Set<String> supplied() {
        var fields = new HashSet<>(supplied);
        fields.remove("expectedVersion");
        return Set.copyOf(fields);
    }

    public Long version() {
        if (!patch) return null;
        var value = body.get("expectedVersion");
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue()<0) throw invalid("expectedVersion",
            "A nonnegative integer version is required.");
        return value.longValue();
    }

    public String text(String field, boolean nullable, boolean required, boolean blankToNull) {
        var value = body.get(field);
        if (value == null) {
            if (!patch && required) throw invalid(field, "A value is required.");
            return null;
        }
        if (value.isNull()) {
            if (!nullable) throw invalid(field, "Null is not allowed.");
            return null;
        }
        if (!value.isTextual()) throw invalid(field, "Expected a string.");
        String text = value.textValue().trim();
        if (text.isEmpty()) {
            if (blankToNull) return null;
            if (!nullable) throw invalid(field, "A nonblank value is required.");
        }
        return text;
    }

    public Boolean active() {
        var value = body.get("active");
        if (value == null) return patch ? null : true;
        if (!value.isBoolean()) throw invalid("active", "Expected a boolean.");
        return value.booleanValue();
    }

    public Integer order() {
        var value = body.get("displayOrder");
        if (value == null) return patch ? null : 0;
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue()<0) throw invalid("displayOrder",
            "Expected a nonnegative integer.");
        return value.intValue();
    }

    public UUID uuid(String field) {
        String text = text(field, true, false, false);
        if (text == null) return null;
        // Do not accept shortened UUID groups or surrounding whitespace.
        if (!body.get(field).textValue().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw invalid(field, "Expected a hyphenated UUID string.");
        return UUID.fromString(text);
    }

    public <E extends Enum<E>> Set<E> roles(Class<E> type) {
        var value = body.get("roles");
        if (value == null && patch) return null;
        if (value == null || !value.isArray() || value.isEmpty() || value.size()>type.getEnumConstants().length) throw invalid("roles",
            "A nonempty array of unique role strings is required.");
        Set<E> roles = new HashSet<>();
        for (var role : value) {
            if (!role.isTextual()) throw invalid("roles", "Expected role strings.");
            E parsed;
            try {
                parsed = Enum.valueOf(type, role.textValue());
            } catch (IllegalArgumentException exception) {
                throw invalid("roles", "Unknown organization role.");
            }
            if (!roles.add(parsed)) throw invalid("roles", "Roles must be unique.");
        }
        return Set.copyOf(roles);
    }

    public static <T> T validate(T fields, Validator validator) {
        var errors = validator.validate(fields).stream().map(v -> new ProjectProblem.FieldError(v.getPropertyPath().toString(),
            "invalid", v.getMessage())).toList();
        if (!errors.isEmpty()) throw new ProjectProblem(400, "Request validation failed.", errors);
        return fields;
    }

    public static ProjectProblem invalid(String field, String message) {
        return ProjectProblem.field(400, field, "invalid", message);
    }
}

package com.exportdocs.project.api;

import com.exportdocs.party.api.SetupJson;
import com.exportdocs.project.application.ShipmentProductWrite;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShipmentProductRequestParser {
    private final Validator validator;

    public ShipmentProductRequestParser(Validator validator) { this.validator = validator; }

    public ShipmentProductWrite parse(JsonNode body, boolean patch) {
        var allowed = patch ? Set.of("expectedVersion", "expectedProjectVersion", "productCategoryId", "cartonCount",
            "quantity", "quantityUnit", "netWeightKg", "grossWeightKg", "unitPrice", "currency", "lotReference",
            "processingInformation") : Set.of("productId", "expectedProjectVersion", "productCategoryId", "cartonCount",
            "quantity", "quantityUnit", "netWeightKg", "grossWeightKg", "unitPrice", "currency", "lotReference",
            "processingInformation");
        var json = new SetupJson(body, allowed, false);
        long projectVersion = version(body, "expectedProjectVersion");
        Long rowVersion = patch ? version(body, "expectedVersion") : null;
        UUID productId = patch ? null : json.uuid("productId");
        if (!patch && productId == null) throw SetupJson.invalid("productId", "A product is required.");
        Integer cartons = integer(body, "cartonCount");
        var fields = new Fields(text(body, "quantityUnit"), text(body, "lotReference"),
            text(body, "processingInformation"), text(body, "currency"));
        SetupJson.validate(fields, validator);
        return new ShipmentProductWrite(productId, json.uuid("productCategoryId"), cartons,
            decimal(body, "quantity", 3, 15, true), fields.quantityUnit(), decimal(body, "netWeightKg", 3, 15, true),
            decimal(body, "grossWeightKg", 3, 15, true), decimal(body, "unitPrice", 4, 14, false),
            fields.currency(), fields.lotReference(), fields.processingInformation(), supplied(json.supplied()), projectVersion, rowVersion);
    }

    private Set<String> supplied(Set<String> supplied) {
        var fields = new java.util.HashSet<>(supplied);
        fields.remove("expectedProjectVersion");
        fields.remove("expectedVersion");
        return Set.copyOf(fields);
    }

    private long version(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0)
            throw SetupJson.invalid(field, "A nonnegative integer version is required.");
        return value.longValue();
    }

    private Integer integer(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 1)
            throw SetupJson.invalid(field, "Expected a positive integer.");
        return value.intValue();
    }

    private BigDecimal decimal(JsonNode body, String field, int scale, int integerDigits, boolean positive) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) return null;
        String regex = "[0-9]{1," + integerDigits + "}(?:\\.[0-9]{1," + scale + "})?";
        if (!value.isTextual() || !value.textValue().matches(regex)) throw SetupJson.invalid(field, "Expected an exact decimal string.");
        BigDecimal result = new BigDecimal(value.textValue());
        if (positive && result.signum() == 0) throw SetupJson.invalid(field, "Value must be positive.");
        return result.setScale(scale);
    }

    private String text(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw SetupJson.invalid(field, "Expected a string.");
        String text = value.textValue().trim();
        return text.isEmpty() ? null : text;
    }

    private record Fields(@Size(max = 30) String quantityUnit, @Size(max = 150) String lotReference,
        String processingInformation, @Size(max = 3) @Pattern(regexp = "[A-Z]{3}") String currency) {}
}

package com.exportdocs.catalog.api;

import com.exportdocs.catalog.application.GradeWrite;
import com.exportdocs.catalog.application.ProductWrite;
import com.exportdocs.party.api.SetupJson;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CatalogRequestParser {
    private final Validator validator;

    public CatalogRequestParser(Validator validator) {
        this.validator = validator;
    }

    public ProductWrite product(JsonNode body, boolean patch) {
        var json = new SetupJson(body, Set.of("code", "name", "scientificName",
            "hsCode", "active"), patch);
        Long version = json.version();
        var f = new ProductWrite.Fields(json.text("code", false, true, false), json.text("name",
            false, true, false), json.text("scientificName", true, false, true), json.text("hsCode", true, false,
            false), json.active());
        return new ProductWrite(SetupJson.validate(f, validator), json.supplied(), version);
    }

    public GradeWrite grade(JsonNode body, boolean patch) {
        var json = new SetupJson(body, Set.of("code", "name", "displayOrder",
            "defaultCartonNetKg", "defaultPricePerKg", "defaultCurrency", "active"), patch);
        Long version = json.version();
        var f = new GradeWrite.Fields(json.text("code", false, true, false), json.text("name",
            false, true, false), json.order(), decimal(body, "defaultCartonNetKg", 3, 15, true), decimal(body,
            "defaultPricePerKg", 4, 14, false), json.text("defaultCurrency", true, false, false), json.active());
        return new GradeWrite(SetupJson.validate(f, validator), json.supplied(), version);
    }

    private BigDecimal decimal(JsonNode body, String field, int scale, int integerDigits, boolean positive) {
        var value = body.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual() || !value.textValue().matches("(?:0|[1-9][0-9]{0," + (integerDigits - 1) + "})(?:\\.[0-9]{1," + scale + "})?")) throw SetupJson.invalid(field,
            "Expected an exact decimal string within the supported precision.");
        var decimal = new BigDecimal(value.textValue());
        if (positive && decimal.signum() == 0) throw SetupJson.invalid(field, "Weight must be positive.");
        return decimal.setScale(scale);
    }
}

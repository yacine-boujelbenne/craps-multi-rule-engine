package com.exportdocs.catalog.api;

import com.exportdocs.catalog.application.GradeWrite;
import com.exportdocs.catalog.application.ProductWrite;
import com.exportdocs.catalog.application.TaxonomyWrite;
import com.exportdocs.party.api.SetupJson;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CatalogRequestParser {
    private final Validator validator;

    public CatalogRequestParser(Validator validator) {
        this.validator = validator;
    }

    public ProductWrite product(JsonNode body, boolean patch) {
        var json = new SetupJson(body, Set.of("code", "name", "commercialName", "scientificName", "description",
            "processingMethod", "storageRequirements", "defaultMeasurementUnit", "hsCode", "active"), patch);
        Long version = json.version();
        String name = json.text("name", false, false, false);
        String commercialName = json.text("commercialName", false, false, false);
        if (name != null && commercialName != null && !name.equals(commercialName)) throw SetupJson.invalid("commercialName",
            "Both name aliases must contain the same value.");
        if (name == null) name = commercialName;
        if (!patch && name == null) throw SetupJson.invalid("commercialName", "A product name is required.");
        String unit = json.text("defaultMeasurementUnit", false, false, false);
        if (!patch && unit == null) unit = "KG";
        var f = new ProductWrite.Fields(json.text("code", false, true, false), name,
            json.text("scientificName", true, false, true), json.text("hsCode", true, false, false), json.active(),
            json.text("description", true, false, true), json.text("processingMethod", true, false, true),
            json.text("storageRequirements", true, false, true), unit);
        var supplied = new HashSet<>(json.supplied());
        if (supplied.remove("commercialName")) supplied.add("name");
        return new ProductWrite(SetupJson.validate(f, validator), Set.copyOf(supplied), version);
    }

    public TaxonomyWrite taxonomy(JsonNode body, boolean patch) {
        var allowed = patch ? Set.of("code", "name", "description", "active") : Set.of("code", "name", "description");
        var json = new SetupJson(body, allowed, patch);
        Long version = json.version();
        var fields = new TaxonomyWrite.Fields(json.text("code", false, true, false), json.text("name", false, true, false),
            json.text("description", true, false, true), patch ? json.active() : Boolean.TRUE);
        return new TaxonomyWrite(SetupJson.validate(fields, validator), json.supplied(), version);
    }

    public TaxonomyWrite.Assignment assignment(JsonNode body) {
        new SetupJson(body, Set.of("expectedProductVersion", "categoryIds"), false);
        var version = body.get("expectedProductVersion");
        if (version == null || !version.isIntegralNumber() || !version.canConvertToLong() || version.longValue() < 0)
            throw SetupJson.invalid("expectedProductVersion", "A nonnegative product version is required.");
        var values = body.get("categoryIds");
        if (values == null || !values.isArray()) throw SetupJson.invalid("categoryIds", "A unique array of category UUIDs is required.");
        var ids = new HashSet<UUID>();
        for (var value : values) {
            if (!value.isTextual() || !value.textValue().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
                throw SetupJson.invalid("categoryIds", "Expected canonical category UUID strings.");
            if (!ids.add(UUID.fromString(value.textValue()))) throw SetupJson.invalid("categoryIds", "Category IDs must be unique.");
        }
        return new TaxonomyWrite.Assignment(version.longValue(), Set.copyOf(ids));
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

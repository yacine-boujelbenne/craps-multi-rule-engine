package com.exportdocs.party.api;

import com.exportdocs.party.application.OrganizationWrite;
import com.exportdocs.party.application.AddressWrite;
import com.exportdocs.party.domain.OrganizationAddress;
import com.exportdocs.party.domain.OrganizationRole;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.HashSet;
import org.springframework.stereotype.Component;

@Component
public class OrganizationRequestParser {
    private final Validator validator;

    public OrganizationRequestParser(Validator validator) {
        this.validator = validator;
    }

    public OrganizationWrite parse(JsonNode body, boolean patch) {
        var json = new SetupJson(body, Set.of("legalName", "countryCode", "addressText",
            "registrationNumber", "tradeName", "taxIdentifier", "email", "phone", "roles", "active"), patch);
        Long version = json.version();
        var fields = new OrganizationWrite.Fields(json.text("legalName", false, true, false),
            json.text("countryCode", true, false, false), json.text("addressText", true, false, true),
            json.text("registrationNumber", true, false, true), json.text("tradeName", true, false, true),
            json.text("taxIdentifier", true, false, true), json.text("email", true, false, true),
            json.text("phone", true, false, true), json.roles(OrganizationRole.class), json.active());
        return new OrganizationWrite(SetupJson.validate(fields, validator), json.supplied(), version);
    }

    public AddressWrite address(JsonNode body, boolean patch) {
        var allowed = new HashSet<>(Set.of("kind", "countryCode", "cityId", "manualCity",
            "addressLine1", "addressLine2", "postalCode"));
        allowed.add(patch ? "active" : "expectedOrganizationVersion");
        var json = new SetupJson(body, allowed, patch);
        Long version = json.version(), organizationVersion = null;
        if (!patch && body.has("expectedOrganizationVersion")) {
            var value = body.get("expectedOrganizationVersion");
            if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0)
                throw SetupJson.invalid("expectedOrganizationVersion", "A nonnegative integer version is required.");
            organizationVersion = value.longValue();
        }
        String kind = json.text("kind", false, true, false);
        OrganizationAddress.Kind parsedKind = null;
        if (kind != null) try {
            parsedKind = OrganizationAddress.Kind.valueOf(kind);
        } catch (IllegalArgumentException exception) {
            throw SetupJson.invalid("kind", "Unknown address kind.");
        }
        var fields = new AddressWrite.Fields(parsedKind, json.text("countryCode", false, true, false),
            json.uuid("cityId"), json.text("manualCity", true, false, true),
            json.text("addressLine1", false, true, false), json.text("addressLine2", true, false, true),
            json.text("postalCode", true, false, true), patch ? json.active() : Boolean.TRUE);
        return new AddressWrite(SetupJson.validate(fields, validator), json.supplied(), version, organizationVersion);
    }
}

package com.exportdocs.party.api;

import com.exportdocs.party.application.OrganizationWrite;
import com.exportdocs.party.domain.OrganizationRole;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OrganizationRequestParser {
    private final Validator validator;

    public OrganizationRequestParser(Validator validator) {
        this.validator = validator;
    }

    public OrganizationWrite parse(JsonNode body, boolean patch) {
        var json = new SetupJson(body, Set.of("legalName", "countryCode", "addressText",
            "registrationNumber", "roles", "active"), patch);
        Long version = json.version();
        var fields = new OrganizationWrite.Fields(json.text("legalName", false, true, false),
            json.text("countryCode", true, false, false), json.text("addressText", true, false, true),
            json.text("registrationNumber", true, false, true), json.roles(OrganizationRole.class), json.active());
        return new OrganizationWrite(SetupJson.validate(fields, validator), json.supplied(), version);
    }
}

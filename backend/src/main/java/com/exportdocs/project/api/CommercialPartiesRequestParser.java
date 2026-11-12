package com.exportdocs.project.api;

import com.exportdocs.party.api.SetupJson;
import com.exportdocs.project.application.CommercialPartiesWrite;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CommercialPartiesRequestParser {
    public CommercialPartiesWrite parse(JsonNode body) {
        var json = new SetupJson(body, Set.of("supplierId", "clientId", "importerId",
            "consigneeId"), true);
        long version = json.version();
        return new CommercialPartiesWrite(new CommercialPartiesWrite.Fields(json.uuid("supplierId"),
            json.uuid("clientId"), json.uuid("importerId"), json.uuid("consigneeId")), json.supplied(),
            version);
    }
}

package com.exportdocs.project.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CommercialPartiesWrite(Fields fields, Set<String> supplied, long expectedVersion) {
    public record Fields(UUID supplierId, UUID clientId, UUID importerId, UUID consigneeId) {
        public Map<String, UUID> values() {
            Map<String, UUID> values = new HashMap<>();
            values.put("supplierId", supplierId);
            values.put("clientId", clientId);
            values.put("importerId", importerId);
            values.put("consigneeId", consigneeId);
            return values;
        }
    }
}

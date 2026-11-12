package com.exportdocs.project.api;

import java.util.UUID;

public record CommercialPartiesResponse(UUID projectId, long version, UUID supplierId, UUID clientId,
    UUID importerId, UUID consigneeId) {
}

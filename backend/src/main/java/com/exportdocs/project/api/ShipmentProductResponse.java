package com.exportdocs.project.api;

import java.util.UUID;

public record ShipmentProductResponse(UUID id, long version, UUID productId, UUID productCategoryId,
    Integer cartonCount, String quantity, String quantityUnit, String netWeightKg, String grossWeightKg,
    String unitPrice, String currency, String lotReference, String processingInformation,
    Long appliedGradeVersion, String appliedGradeCode, String appliedGradeName, String appliedCartonNetKg,
    long projectVersion) {}

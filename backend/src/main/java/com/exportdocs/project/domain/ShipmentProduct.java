package com.exportdocs.project.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentProduct(UUID id, long version, UUID productId, UUID productCategoryId,
    Integer cartonCount, BigDecimal quantity, String quantityUnit, BigDecimal netWeightKg, BigDecimal grossWeightKg,
    BigDecimal unitPrice, String currency, String lotReference, String processingInformation,
    Long appliedGradeVersion, String appliedGradeCode, String appliedGradeName, BigDecimal appliedCartonNetKg) {}

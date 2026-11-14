package com.exportdocs.project.application;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ShipmentProductWrite(UUID productId, UUID productCategoryId, Integer cartonCount,
    BigDecimal quantity, String quantityUnit, BigDecimal netWeightKg, BigDecimal grossWeightKg,
    BigDecimal unitPrice, String currency, String lotReference, String processingInformation,
    Set<String> supplied, long expectedProjectVersion, Long expectedVersion) {}

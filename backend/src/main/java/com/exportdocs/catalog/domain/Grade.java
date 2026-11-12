package com.exportdocs.catalog.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Grade(UUID id, UUID productId, long version, String code, String name, int displayOrder,
    BigDecimal defaultCartonNetKg, BigDecimal defaultPricePerKg, String defaultCurrency, boolean active) {
}

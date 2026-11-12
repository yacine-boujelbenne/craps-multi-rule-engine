package com.exportdocs.catalog.api;

import java.util.List;
import java.util.UUID;

public final class CatalogDtos {
    private CatalogDtos() {
    }

    public record ProductResponse(UUID id, long version, String code, String name, String scientificName, String hsCode,
        boolean active) {
    }

    public record GradeResponse(UUID id, UUID productId, long version, String code, String name, int displayOrder,
        String defaultCartonNetKg, String defaultPricePerKg, String defaultCurrency, boolean active) {
    }

    public record ProductPage(List<ProductResponse> items, int page, int size, long totalElements, int totalPages) {
    }

    public record GradePage(List<GradeResponse> items, int page, int size, long totalElements, int totalPages) {
    }
}

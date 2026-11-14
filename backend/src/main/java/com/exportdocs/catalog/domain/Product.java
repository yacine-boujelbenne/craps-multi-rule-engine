package com.exportdocs.catalog.domain;

import java.util.UUID;
import java.util.List;

public record Product(UUID id, long version, String code, String name, String scientificName, String hsCode,
    boolean active, String description, String processingMethod, String storageRequirements, String defaultMeasurementUnit,
    List<TaxonomyCategory> categories, List<Grade> grades) {
}

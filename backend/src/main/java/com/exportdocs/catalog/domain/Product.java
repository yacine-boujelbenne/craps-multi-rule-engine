package com.exportdocs.catalog.domain;

import java.util.UUID;

public record Product(UUID id, long version, String code, String name, String scientificName, String hsCode,
    boolean active) {
}

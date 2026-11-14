package com.exportdocs.catalog.domain;

import java.util.UUID;

public record TaxonomyCategory(UUID id, long version, String code, String name, String description, boolean active) {}

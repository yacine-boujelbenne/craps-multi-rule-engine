package com.exportdocs.catalog.application;

import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record TaxonomyWrite(Fields fields, Set<String> supplied, Long expectedVersion) {
    public record Fields(@Size(min = 1, max = 80) String code, @Size(min = 1, max = 200) String name,
        @Size(max = 500) String description, Boolean active) {}
    public record Assignment(long expectedProductVersion, Set<UUID> categoryIds) {}
}

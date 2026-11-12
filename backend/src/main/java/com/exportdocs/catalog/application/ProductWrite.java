package com.exportdocs.catalog.application;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

public record ProductWrite(Fields fields, Set<String> supplied, Long expectedVersion) {
    public record Fields(@Size(min = 1, max = 80) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/+-]{0,79}") String code, @Size(min = 1,
        max = 250) String name, @Size(max = 250) String scientificName, @Pattern(regexp = "[0-9]{6,10}") String hsCode,
        Boolean active) {
    }
}

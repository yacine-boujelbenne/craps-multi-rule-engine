package com.exportdocs.project.application;

import java.util.Set;

public record ProjectWrite(ProjectFields fields, Set<String> supplied, Long expectedVersion) {
    public ProjectWrite { supplied = Set.copyOf(supplied); }
}

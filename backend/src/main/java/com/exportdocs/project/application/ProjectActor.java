package com.exportdocs.project.application;

import java.util.Set;
import java.util.UUID;

public record ProjectActor(UUID id, Set<String> roles) {
    public ProjectActor { roles = Set.copyOf(roles); }
    public boolean admin() { return roles.contains("ADMIN"); }
    public boolean canWrite() { return admin() || roles.contains("OPERATOR"); }
    public boolean canRead() { return canWrite() || roles.contains("VIEWER") || roles.contains("REVIEWER"); }
}

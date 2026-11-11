package com.exportdocs.project.application;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectActorTest {
    @Test
    void administratorsHaveReadAndWritePermission() {
        var actor = actor("ADMIN");
        assertTrue(actor.admin());
        assertTrue(actor.canRead());
        assertTrue(actor.canWrite());
    }

    @Test
    void operatorsHaveReadAndWritePermissionWithoutAdminAccess() {
        var actor = actor("OPERATOR");
        assertFalse(actor.admin());
        assertTrue(actor.canRead());
        assertTrue(actor.canWrite());
    }

    @Test
    void reviewersAndViewersCanOnlyRead() {
        for (String role : Set.of("REVIEWER", "VIEWER")) {
            var actor = actor(role);
            assertTrue(actor.canRead());
            assertFalse(actor.canWrite());
        }
    }

    @Test
    void missingAndUnknownRolesGrantNoPermission() {
        for (var roles : java.util.List.of(Set.<String>of(), Set.of("UNKNOWN"))) {
            var actor = new ProjectActor(UUID.randomUUID(), roles);
            assertFalse(actor.canRead());
            assertFalse(actor.canWrite());
        }
    }

    @Test
    void roleSnapshotCannotBeChangedByMutatingTheSourceSet() {
        Set<String> roles = new HashSet<>(Set.of("VIEWER"));
        var actor = new ProjectActor(UUID.randomUUID(), roles);
        roles.add("ADMIN");
        assertFalse(actor.admin());
        assertFalse(actor.canWrite());
    }

    private ProjectActor actor(String role) { return new ProjectActor(UUID.randomUUID(), Set.of(role)); }
}

package com.exportdocs.project.infrastructure;

import com.exportdocs.project.application.ProjectActor;
import com.exportdocs.project.application.ProjectProblem;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ProjectActorProvider {
    private final JdbcTemplate jdbc;

    public ProjectActorProvider(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public ProjectActor current() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ProjectProblem(401, "Authentication required.", List.of());
        }
        var users = jdbc.query("SELECT id FROM app_user WHERE external_subject=? AND active",
                (rs, row) -> rs.getObject("id", UUID.class), authentication.getName());
        if (users.isEmpty()) throw new ProjectProblem(403, "An active application user is required.", List.of());
        UUID id = users.getFirst();
        var roles = jdbc.queryForList("SELECT role FROM role_assignment WHERE user_id=?", String.class, id);
        return new ProjectActor(id, new HashSet<>(roles));
    }
}

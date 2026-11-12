package com.exportdocs.party.infrastructure;

import com.exportdocs.project.application.ProjectProblem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Coordinates directory changes with all APIs that bind companies to shipments. */
@Repository
public class OrganizationSelectionStore {
    private final JdbcTemplate jdbc;

    public OrganizationSelectionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProjectProblem.FieldError> validate(Map<String, List<UUID>> selections, Map<String, String> roles) {
        // Lock all companies in the same order to avoid cross-project lock cycles.
        var ids = selections.values().stream().flatMap(List::stream).distinct().sorted().toList();
        Map<UUID, Boolean> active = new HashMap<>();
        for (UUID id : ids) {
            var rows = jdbc.query("SELECT active FROM organization WHERE id=? FOR SHARE", (rs, row) -> rs.getBoolean(1), id);
            active.put(id, !rows.isEmpty() && rows.getFirst());
        }
        var errors = new ArrayList<ProjectProblem.FieldError>();
        selections.forEach((field, values) -> {
            for (UUID id : values) {
                boolean validRole = Boolean.TRUE.equals(jdbc.queryForObject("""
                        SELECT EXISTS (SELECT 1 FROM organization_role WHERE organization_id=? AND role=?)
                        """, Boolean.class, id,
                    roles.get(field)));
                if (!Boolean.TRUE.equals(active.get(id)) || !validRole) {
                    errors.add(new ProjectProblem.FieldError(field, "invalid_reference", "Company must exist, be active and have the required party role."));
                }
            }
        });
        return errors;
    }
}

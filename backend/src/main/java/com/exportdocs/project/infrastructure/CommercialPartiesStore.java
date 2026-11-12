package com.exportdocs.project.infrastructure;

import com.exportdocs.party.infrastructure.OrganizationSelectionStore;
import com.exportdocs.project.application.CommercialPartiesWrite;
import com.exportdocs.project.application.ProjectProblem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CommercialPartiesStore {
    private static final Map<String, String> ROLES = Map.of("supplierId", "SUPPLIER",
        "clientId", "CLIENT", "importerId", "IMPORTER", "consigneeId",
        "CONSIGNEE");
    private final JdbcTemplate jdbc;
    private final OrganizationSelectionStore selections;

    public CommercialPartiesStore(JdbcTemplate jdbc, OrganizationSelectionStore selections) {
        this.jdbc = jdbc;
        this.selections = selections;
    }

    public Map<String, UUID> changes(CommercialPartiesWrite request, ProjectDetailsStore.Details current) {
        Map<String, UUID> changes = new HashMap<>();
        for (var field : request.supplied()) {
            UUID requested = request.fields().values().get(field);
            if (!Objects.equals(requested, current.one(ROLES.get(field))))changes.put(field, requested);
        }
        return changes;
    }

    public void validate(Map<String, UUID> changes) {
        Map<String, List<UUID>> requested = new HashMap<>();
        changes.forEach((field, id) -> requested.put(field, id == null ? List.of() : List.of(id)));
        var errors = selections.validate(requested, ROLES);
        if (!errors.isEmpty()) throw new ProjectProblem(422, "Invalid shipment commercial parties.", errors);
    }

    public void replace(UUID project, Map<String, UUID> changes) {
        changes.forEach((field, id) -> {
            jdbc.update("DELETE FROM project_party WHERE project_id=? AND role=?", project, ROLES.get(field));
            if (id != null)jdbc.update("INSERT INTO project_party(project_id,organization_id,role) VALUES (?,?,?)", project, id, ROLES.get(field));
        });
    }
}

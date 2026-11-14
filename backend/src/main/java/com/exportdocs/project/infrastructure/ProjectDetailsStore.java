package com.exportdocs.project.infrastructure;

import com.exportdocs.party.infrastructure.OrganizationSelectionStore;
import com.exportdocs.project.application.ProjectActor;
import com.exportdocs.project.application.ProjectFields;
import com.exportdocs.project.application.ProjectProblem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectDetailsStore {
    private final JdbcTemplate jdbc;
    private final OrganizationSelectionStore selections;
    private static final Map<String, String> PARTY_FIELDS = Map.of(
            "exporterId", "EXPORTER", "producerIds", "PRODUCER", "consigneeId", "CONSIGNEE",
            "notifyPartyId", "NOTIFY_PARTY", "freightForwarderId", "FREIGHT_FORWARDER", "shippingLineId", "SHIPPING_LINE");

    public ProjectDetailsStore(JdbcTemplate jdbc, OrganizationSelectionStore selections) {
        this.jdbc = jdbc;
        this.selections = selections;
    }

    public void auditContext(UUID actor) {
        jdbc.queryForObject("SELECT set_config('app.actor_id', ?, true)", String.class, actor.toString());
        jdbc.queryForObject("SELECT set_config('app.request_id', ?, true)", String.class, UUID.randomUUID().toString());
    }

    public boolean visible(UUID project, ProjectActor actor) {
        return actor.admin() || Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM project_access WHERE project_id=? AND user_id=?)",
                Boolean.class, project, actor.id()));
    }

    public boolean canEditSetup(UUID project,ProjectActor actor) {
        return canEditForm(project, actor, "PROJECT_SETUP");
    }

    public boolean canEditForm(UUID project, ProjectActor actor, String form) {
        return actor.admin() || Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM project_form_assignment
                WHERE project_id=? AND user_id=? AND form_key=?)
                """,Boolean.class,project,actor.id(),form));
    }

    public void validate(ProjectFields fields, Set<String> supplied) {
        Map<String, List<UUID>> requested = new HashMap<>();
        partyValues(fields).forEach((field, ids) -> {
            if (supplied.contains(field)) requested.put(field, ids);
        });
        var errors = new ArrayList<>(selections.validate(requested, PARTY_FIELDS));
        Map<String, UUID> ports = new HashMap<>();
        ports.put("portOfLoadingId", fields.portOfLoadingId());
        ports.put("portOfDischargeId", fields.portOfDischargeId());
        ports.forEach((field, id) -> {
            if (supplied.contains(field) && id != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM port WHERE id=? AND active)", Boolean.class, id))) {
                errors.add(new ProjectProblem.FieldError(field, "invalid_reference", "Port must exist and be active."));
            }
        });
        if (!errors.isEmpty()) throw new ProjectProblem(422, "Invalid shipment reference data.", errors);
    }

    public Details load(UUID project) {
        Map<String, List<UUID>> parties = new HashMap<>();
        jdbc.query("SELECT role, organization_id FROM project_party WHERE project_id=? ORDER BY organization_id",
                rs -> { parties.computeIfAbsent(rs.getString("role"), key -> new ArrayList<>())
                        .add(rs.getObject("organization_id", UUID.class)); }, project);
        return jdbc.queryForObject("SELECT port_of_loading_id, port_of_discharge_id FROM shipment_transport WHERE project_id=?",
                (rs, row) -> new Details(parties, rs.getObject("port_of_loading_id", UUID.class),
                        rs.getObject("port_of_discharge_id", UUID.class)), project);
    }

    public void create(UUID project, UUID creator, ProjectFields fields) {
        jdbc.update("INSERT INTO project_access(project_id,user_id) VALUES (?,?)", project, creator);
        jdbc.update("INSERT INTO shipment_transport(project_id,port_of_loading_id,port_of_discharge_id) VALUES (?,?,?)",
                project, fields.portOfLoadingId(), fields.portOfDischargeId());
        partyValues(fields).forEach((field, ids) -> ids.forEach(id ->
                jdbc.update("INSERT INTO project_party(project_id,organization_id,role) VALUES (?,?,?)", project, id, PARTY_FIELDS.get(field))));
    }

    public boolean change(UUID project, ProjectFields fields, Set<String> supplied, Details current) {
        boolean changed = false;
        for (var entry : partyValues(fields).entrySet()) {
            String role = PARTY_FIELDS.get(entry.getKey());
            if (!supplied.contains(entry.getKey())) continue;
            Set<UUID> previous = new HashSet<>(current.parties().getOrDefault(role, List.of()));
            Set<UUID> next = new HashSet<>(entry.getValue());
            for (UUID id : previous) {
                if (!next.contains(id)) {
                    jdbc.update("DELETE FROM project_party WHERE project_id=? AND role=? AND organization_id=?", project, role, id);
                    changed = true;
                }
            }
            for (UUID id : next) {
                if (!previous.contains(id)) {
                    jdbc.update("INSERT INTO project_party(project_id,organization_id,role) VALUES (?,?,?)", project, id, role);
                    changed = true;
                }
            }
        }
        UUID loading = supplied.contains("portOfLoadingId") ? fields.portOfLoadingId() : current.loading();
        UUID discharge = supplied.contains("portOfDischargeId") ? fields.portOfDischargeId() : current.discharge();
        if (!Objects.equals(loading, current.loading()) || !Objects.equals(discharge, current.discharge())) {
            jdbc.update("UPDATE shipment_transport SET port_of_loading_id=?,port_of_discharge_id=? WHERE project_id=?", loading, discharge, project);
            changed = true;
        }
        return changed;
    }

    private Map<String, List<UUID>> partyValues(ProjectFields fields) {
        Map<String, List<UUID>> values = new HashMap<>();
        values.put("exporterId", singleton(fields.exporterId()));
        values.put("producerIds", fields.producerIds() == null ? List.of() : fields.producerIds());
        values.put("consigneeId", singleton(fields.consigneeId()));
        values.put("notifyPartyId", singleton(fields.notifyPartyId()));
        values.put("freightForwarderId", singleton(fields.freightForwarderId()));
        values.put("shippingLineId", singleton(fields.shippingLineId()));
        return values;
    }

    private List<UUID> singleton(UUID id) { return id == null ? List.of() : List.of(id); }

    public record Details(Map<String, List<UUID>> parties, UUID loading, UUID discharge) {
        public UUID one(String role) {
            var ids = parties.getOrDefault(role, List.of());
            return ids.isEmpty() ? null : ids.getFirst();
        }
        public List<UUID> producers() { return parties.getOrDefault("PRODUCER", List.of()); }
    }
}

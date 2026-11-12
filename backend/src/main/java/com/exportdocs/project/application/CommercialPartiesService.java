package com.exportdocs.project.application;

import com.exportdocs.project.api.CommercialPartiesMapper;
import com.exportdocs.project.api.CommercialPartiesResponse;
import com.exportdocs.project.infrastructure.CommercialPartiesStore;
import com.exportdocs.project.infrastructure.ProjectActorProvider;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
import com.exportdocs.project.infrastructure.ProjectRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialPartiesService {
    private final ProjectRepository projects;
    private final ProjectDetailsStore details;
    private final CommercialPartiesStore parties;
    private final ProjectActorProvider actors;
    private final CommercialPartiesMapper mapper;

    public CommercialPartiesService(ProjectRepository projects, ProjectDetailsStore details,
        CommercialPartiesStore parties, ProjectActorProvider actors, CommercialPartiesMapper mapper) {
        this.projects = projects;
        this.details = details;
        this.parties = parties;
        this.actors = actors;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true,
        isolation = Isolation.REPEATABLE_READ)
    public CommercialPartiesResponse get(UUID id) {
        var actor = read();
        if (!details.visible(id, actor)) throw ProjectProblem.notFound();
        var project = projects.findById(id).orElseThrow(ProjectProblem::notFound);
        return mapper.response(project, details.load(id));
    }

    @Transactional
    public CommercialPartiesResponse patch(UUID id, CommercialPartiesWrite request) {
        var actor = read();
        if (!details.visible(id, actor)) throw ProjectProblem.notFound();
        var project = projects.findForUpdate(id).orElseThrow(ProjectProblem::notFound);
        // Re-read identity, membership and grant after waiting for the aggregate lock.
        actor = read();
        if (!details.visible(id, actor)) throw ProjectProblem.notFound();
        if (!actor.canWrite() || !details.canEditSetup(id, actor)) throw new ProjectProblem(403, "Project setup edit assignment required.",
            List.of());
        if (project.getVersion() != request.expectedVersion()) throw ProjectProblem.field(409, "expectedVersion",
            "stale_version", "Project changed; reload before updating.");
        var current = details.load(id);
        var changes = parties.changes(request, current);
        if (changes.isEmpty()) return mapper.response(project, current);
        parties.validate(changes);
        details.auditContext(actor.id());
        parties.replace(id, changes);
        project.touch();
        projects.flush();
        return mapper.response(project, details.load(id));
    }

    private ProjectActor read() {
        var actor = actors.current();
        if (!actor.canRead()) throw new ProjectProblem(403, "Insufficient project permission.", List.of());
        return actor;
    }
}

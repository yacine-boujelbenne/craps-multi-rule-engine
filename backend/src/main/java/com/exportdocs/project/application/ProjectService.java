package com.exportdocs.project.application;

import com.exportdocs.project.api.ProjectMapper;
import com.exportdocs.project.api.ProjectPage;
import com.exportdocs.project.api.ProjectResponse;
import com.exportdocs.project.domain.Project;
import com.exportdocs.project.domain.ProjectStatus;
import com.exportdocs.project.infrastructure.ProjectActorProvider;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
import com.exportdocs.project.infrastructure.ProjectRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
public class ProjectService {
    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    private final ProjectRepository projects;
    private final ProjectDetailsStore details;
    private final ProjectActorProvider actors;
    private final ProjectMapper mapper;

    public ProjectService(ProjectRepository projects, ProjectDetailsStore details, ProjectActorProvider actors, ProjectMapper mapper) {
        this.projects = projects;
        this.details = details;
        this.actors = actors;
        this.mapper = mapper;
    }

    @Transactional
    public ProjectResponse create(ProjectWrite request) {
        ProjectActor actor = authorize(true);
        if(!actor.admin()) throw new ProjectProblem(403,"Only administrators can create projects.",List.of());
        validate(request);
        details.auditContext(actor.id());
        var fields = request.fields();
        var project = projects.saveAndFlush(Project.draft(fields.reference(), fields.destinationCountryCode(),
                fields.expectedDepartureDate(), fields.notes(), actor.id()));
        details.create(project.getId(), actor.id(), request.fields());
        return response(project);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProjectResponse get(UUID id) {
        ProjectActor actor = authorize(false);
        if (!details.visible(id, actor)) throw ProjectProblem.notFound();
        return response(projects.findById(id).orElseThrow(ProjectProblem::notFound));
    }

    @Transactional
    public ProjectResponse patch(UUID id, ProjectWrite request) {
        ProjectActor actor = authorize(true);
        if (!details.visible(id, actor)) throw ProjectProblem.notFound();
        // Serialize child-row replacements before checking the client's version.
        var project = projects.findForUpdate(id).orElseThrow(ProjectProblem::notFound);
        if(!details.canEditSetup(id,actor)) throw new ProjectProblem(403,"Project setup edit assignment required.",List.of());
        if (!project.getVersion().equals(request.expectedVersion())) {
            throw ProjectProblem.field(409, "expectedVersion", "stale_version", "Project changed; reload it before updating.");
        }
        validate(request);
        var current = details.load(id);
        details.auditContext(actor.id());
        var fields = request.fields();
        var supplied = request.supplied();
        boolean changed = project.replaceSetup(
                supplied.contains("reference") ? fields.reference() : project.getReference(),
                supplied.contains("destinationCountryCode") ? fields.destinationCountryCode() : project.getDestinationCountryCode(),
                supplied.contains("expectedDepartureDate") ? fields.expectedDepartureDate() : project.getExpectedDepartureDate(),
                supplied.contains("notes") ? fields.notes() : project.getNotes());
        changed = details.change(id, request.fields(), request.supplied(), current) || changed;
        if (changed) {
            project.touch();
            projects.flush();
        }
        return response(project);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ProjectPage list(int page, int size, ProjectStatus status, String query) {
        ProjectActor actor = authorize(false);
        if (page < 0) throw ProjectProblem.field(400, "page", "invalid", "Page must be nonnegative.");
        if (size < 1 || size > 100) throw ProjectProblem.field(400, "size", "invalid", "Size must be between 1 and 100.");
        // JPA setFirstResult takes an int even though Spring Pageable exposes a long offset.
        if ((long) page * size > Integer.MAX_VALUE) throw ProjectProblem.field(400, "page", "invalid", "Page offset is too large.");
        String prefix = null;
        if (query != null) {
            String trimmed = query.trim();
            if (trimmed.isEmpty() || trimmed.length() > 80) {
                throw ProjectProblem.field(400, "q", "invalid", "Reference prefix must contain 1 to 80 characters.");
            }
            prefix = trimmed.toUpperCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        }
        var result = projects.findVisible(actor.id(), actor.admin(), status == null ? null : status.name(), prefix, PageRequest.of(page, size));
        return new ProjectPage(result.stream().map(this::response).toList(), page, size, result.getTotalElements(), result.getTotalPages());
    }

    private void validate(ProjectWrite request) {
        String country = request.fields().destinationCountryCode();
        if (country != null && !COUNTRIES.contains(country)) {
            throw ProjectProblem.field(400, "destinationCountryCode", "invalid", "Use an ISO 3166-1 alpha-2 country code.");
        }
        details.validate(request.fields(), request.supplied());
    }

    private ProjectActor authorize(boolean write) {
        ProjectActor actor = actors.current();
        if ((write && !actor.canWrite()) || (!write && !actor.canRead())) {
            throw new ProjectProblem(403, "Insufficient permission for this operation.", List.of());
        }
        return actor;
    }

    private ProjectResponse response(Project project) { return mapper.response(project, details.load(project.getId())); }
}

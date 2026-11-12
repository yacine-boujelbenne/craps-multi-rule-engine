package com.exportdocs.party.application;

import com.exportdocs.party.api.OrganizationDtos;
import com.exportdocs.party.api.OrganizationMapper;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.party.domain.Organization;
import com.exportdocs.party.infrastructure.OrganizationStore;
import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.infrastructure.ProjectActorProvider;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    private final OrganizationStore store;
    private final ProjectActorProvider actors;
    private final ProjectDetailsStore audit;
    private final OrganizationMapper mapper;

    public OrganizationService(OrganizationStore store, ProjectActorProvider actors, ProjectDetailsStore audit,
        OrganizationMapper mapper) {
        this.store = store;
        this.actors = actors;
        this.audit = audit;
        this.mapper = mapper;
    }

    @Transactional
    public OrganizationDtos.Response create(OrganizationWrite request) {
        var actor = actors.current();
        if (!actor.admin()) throw forbidden();
        validate(request.fields());
        audit.auditContext(actor.id());
        return mapper.response(store.create(UUID.randomUUID(), request.fields()));
    }

    @Transactional
    public OrganizationDtos.Response patch(UUID id, OrganizationWrite request) {
        var actor = actors.current();
        if (!actor.admin()) throw forbidden();
        var current = store.find(id, true);
        if (current.version() != request.expectedVersion()) throw ProjectProblem.field(409, "expectedVersion",
            "stale_version", "Organization changed; reload before updating.");
        validate(request.fields());
        var fields = request.fields();
        var supplied = request.supplied();
        var next = new Organization(id, current.version(), supplied.contains("legalName") ? fields.legalName() : current.legalName(),
            supplied.contains("countryCode") ? fields.countryCode() : current.countryCode(),
            supplied.contains("addressText") ? fields.addressText() : current.addressText(),
            supplied.contains("registrationNumber") ? fields.registrationNumber() : current.registrationNumber(),
            supplied.contains("roles") ? fields.roles() : current.roles(),
            supplied.contains("active") ? fields.active() : current.active(), current.createdAt(), current.updatedAt());
        if (Objects.equals(next, current)) return mapper.response(current);
        audit.auditContext(actor.id());
        return mapper.response(store.update(next, current));
    }

    @Transactional(readOnly = true,
        isolation = Isolation.REPEATABLE_READ)
    public OrganizationDtos.Response get(UUID id) {
        read();
        return mapper.response(store.find(id, false));
    }

    @Transactional(readOnly = true,
        isolation = Isolation.REPEATABLE_READ)
    public OrganizationDtos.Page list(SetupListQuery query) {
        var actor = read();
        if (query.includeInactive() && !actor.admin()) throw forbidden();
        var page = store.list(query);
        return new OrganizationDtos.Page(page.items().stream().map(mapper::response).toList(), query.page(), query.size(),
            page.total(), (int)((page.total() + query.size()-1)/query.size()));
    }

    private com.exportdocs.project.application.ProjectActor read() {
        var actor = actors.current();
        if (!actor.canRead()) throw forbidden();
        return actor;
    }

    private void validate(OrganizationWrite.Fields fields) {
        if (fields.countryCode() != null && !COUNTRIES.contains(fields.countryCode())) throw ProjectProblem.field(400,
            "countryCode", "invalid", "Use an ISO 3166-1 alpha-2 country code.");
    }

    private ProjectProblem forbidden() {
        return new ProjectProblem(403, "Insufficient directory permission.", List.of());
    }
}

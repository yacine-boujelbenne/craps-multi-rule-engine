package com.exportdocs.geography.application;

import com.exportdocs.geography.api.GeographyDtos;
import com.exportdocs.geography.api.GeographyMapper;
import com.exportdocs.geography.infrastructure.GeographyStore;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.infrastructure.ProjectActorProvider;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeographyService {
    private final GeographyStore store;
    private final ProjectActorProvider actors;
    private final GeographyMapper mapper;
    private final com.exportdocs.project.infrastructure.ProjectDetailsStore audit;

    public GeographyService(GeographyStore store, ProjectActorProvider actors, GeographyMapper mapper, com.exportdocs.project.infrastructure.ProjectDetailsStore audit) {
        this.audit = audit;
        this.store = store;
        this.actors = actors;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<GeographyDtos.CountryResponse> countries() {
        read();
        return store.countries().stream().map(mapper::country).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public GeographyDtos.CityPage cities(String countryCode, SetupListQuery query) {
        read();
        if (!store.activeCountry(countryCode)) throw ProjectProblem.field(404, "countryCode", "not_found",
            "Active country not found.");
        var result = store.cities(countryCode, query);
        return new GeographyDtos.CityPage(result.items().stream().map(mapper::city).toList(),
            query.page(), query.size(), result.total(), (int)((result.total() + query.size() - 1) / query.size()));
    }

    @Transactional(readOnly = true)
    public List<GeographyDtos.PortResponse> ports() {
        read();
        return store.ports();
    }

    @Transactional
    public GeographyDtos.PortResponse createPort(GeographyDtos.PortWrite input) {
        var actor=actors.current();
        if (!actor.canWrite()) throw new ProjectProblem(403,"Write permission required.",List.of());
        if (!store.activeCountry(input.countryCode())) throw ProjectProblem.field(400,"countryCode","invalid","Choose an active country.");
        audit.auditContext(actor.id());
        return store.createPort(input);
    }

    private void read() {
        if (!actors.current().canRead()) throw new ProjectProblem(403, "Insufficient geography permission.", List.of());
    }
}

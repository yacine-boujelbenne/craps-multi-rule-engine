package com.exportdocs.party.application;

import com.exportdocs.party.api.OrganizationDtos;
import com.exportdocs.party.api.OrganizationMapper;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.party.domain.Organization;
import com.exportdocs.party.domain.OrganizationAddress;
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
        if (!actor.canWrite()) throw forbidden();
        validate(request.fields());
        audit.auditContext(actor.id());
        return mapper.response(store.create(UUID.randomUUID(), request.fields()));
    }

    @Transactional
    public OrganizationDtos.Response patch(UUID id, OrganizationWrite request) {
        var actor = actors.current();
        if (!actor.canWrite()) throw forbidden();
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
            supplied.contains("tradeName") ? fields.tradeName() : current.tradeName(),
            supplied.contains("taxIdentifier") ? fields.taxIdentifier() : current.taxIdentifier(),
            supplied.contains("email") ? fields.email() : current.email(),
            supplied.contains("phone") ? fields.phone() : current.phone(),
            supplied.contains("roles") ? fields.roles() : current.roles(),
            supplied.contains("active") ? fields.active() : current.active(), current.createdAt(), current.updatedAt(),
            current.addresses());
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

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<OrganizationDtos.AddressResponse> addresses(UUID organization) {
        read();
        return store.find(organization, false).addresses().stream().map(mapper::address).toList();
    }

    @Transactional
    public OrganizationDtos.AddressResponse createAddress(UUID organization, AddressWrite request) {
        var actor = actors.current();
        if (!actor.canWrite()) throw forbidden();
        var parent = store.find(organization, true);
        if (request.expectedOrganizationVersion() != null && parent.version() != request.expectedOrganizationVersion())
            throw ProjectProblem.field(409, "expectedOrganizationVersion", "stale_version",
                "Organization changed; reload before creating the address.");
        var fields = request.fields();
        var next = new OrganizationAddress(UUID.randomUUID(), organization, fields.kind(), fields.countryCode(),
            fields.cityId(), fields.manualCity(), fields.addressLine1(), fields.addressLine2(), fields.postalCode(), true, 0);
        validateCitySelection(next);
        store.validateAddressLocation(next);
        audit.auditContext(actor.id());
        return mapper.address(store.createAddress(next));
    }

    @Transactional
    public OrganizationDtos.AddressResponse patchAddress(UUID organization, UUID id, AddressWrite request) {
        var actor = actors.current();
        if (!actor.canWrite()) throw forbidden();
        store.find(organization, true);
        var current = store.address(organization, id, true);
        if (current.version() != request.expectedVersion()) throw ProjectProblem.field(409, "expectedVersion",
            "stale_version", "Address changed; reload before updating.");
        var fields = request.fields();
        var supplied = request.supplied();
        var next = new OrganizationAddress(id, organization,
            supplied.contains("kind") ? fields.kind() : current.kind(),
            supplied.contains("countryCode") ? fields.countryCode() : current.countryCode(),
            supplied.contains("cityId") ? fields.cityId() : current.cityId(),
            supplied.contains("manualCity") ? fields.manualCity() : current.manualCity(),
            supplied.contains("addressLine1") ? fields.addressLine1() : current.addressLine1(),
            supplied.contains("addressLine2") ? fields.addressLine2() : current.addressLine2(),
            supplied.contains("postalCode") ? fields.postalCode() : current.postalCode(),
            supplied.contains("active") ? fields.active() : current.active(), current.version());
        validateCitySelection(next);
        if (Objects.equals(next, current)) return mapper.address(current);
        if (!Objects.equals(next.countryCode(), current.countryCode()) || !Objects.equals(next.cityId(), current.cityId())
            || !Objects.equals(next.manualCity(), current.manualCity())) store.validateAddressLocation(next);
        audit.auditContext(actor.id());
        return mapper.address(store.updateAddress(next));
    }

    private void validateCitySelection(OrganizationAddress address) {
        if ((address.cityId() == null) == (address.manualCity() == null)) throw ProjectProblem.field(400,
            "manualCity", "invalid", "Choose exactly one of cityId or nonblank manualCity.");
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

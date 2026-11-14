package com.exportdocs.catalog.application;

import com.exportdocs.catalog.api.CatalogDtos;
import com.exportdocs.catalog.api.CatalogMapper;
import com.exportdocs.catalog.domain.Product;
import com.exportdocs.catalog.domain.Grade;
import com.exportdocs.catalog.domain.TaxonomyCategory;
import com.exportdocs.catalog.infrastructure.CatalogStore;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.project.application.ProjectActor;
import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.infrastructure.ProjectActorProvider;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final CatalogStore store;
    private final ProjectActorProvider actors;
    private final ProjectDetailsStore audit;
    private final CatalogMapper mapper;

    public CatalogService(CatalogStore store, ProjectActorProvider actors, ProjectDetailsStore audit,
        CatalogMapper mapper) {
        this.store = store;
        this.actors = actors;
        this.audit = audit;
        this.mapper = mapper;
    }

    @Transactional
    public CatalogDtos.ProductResponse createProduct(ProductWrite request) {
        var actor = write();
        audit.auditContext(actor.id());
        return mapper.product(store.createProduct(UUID.randomUUID(), request.fields()));
    }

    @Transactional
    public CatalogDtos.GradeResponse createGrade(UUID product, GradeWrite request) {
        var actor = admin();
        store.product(product, false);
        validateDefaults(request.fields().defaultPricePerKg(), request.fields().defaultCurrency());
        audit.auditContext(actor.id());
        return mapper.grade(store.createGrade(product, UUID.randomUUID(), request.fields()));
    }

    @Transactional
    public CatalogDtos.ProductResponse patchProduct(UUID id, ProductWrite request) {
        var actor = write();
        var current = store.product(id, true);
        version(current.version(), request.expectedVersion());
        var fields = request.fields();
        var supplied = request.supplied();
        var next = new Product(id, current.version(), supplied.contains("code") ? fields.code() : current.code(),
            supplied.contains("name") ? fields.name() : current.name(),
            supplied.contains("scientificName") ? fields.scientificName() : current.scientificName(),
            supplied.contains("hsCode") ? fields.hsCode() : current.hsCode(),
            supplied.contains("active") ? fields.active() : current.active(),
            supplied.contains("description") ? fields.description() : current.description(),
            supplied.contains("processingMethod") ? fields.processingMethod() : current.processingMethod(),
            supplied.contains("storageRequirements") ? fields.storageRequirements() : current.storageRequirements(),
            supplied.contains("defaultMeasurementUnit") ? fields.defaultMeasurementUnit() : current.defaultMeasurementUnit(),
            current.categories(), current.grades());
        if (Objects.equals(next, current)) return mapper.product(current);
        audit.auditContext(actor.id());
        return mapper.product(store.updateProduct(next));
    }

    @Transactional
    public CatalogDtos.GradeResponse patchGrade(UUID product, UUID id, GradeWrite request) {
        var actor = admin();
        var current = store.grade(product, id, true);
        version(current.version(), request.expectedVersion());
        var fields = request.fields();
        var supplied = request.supplied();
        var next = new Grade(id, product, current.version(), supplied.contains("code") ? fields.code() : current.code(),
            supplied.contains("name") ? fields.name() : current.name(),
            supplied.contains("displayOrder") ? fields.displayOrder() : current.displayOrder(),
            supplied.contains("defaultCartonNetKg") ? fields.defaultCartonNetKg() : current.defaultCartonNetKg(),
            supplied.contains("defaultPricePerKg") ? fields.defaultPricePerKg() : current.defaultPricePerKg(),
            supplied.contains("defaultCurrency") ? fields.defaultCurrency() : current.defaultCurrency(),
            supplied.contains("active") ? fields.active() : current.active());
        validateDefaults(next.defaultPricePerKg(), next.defaultCurrency());
        if (Objects.equals(next, current)) return mapper.grade(current);
        audit.auditContext(actor.id());
        return mapper.grade(store.updateGrade(next));
    }

    @Transactional(readOnly = true,
        isolation = Isolation.REPEATABLE_READ)
    public CatalogDtos.ProductResponse getProduct(UUID id) {
        read();
        return mapper.product(store.product(id, false));
    }

    @Transactional(readOnly = true,
        isolation = Isolation.REPEATABLE_READ)
    public CatalogDtos.GradeResponse getGrade(UUID product, UUID id) {
        read();
        return mapper.grade(store.grade(product, id, false));
    }

    @Transactional(readOnly = true,
        isolation = Isolation.REPEATABLE_READ)
    public CatalogDtos.ProductPage products(UUID category, SetupListQuery query) {
        listAccess(query);
        var result = store.products(category, query);
        return new CatalogDtos.ProductPage(result.items().stream().map(mapper::product).toList(), query.page(), query.size(),
            result.total(), pages(result.total(), query.size()));
    }

    @Transactional(readOnly = true,
        isolation = Isolation.REPEATABLE_READ)
    public CatalogDtos.GradePage grades(UUID product, SetupListQuery query) {
        listAccess(query);
        store.product(product, false);
        var result = store.grades(product, query);
        return new CatalogDtos.GradePage(result.items().stream().map(mapper::grade).toList(), query.page(), query.size(),
            result.total(), pages(result.total(), query.size()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CatalogDtos.TaxonomyPage taxonomy(SetupListQuery query) {
        listAccess(query);
        var page = store.taxonomy(query);
        return new CatalogDtos.TaxonomyPage(page.items().stream().map(mapper::taxonomy).toList(), query.page(), query.size(),
            page.total(), pages(page.total(), query.size()));
    }

    @Transactional
    public CatalogDtos.TaxonomyResponse createTaxonomy(TaxonomyWrite request) {
        var actor = write();
        audit.auditContext(actor.id());
        return mapper.taxonomy(store.createTaxonomy(UUID.randomUUID(), request.fields()));
    }

    @Transactional
    public CatalogDtos.TaxonomyResponse patchTaxonomy(UUID id, TaxonomyWrite request) {
        var actor = write();
        var current = store.taxonomy(id, true);
        version(current.version(), request.expectedVersion());
        var fields = request.fields();
        var supplied = request.supplied();
        var next = new TaxonomyCategory(id, current.version(), supplied.contains("code") ? fields.code() : current.code(),
            supplied.contains("name") ? fields.name() : current.name(),
            supplied.contains("description") ? fields.description() : current.description(),
            supplied.contains("active") ? fields.active() : current.active());
        if (next.equals(current)) return mapper.taxonomy(current);
        audit.auditContext(actor.id());
        return mapper.taxonomy(store.updateTaxonomy(next));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<CatalogDtos.TaxonomyResponse> assignedCategories(UUID id) {
        read();
        return store.product(id, false).categories().stream().map(mapper::taxonomy).toList();
    }

    @Transactional
    public List<CatalogDtos.TaxonomyResponse> replaceCategories(UUID id, TaxonomyWrite.Assignment request) {
        var actor = write();
        var product = store.product(id, true);
        if (product.version() != request.expectedProductVersion()) throw ProjectProblem.field(409, "expectedProductVersion",
            "stale_version", "Product changed; reload before replacing taxonomy assignments.");
        var current = product.categories().stream().map(TaxonomyCategory::id).collect(Collectors.toSet());
        store.validateCategories(request.categoryIds(), current);
        if (!current.equals(request.categoryIds())) {
            audit.auditContext(actor.id());
            store.replaceCategories(id, current, request.categoryIds());
        }
        return store.assignedCategories(id).stream().map(mapper::taxonomy).toList();
    }

    private int pages(long total, int size) {
        return (int)((total + size - 1)/size);
    }

    private void version(long actual, long expected) {
        if (actual != expected) throw ProjectProblem.field(409, "expectedVersion", "stale_version",
            "Catalog record changed; reload before updating.");
    }

    private void validateDefaults(BigDecimal price, String currency) {
        if ((price == null) != (currency == null)) throw ProjectProblem.field(400, "defaultCurrency",
            "invalid", "Price and currency must both be set or both null.");
        if (currency != null) try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException exception) {
            throw ProjectProblem.field(400, "defaultCurrency", "invalid", "Use an ISO 4217 currency code.");
        }
    }

    private ProjectActor admin() {
        var actor = actors.current();
        if (!actor.admin()) throw forbidden();
        return actor;
    }

    private ProjectActor write() {
        var actor = actors.current();
        if (!actor.canWrite()) throw forbidden();
        return actor;
    }

    private ProjectActor read() {
        var actor = actors.current();
        if (!actor.canRead()) throw forbidden();
        return actor;
    }

    private void listAccess(SetupListQuery query) {
        var actor = read();
        if (query.includeInactive() && !actor.admin()) throw forbidden();
    }

    private ProjectProblem forbidden() {
        return new ProjectProblem(403, "Insufficient catalog permission.", List.of());
    }
}

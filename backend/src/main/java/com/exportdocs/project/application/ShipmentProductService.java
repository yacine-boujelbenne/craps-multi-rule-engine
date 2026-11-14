package com.exportdocs.project.application;

import com.exportdocs.catalog.domain.Grade;
import com.exportdocs.project.api.ShipmentProductMapper;
import com.exportdocs.project.api.ShipmentProductResponse;
import com.exportdocs.project.domain.Project;
import com.exportdocs.project.domain.ShipmentProduct;
import com.exportdocs.project.infrastructure.ProjectActorProvider;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
import com.exportdocs.project.infrastructure.ProjectRepository;
import com.exportdocs.project.infrastructure.ShipmentProductStore;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentProductService {
    private final ProjectRepository projects;
    private final ProjectDetailsStore details;
    private final ShipmentProductStore products;
    private final ProjectActorProvider actors;
    private final ShipmentProductMapper mapper;

    public ShipmentProductService(ProjectRepository projects, ProjectDetailsStore details, ShipmentProductStore products,
        ProjectActorProvider actors, ShipmentProductMapper mapper) {
        this.projects = projects;
        this.details = details;
        this.products = products;
        this.actors = actors;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<ShipmentProductResponse> list(UUID id) {
        var actor = read();
        if (!details.visible(id, actor)) throw ProjectProblem.notFound();
        var project = projects.findById(id).orElseThrow(ProjectProblem::notFound);
        return products.list(id).stream().map(p -> mapper.response(p, project.getVersion())).toList();
    }

    @Transactional
    public ShipmentProductResponse create(UUID id, ShipmentProductWrite request) {
        var locked = lockAndAuthorize(id);
        var actor = locked.actor();
        Project project = locked.project();
        checkProjectVersion(project, request.expectedProjectVersion());
        rejectOperatorPrices(actor, request);
        validatePair(request);
        products.requireProduct(request.productId());
        Grade grade = request.productCategoryId() == null ? null : products.activeGrade(request.productId(), request.productCategoryId());
        var price = request.supplied().contains("unitPrice") ? request.unitPrice() : grade == null ? null : grade.defaultPricePerKg();
        var currency = request.supplied().contains("currency") ? request.currency() : grade == null ? null : grade.defaultCurrency();
        var row = new ShipmentProduct(UUID.randomUUID(), 0, request.productId(), request.productCategoryId(),
            request.cartonCount(), request.quantity(), request.quantityUnit(), request.netWeightKg(), request.grossWeightKg(),
            price, currency,
            request.lotReference(), request.processingInformation(), grade == null ? null : grade.version(),
            grade == null ? null : grade.code(), grade == null ? null : grade.name(), grade == null ? null : grade.defaultCartonNetKg());
        validateValues(row);
        details.auditContext(actor.id());
        var saved = products.create(id, row);
        project.touch();
        projects.flush();
        return mapper.response(saved, project.getVersion());
    }

    @Transactional
    public ShipmentProductResponse patch(UUID id, UUID productId, ShipmentProductWrite request) {
        var locked = lockAndAuthorize(id);
        var actor = locked.actor();
        Project project = locked.project();
        checkProjectVersion(project, request.expectedProjectVersion());
        ShipmentProduct current = products.find(id, productId, true);
        if (current.version() != request.expectedVersion()) throw ProjectProblem.field(409, "expectedVersion", "stale_version",
            "Shipment product changed; reload before updating.");
        rejectOperatorPrices(actor, request);
        validatePair(request);
        var supplied = request.supplied();
        UUID gradeId = supplied.contains("productCategoryId") ? request.productCategoryId() : current.productCategoryId();
        boolean gradeChanged = !Objects.equals(gradeId, current.productCategoryId());
        Grade grade = gradeChanged && gradeId != null ? products.activeGrade(current.productId(), gradeId) : null;
        var price = gradeChanged ? grade == null ? null : grade.defaultPricePerKg() : current.unitPrice();
        var currency = gradeChanged ? grade == null ? null : grade.defaultCurrency() : current.currency();
        var appliedVersion = gradeChanged ? grade == null ? null : grade.version() : current.appliedGradeVersion();
        var appliedCode = gradeChanged ? grade == null ? null : grade.code() : current.appliedGradeCode();
        var appliedName = gradeChanged ? grade == null ? null : grade.name() : current.appliedGradeName();
        var appliedCartonNet = gradeChanged ? grade == null ? null : grade.defaultCartonNetKg() : current.appliedCartonNetKg();
        if (supplied.contains("unitPrice")) price = request.unitPrice();
        if (supplied.contains("currency")) currency = request.currency();
        var next = new ShipmentProduct(current.id(), current.version(), current.productId(), gradeId,
            supplied.contains("cartonCount") ? request.cartonCount() : current.cartonCount(),
            supplied.contains("quantity") ? request.quantity() : current.quantity(),
            supplied.contains("quantityUnit") ? request.quantityUnit() : current.quantityUnit(),
            supplied.contains("netWeightKg") ? request.netWeightKg() : current.netWeightKg(),
            supplied.contains("grossWeightKg") ? request.grossWeightKg() : current.grossWeightKg(),
            price, currency,
            supplied.contains("lotReference") ? request.lotReference() : current.lotReference(),
            supplied.contains("processingInformation") ? request.processingInformation() : current.processingInformation(),
            appliedVersion, appliedCode, appliedName, appliedCartonNet);
        validateValues(next);
        if (sameValues(current, next)) return mapper.response(current, project.getVersion());
        details.auditContext(actor.id());
        var saved = products.update(id, next);
        project.touch();
        projects.flush();
        return mapper.response(saved, project.getVersion());
    }

    private Locked lockAndAuthorize(UUID id) {
        var actor = read();
        if (!details.visible(id, actor)) throw ProjectProblem.notFound();
        Project project = projects.findForUpdate(id).orElseThrow(ProjectProblem::notFound);
        actor = read();
        if (!details.visible(id, actor)) throw ProjectProblem.notFound();
        if (!actor.canWrite() || !details.canEditForm(id, actor, "PACKING_LIST"))
            throw new ProjectProblem(403, "Packing list edit assignment required.", List.of());
        return new Locked(project, actor);
    }

    private ProjectActor read() {
        var actor = actors.current();
        if (!actor.canRead()) throw new ProjectProblem(403, "Insufficient project permission.", List.of());
        return actor;
    }

    private void checkProjectVersion(Project project, long expected) {
        if (project.getVersion() != expected) throw ProjectProblem.field(409, "expectedProjectVersion", "stale_version",
            "Project changed; reload before updating shipment products.");
    }

    private void rejectOperatorPrices(ProjectActor actor, ShipmentProductWrite request) {
        if (!actor.admin() && (request.supplied().contains("unitPrice") || request.supplied().contains("currency")))
            throw new ProjectProblem(403, "Only administrators may set shipment prices.", List.of());
    }

    private void validatePair(ShipmentProductWrite request) {
        boolean price = request.supplied().contains("unitPrice");
        boolean currency = request.supplied().contains("currency");
        if (price != currency) throw ProjectProblem.field(400, price ? "currency" : "unitPrice", "required_pair",
            "Price and currency must be supplied together.");
    }

    private void validateValues(ShipmentProduct value) {
        if ((value.quantity() == null) != (value.quantityUnit() == null)) throw ProjectProblem.field(400,
            value.quantity() == null ? "quantity" : "quantityUnit", "required_pair", "Quantity and unit must be supplied together.");
        if (value.netWeightKg() != null && value.grossWeightKg() != null && value.grossWeightKg().compareTo(value.netWeightKg()) < 0)
            throw ProjectProblem.field(400, "grossWeightKg", "invalid", "Gross weight cannot be less than net weight.");
        if ((value.unitPrice() == null) != (value.currency() == null)) throw ProjectProblem.field(400,
            value.unitPrice() == null ? "unitPrice" : "currency", "required_pair", "Price and currency must both be null or populated.");
    }

    private boolean sameValues(ShipmentProduct a, ShipmentProduct b) {
        return Objects.equals(a.productCategoryId(), b.productCategoryId()) && Objects.equals(a.cartonCount(), b.cartonCount())
            && Objects.equals(a.quantity(), b.quantity()) && Objects.equals(a.quantityUnit(), b.quantityUnit())
            && Objects.equals(a.netWeightKg(), b.netWeightKg()) && Objects.equals(a.grossWeightKg(), b.grossWeightKg())
            && Objects.equals(a.unitPrice(), b.unitPrice()) && Objects.equals(a.currency(), b.currency())
            && Objects.equals(a.lotReference(), b.lotReference()) && Objects.equals(a.processingInformation(), b.processingInformation())
            && Objects.equals(a.appliedGradeVersion(), b.appliedGradeVersion()) && Objects.equals(a.appliedGradeCode(), b.appliedGradeCode())
            && Objects.equals(a.appliedGradeName(), b.appliedGradeName()) && Objects.equals(a.appliedCartonNetKg(), b.appliedCartonNetKg());
    }

    private record Locked(Project project, ProjectActor actor) {}
}

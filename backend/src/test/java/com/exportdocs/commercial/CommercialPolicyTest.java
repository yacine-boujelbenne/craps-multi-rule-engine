package com.exportdocs.commercial;

import com.exportdocs.catalog.api.CatalogMapper;
import com.exportdocs.catalog.application.CatalogService;
import com.exportdocs.catalog.application.GradeWrite;
import com.exportdocs.catalog.application.ProductWrite;
import com.exportdocs.catalog.application.TaxonomyWrite;
import com.exportdocs.catalog.domain.Grade;
import com.exportdocs.catalog.domain.Product;
import com.exportdocs.catalog.domain.TaxonomyCategory;
import com.exportdocs.catalog.infrastructure.CatalogStore;
import com.exportdocs.geography.api.GeographyMapper;
import com.exportdocs.geography.application.GeographyService;
import com.exportdocs.geography.domain.City;
import com.exportdocs.geography.domain.Country;
import com.exportdocs.geography.infrastructure.GeographyStore;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.party.api.OrganizationMapper;
import com.exportdocs.party.application.OrganizationService;
import com.exportdocs.party.application.OrganizationWrite;
import com.exportdocs.party.application.AddressWrite;
import com.exportdocs.party.domain.OrganizationRole;
import com.exportdocs.party.domain.Organization;
import com.exportdocs.party.domain.OrganizationAddress;
import com.exportdocs.party.infrastructure.OrganizationStore;
import com.exportdocs.project.application.ProjectActor;
import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.domain.Project;
import com.exportdocs.project.infrastructure.ProjectRepository;
import com.exportdocs.project.infrastructure.ProjectActorProvider;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommercialPolicyTest {
    final ProjectActorProvider actors = mock(ProjectActorProvider.class);
    final ProjectDetailsStore details = mock(ProjectDetailsStore.class);
    final OrganizationStore organizations = mock(OrganizationStore.class);
    final CatalogStore catalog = mock(CatalogStore.class);
    final OrganizationService organizationService = new OrganizationService(organizations, actors, details,
        Mappers.getMapper(OrganizationMapper.class));
    final CatalogService catalogService = new CatalogService(catalog, actors, details, Mappers.getMapper(CatalogMapper.class));
    final GeographyStore geography = mock(GeographyStore.class);
    final GeographyService geographyService = new GeographyService(geography, actors,
        Mappers.getMapper(GeographyMapper.class), details);
    final ProjectRepository projects = mock(ProjectRepository.class);
    @ParameterizedTest
    @ValueSource(strings = {
        "OPERATOR", "REVIEWER", "VIEWER"
    }
    )
    void readOnlyRolesCannotMutateMetadataAndFormGrantsCannotGrantGradeMutation(String role) {
        UUID actor = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(actors.current()).thenReturn(new ProjectActor(actor, Set.of(role)));
        // A project form grant cannot override the role required by the master-data API.
        when(details.canEditSetup(any(), any())).thenReturn(true);
        var organization = new OrganizationWrite(new OrganizationWrite.Fields("Co", null, null, null,
            null, null, null, null,
            Set.of(OrganizationRole.CLIENT), true), Set.of("legalName", "roles", "active"), 0L);
        var product = new ProductWrite(new ProductWrite.Fields("P", "Product", null, null, true, null, null, null, "KG"),
            Set.of("code", "name", "active"), 0L);
        var grade = new GradeWrite(new GradeWrite.Fields("G", "Grade", 0,
            new BigDecimal("12.000"), new BigDecimal("2.5000"), "USD", true),
            Set.of("code", "name", "displayOrder", "defaultCartonNetKg", "defaultPricePerKg",
            "defaultCurrency", "active"), 0L);
        if (!role.equals("OPERATOR")) {
            assertForbidden(() -> organizationService.create(organization));
            assertForbidden(() -> organizationService.patch(id, organization));
            assertForbidden(() -> catalogService.createProduct(product));
            assertForbidden(() -> catalogService.patchProduct(id, product));
        }
        assertForbidden(() -> catalogService.createGrade(id, grade));
        assertForbidden(() -> catalogService.patchGrade(id, id, grade));
        verifyNoInteractions(organizations, catalog);
        verify(details, never()).auditContext(any());
        verify(details, never()).canEditSetup(any(), any());
    }

    @Test
    void gradePatchChecksCurrencyPairAfterMergingAndBeforeMutation() {
        UUID product = UUID.randomUUID(), grade = UUID.randomUUID();
        when(actors.current()).thenReturn(new ProjectActor(UUID.randomUUID(), Set.of("ADMIN")));
        when(catalog.grade(product, grade, true)).thenReturn(new Grade(grade, product, 2L, "G",
            "Grade", 0, null, new BigDecimal("2.5000"), "USD", true));
        var priceOnlyClear = new GradeWrite(new GradeWrite.Fields(null, null, null, null, null, null, null),
            Set.of("defaultPricePerKg"), 2L);
        assertThatThrownBy(() -> catalogService.patchGrade(product, grade, priceOnlyClear))
        .isInstanceOfSatisfying(ProjectProblem.class, problem -> {
            assertThat(problem.status()).isEqualTo(400);
            assertThat(problem.errors()).extracting(ProjectProblem.FieldError::field).contains("defaultCurrency");
        });
        verify(catalog, never()).updateGrade(any());
        verify(details, never()).auditContext(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OPERATOR", "VIEWER", "REVIEWER"})
    void geographyIsReadableForEveryExistingRoleWithoutMutation(String role) {
        when(actors.current()).thenReturn(new ProjectActor(UUID.randomUUID(), Set.of(role)));
        when(geography.countries()).thenReturn(List.of(new Country("TN", "Tunisia")));
        when(geography.activeCountry("TN")).thenReturn(true);
        var query = new SetupListQuery(1, 2, null, false, null);
        UUID city = UUID.randomUUID();
        when(geography.cities("TN", query)).thenReturn(new GeographyStore.CityPage(
            List.of(new City(city, "TN", "Tunis", true)), 3));
        assertThat(geographyService.countries()).singleElement().satisfies(country -> {
            assertThat(country.code()).isEqualTo("TN");
            assertThat(country.name()).isEqualTo("Tunisia");
        });
        var result = geographyService.cities("TN", query);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(city);
            assertThat(item.countryCode()).isEqualTo("TN");
            assertThat(item.name()).isEqualTo("Tunis");
            assertThat(item.active()).isTrue();
        });
        verifyNoInteractions(details, organizations, catalog);
    }

    @Test
    void geographyRejectsMissingReadPermissionAndUnavailableCountryBeforeSearching() {
        var query = new SetupListQuery(0, 20, null, false, null);
        when(actors.current()).thenReturn(new ProjectActor(UUID.randomUUID(), Set.of()));
        assertForbidden(() -> geographyService.countries());
        assertForbidden(() -> geographyService.cities("TN", query));
        verifyNoInteractions(geography);
        when(actors.current()).thenReturn(new ProjectActor(UUID.randomUUID(), Set.of("VIEWER")));
        assertThatThrownBy(() -> geographyService.cities("ZZ", query)).isInstanceOfSatisfying(ProjectProblem.class,
            problem -> assertThat(problem.status()).isEqualTo(404));
        verify(geography, never()).cities(any(), any());
        when(geography.activeCountry("TN")).thenReturn(true);
        when(geography.cities("TN", query)).thenReturn(new GeographyStore.CityPage(List.of(), 0));
        var empty = geographyService.cities("TN", query);
        assertThat(empty.items()).isEmpty();
        assertThat(empty.totalElements()).isZero();
        assertThat(empty.totalPages()).isZero();
        verifyNoInteractions(details);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OPERATOR"})
    void companyMetadataWritesPreserveOtherFieldsAndCheckNoopAndStaleVersions(String role) {
        UUID actor = UUID.randomUUID(), id = UUID.randomUUID();
        when(actors.current()).thenReturn(new ProjectActor(actor, Set.of(role)));
        var fields = new OrganizationWrite.Fields("Company", "TN", "Legacy address", "REG", "Trade", "TAX",
            "sales@example.test", "+216 00123456", Set.of(OrganizationRole.CARRIER), true);
        var current = new Organization(id, 3, fields.legalName(), fields.countryCode(), fields.addressText(),
            fields.registrationNumber(), fields.tradeName(), fields.taxIdentifier(), fields.email(), fields.phone(),
            fields.roles(), true, Instant.EPOCH, Instant.EPOCH, List.of());
        when(organizations.create(any(), eq(fields))).thenReturn(current);
        var created = organizationService.create(new OrganizationWrite(fields,
            Set.of("legalName", "roles", "tradeName", "taxIdentifier", "email", "phone"), null));
        assertThat(created.tradeName()).isEqualTo("Trade");
        assertThat(created.taxIdentifier()).isEqualTo("TAX");
        assertThat(created.email()).isEqualTo("sales@example.test");
        assertThat(created.phone()).isEqualTo("+216 00123456");
        assertThat(created.roles()).containsExactly(OrganizationRole.CARRIER);
        verify(details).auditContext(actor);
        clearInvocations(details, organizations);
        when(organizations.find(id, true)).thenReturn(current);
        assertThatThrownBy(() -> organizationService.patch(id, new OrganizationWrite(fields, Set.of("email"), 2L)))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(409));
        var noop = organizationService.patch(id, new OrganizationWrite(fields, Set.of("email"), 3L));
        assertThat(noop.version()).isEqualTo(3);
        verify(organizations, never()).update(any(), any());
        verifyNoInteractions(details);
        var clear = new OrganizationWrite.Fields(null, null, null, null, null, null, null, null, null, null);
        var next = new Organization(id, 3, "Company", "TN", "Legacy address", "REG", "Trade", "TAX", null,
            "+216 00123456", Set.of(OrganizationRole.CARRIER), true, Instant.EPOCH, Instant.EPOCH, List.of());
        when(organizations.update(next, current)).thenReturn(next);
        var response = organizationService.patch(id, new OrganizationWrite(clear, Set.of("email"), 3L));
        assertThat(response.email()).isNull();
        assertThat(response.tradeName()).isEqualTo("Trade");
        assertThat(response.taxIdentifier()).isEqualTo("TAX");
        assertThat(response.phone()).isEqualTo("+216 00123456");
        assertThat(response.countryCode()).isEqualTo("TN");
        assertThat(response.addressText()).isEqualTo("Legacy address");
        assertThat(response.registrationNumber()).isEqualTo("REG");
        verify(organizations).update(next, current);
        verify(details).auditContext(actor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OPERATOR", "VIEWER", "REVIEWER"})
    void addressReadsAndCompanyResponsesIncludeInactiveAddresses(String role) {
        UUID company = UUID.randomUUID();
        var address = new OrganizationAddress(UUID.randomUUID(), company, OrganizationAddress.Kind.BUSINESS,
            "TN", null, "Tunis", "Road", null, "00123", false, 2);
        var parent = addressParent(company, List.of(address));
        when(actors.current()).thenReturn(new ProjectActor(UUID.randomUUID(), Set.of(role)));
        when(organizations.find(company, false)).thenReturn(parent);
        var query = new SetupListQuery(0, 20, null, false, null);
        when(organizations.list(query)).thenReturn(new OrganizationStore.Page(List.of(parent), 1));
        assertThat(organizationService.addresses(company)).singleElement().satisfies(item -> {
            assertThat(item.active()).isFalse();
            assertThat(item.postalCode()).isEqualTo("00123");
            assertThat(item.organizationId()).isEqualTo(company);
        });
        assertThat(organizationService.get(company).addresses()).isEqualTo(organizationService.addresses(company));
        assertThat(organizationService.list(query).items().getFirst().addresses()).hasSize(1);
        verifyNoInteractions(details);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "REVIEWER"})
    void readOnlyRolesCannotWriteAddresses(String role) {
        when(actors.current()).thenReturn(new ProjectActor(UUID.randomUUID(), Set.of(role)));
        var write = new AddressWrite(null, Set.of(), 0L, null);
        UUID company = UUID.randomUUID();
        assertForbidden(() -> organizationService.createAddress(company, write));
        assertForbidden(() -> organizationService.patchAddress(company, UUID.randomUUID(), write));
        verifyNoInteractions(organizations, details);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OPERATOR"})
    void addressCreateLocksAndChecksParentWithoutChangingItsMetadataVersion(String role) {
        UUID company = UUID.randomUUID(), actor = UUID.randomUUID();
        when(actors.current()).thenReturn(new ProjectActor(actor, Set.of(role)));
        when(organizations.find(company, true)).thenReturn(addressParent(company, List.of()));
        var fields = new AddressWrite.Fields(OrganizationAddress.Kind.BUSINESS, "TN", null, "Tunis", "Road", null, "00123", true);
        assertThatThrownBy(() -> organizationService.createAddress(company, new AddressWrite(fields, Set.of(), null, 2L)))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(409));
        verify(organizations, never()).validateAddressLocation(any());
        verify(organizations, never()).createAddress(any());
        verifyNoInteractions(details);
        clearInvocations(organizations);
        when(organizations.createAddress(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = organizationService.createAddress(company, new AddressWrite(fields, Set.of(), null, 3L));
        assertThat(response.version()).isZero();
        assertThat(response.postalCode()).isEqualTo("00123");
        var order = inOrder(organizations, details);
        order.verify(organizations).find(company, true);
        order.verify(organizations).validateAddressLocation(any());
        order.verify(details).auditContext(actor);
        order.verify(organizations).createAddress(any());
        verify(organizations, never()).update(any(), any());
    }

    @Test
    void addressPatchChecksStaleVersionsAndPreservesHistoricalLocationsWithoutNoopAudits() {
        UUID company = UUID.randomUUID(), id = UUID.randomUUID(), city = UUID.randomUUID(), actor = UUID.randomUUID();
        var current = new OrganizationAddress(id, company, OrganizationAddress.Kind.BUSINESS, "TN", city, null,
            "Road", null, "00123", true, 4);
        when(actors.current()).thenReturn(new ProjectActor(actor, Set.of("OPERATOR")));
        when(organizations.address(company, id, true)).thenReturn(current);
        var fields = new AddressWrite.Fields(null, null, null, null, "Road", null, null, false);
        assertThatThrownBy(() -> organizationService.patchAddress(company, id,
            new AddressWrite(fields, Set.of("addressLine1"), 3L, null)))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(409));
        assertThat(organizationService.patchAddress(company, id,
            new AddressWrite(fields, Set.of("addressLine1"), 4L, null)).version()).isEqualTo(4);
        verifyNoInteractions(details);
        verify(organizations, never()).updateAddress(any());
        clearInvocations(organizations);
        var next = new OrganizationAddress(id, company, current.kind(), "TN", city, null, "Road", null, "00123", false, 4);
        when(organizations.updateAddress(next)).thenReturn(next);
        var response = organizationService.patchAddress(company, id, new AddressWrite(fields, Set.of("active"), 4L, null));
        assertThat(response.active()).isFalse();
        assertThat(response.cityId()).isEqualTo(city);
        verify(organizations, never()).validateAddressLocation(any());
        var order = inOrder(organizations, details);
        order.verify(organizations).find(company, true);
        order.verify(organizations).address(company, id, true);
        order.verify(details).auditContext(actor);
        order.verify(organizations).updateAddress(next);
        verify(organizations, never()).update(any(), any());
    }

    @Test
    void addressCityModeSwitchRequiresClearingOldModeAndRevalidatesLocation() {
        UUID company = UUID.randomUUID(), id = UUID.randomUUID(), city = UUID.randomUUID();
        var current = new OrganizationAddress(id, company, OrganizationAddress.Kind.BUSINESS, "TN", city, null,
            "Road", null, "00123", true, 0);
        when(actors.current()).thenReturn(new ProjectActor(UUID.randomUUID(), Set.of("ADMIN")));
        when(organizations.address(company, id, true)).thenReturn(current);
        var fields = new AddressWrite.Fields(null, null, null, "Manual", null, null, null, null);
        assertThatThrownBy(() -> organizationService.patchAddress(company, id,
            new AddressWrite(fields, Set.of("manualCity"), 0L, null)))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(400));
        var noCity = new AddressWrite.Fields(OrganizationAddress.Kind.OTHER, "TN", null, null, "Road", null, null, true);
        when(organizations.find(company, true)).thenReturn(addressParent(company, List.of()));
        assertThatThrownBy(() -> organizationService.createAddress(company, new AddressWrite(noCity, Set.of(), null, null)))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(400));
        verifyNoInteractions(details);
        var next = new OrganizationAddress(id, company, current.kind(), "TN", null, "Manual", "Road", null, "00123", true, 0);
        when(organizations.updateAddress(next)).thenReturn(next);
        assertThat(organizationService.patchAddress(company, id,
            new AddressWrite(fields, Set.of("cityId", "manualCity"), 0L, null)).manualCity()).isEqualTo("Manual");
        verify(organizations).validateAddressLocation(next);
        verify(organizations).updateAddress(next);
        clearInvocations(details);
        doThrow(ProjectProblem.field(404, "addressId", "not_found", "Address not found."))
            .when(organizations).address(company, id, true);
        assertThatThrownBy(() -> organizationService.patchAddress(company, id,
            new AddressWrite(fields, Set.of("manualCity"), 0L, null)))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(404));
        verifyNoInteractions(details);
    }

    private Organization addressParent(UUID id, List<OrganizationAddress> addresses) {
        return new Organization(id, 3, "Company", "TN", "Legacy address", "REG", null, null, null, null,
            Set.of(OrganizationRole.CLIENT), true, Instant.EPOCH, Instant.EPOCH, addresses);
    }













    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OPERATOR"})
    void productMetadataWritesKeepAliasesAndOmittedValues(String role) {
        UUID id = UUID.randomUUID(), actor = UUID.randomUUID();
        when(actors.current()).thenReturn(new ProjectActor(actor, Set.of(role)));
        var fields = new ProductWrite.Fields("P", "Product", "Species", "123456", true, "Description", "Frozen", "-18 C", "KG");
        var current = new Product(id, 2, "P", "Product", "Species", "123456", true, "Description", "Frozen", "-18 C", "KG", List.of(), List.of());
        when(catalog.createProduct(any(), eq(fields))).thenReturn(current);
        var response = catalogService.createProduct(new ProductWrite(fields, Set.of("code", "name"), null));
        assertThat(response.name()).isEqualTo("Product");
        assertThat(response.commercialName()).isEqualTo(response.name());
        assertThat(response.processingMethod()).isEqualTo("Frozen");
        assertThat(response.defaultMeasurementUnit()).isEqualTo("KG");
        clearInvocations(details);
        when(catalog.product(id, true)).thenReturn(current);
        assertThatThrownBy(() -> catalogService.patchProduct(id, new ProductWrite(fields, Set.of("name"), 1L)))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(409));
        assertThat(catalogService.patchProduct(id, new ProductWrite(fields, Set.of("name"), 2L)).version()).isEqualTo(2);
        verifyNoInteractions(details);
        var clear = new ProductWrite.Fields(null, null, null, null, null, null, null, null, null);
        var next = new Product(id, 2, "P", "Product", "Species", "123456", true, null, "Frozen", "-18 C", "KG", List.of(), List.of());
        when(catalog.updateProduct(next)).thenReturn(next);
        var patched = catalogService.patchProduct(id, new ProductWrite(clear, Set.of("description"), 2L));
        assertThat(patched.description()).isNull();
        assertThat(patched.scientificName()).isEqualTo("Species");
        assertThat(patched.storageRequirements()).isEqualTo("-18 C");
        verify(catalog).updateProduct(next);
        verify(details).auditContext(actor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEWER", "REVIEWER"})
    void taxonomyWritesRequireMetadataWriteRoles(String role) {
        when(actors.current()).thenReturn(new ProjectActor(UUID.randomUUID(), Set.of(role)));
        assertForbidden(() -> catalogService.createTaxonomy(null));
        assertForbidden(() -> catalogService.patchTaxonomy(UUID.randomUUID(), null));
        assertForbidden(() -> catalogService.replaceCategories(UUID.randomUUID(), null));
        verifyNoInteractions(catalog, details);
    }

    @Test
    void taxonomyPatchesAndAssignmentsUseTheirOwnVersionAndNoopRules() {
        UUID category = UUID.randomUUID(), product = UUID.randomUUID(), actor = UUID.randomUUID();
        when(actors.current()).thenReturn(new ProjectActor(actor, Set.of("OPERATOR")));
        var current = new TaxonomyCategory(category, 3, "T", "Taxonomy", "Description", false);
        when(catalog.taxonomy(category, true)).thenReturn(current);
        var fields = new TaxonomyWrite.Fields("T", null, null, null);
        assertThatThrownBy(() -> catalogService.patchTaxonomy(category, new TaxonomyWrite(fields, Set.of("code"), 2L)))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(409));
        assertThat(catalogService.patchTaxonomy(category, new TaxonomyWrite(fields, Set.of("code"), 3L)).version()).isEqualTo(3);
        verify(catalog, never()).updateTaxonomy(any());
        verifyNoInteractions(details);
        var storedProduct = new Product(product, 5, "P", "Product", null, null, true, null, null, null, "KG",
            List.of(current), List.of());
        when(catalog.product(product, true)).thenReturn(storedProduct);
        when(catalog.assignedCategories(product)).thenReturn(List.of(current));
        assertThatThrownBy(() -> catalogService.replaceCategories(product, new TaxonomyWrite.Assignment(4, Set.of(category))))
            .isInstanceOfSatisfying(ProjectProblem.class, problem -> assertThat(problem.status()).isEqualTo(409));
        assertThat(catalogService.replaceCategories(product, new TaxonomyWrite.Assignment(5, Set.of(category))))
            .singleElement().satisfies(item -> assertThat(item.active()).isFalse());
        verifyNoInteractions(details);
        verify(catalog, never()).replaceCategories(any(), any(), any());
        when(catalog.assignedCategories(product)).thenReturn(List.of());
        assertThat(catalogService.replaceCategories(product, new TaxonomyWrite.Assignment(5, Set.of()))).isEmpty();
        verify(catalog).validateCategories(Set.of(), Set.of(category));
        verify(catalog).replaceCategories(product, Set.of(category), Set.of());
        verify(details).auditContext(actor);
        verify(catalog, never()).updateGrade(any());
        verify(catalog, never()).updateProduct(any());
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ProjectProblem.class,
            problem -> assertThat(problem.status()).isEqualTo(403));
    }
}

package com.exportdocs.commercial;

import com.exportdocs.catalog.api.CatalogMapper;
import com.exportdocs.catalog.application.CatalogService;
import com.exportdocs.catalog.application.GradeWrite;
import com.exportdocs.catalog.application.ProductWrite;
import com.exportdocs.catalog.domain.Grade;
import com.exportdocs.catalog.infrastructure.CatalogStore;
import com.exportdocs.party.api.OrganizationMapper;
import com.exportdocs.party.application.OrganizationService;
import com.exportdocs.party.application.OrganizationWrite;
import com.exportdocs.party.domain.OrganizationRole;
import com.exportdocs.party.infrastructure.OrganizationStore;
import com.exportdocs.project.application.ProjectActor;
import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.infrastructure.ProjectActorProvider;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommercialPolicyTest {
    final ProjectActorProvider actors = mock(ProjectActorProvider.class);
    final ProjectDetailsStore details = mock(ProjectDetailsStore.class);
    final OrganizationStore organizations = mock(OrganizationStore.class);
    final CatalogStore catalog = mock(CatalogStore.class);
    final OrganizationService organizationService = new OrganizationService(organizations, actors, details,
        mock(OrganizationMapper.class));
    final CatalogService catalogService = new CatalogService(catalog, actors, details, mock(CatalogMapper.class));
    @ParameterizedTest
    @ValueSource(strings = {
        "OPERATOR", "REVIEWER", "VIEWER"
    }
    )
    void formAssignmentCannotGrantDirectoryOrCatalogMutation(String role) {
        UUID actor = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(actors.current()).thenReturn(new ProjectActor(actor, Set.of(role)));
        // A project form grant cannot override the master-data ADMIN requirement.
        when(details.canEditSetup(any(), any())).thenReturn(true);
        var organization = new OrganizationWrite(new OrganizationWrite.Fields("Co", null, null, null,
            Set.of(OrganizationRole.CLIENT), true), Set.of("legalName", "roles", "active"), 0L);
        var product = new ProductWrite(new ProductWrite.Fields("P", "Product", null, null, true),
            Set.of("code", "name", "active"), 0L);
        var grade = new GradeWrite(new GradeWrite.Fields("G", "Grade", 0,
            new BigDecimal("12.000"), new BigDecimal("2.5000"), "USD", true),
            Set.of("code", "name", "displayOrder", "defaultCartonNetKg", "defaultPricePerKg",
            "defaultCurrency", "active"), 0L);
        assertForbidden(() -> organizationService.create(organization));
        assertForbidden(() -> organizationService.patch(id, organization));
        assertForbidden(() -> catalogService.createProduct(product));
        assertForbidden(() -> catalogService.patchProduct(id, product));
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

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ProjectProblem.class,
            problem -> assertThat(problem.status()).isEqualTo(403));
    }
}

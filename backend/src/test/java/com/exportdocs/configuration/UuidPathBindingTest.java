package com.exportdocs.configuration;

import com.exportdocs.account.application.AccountIdentity;
import com.exportdocs.account.application.AccountPrincipal;
import com.exportdocs.account.application.AccountRecord;
import com.exportdocs.account.application.AccountRole;
import com.exportdocs.account.infrastructure.AccountSecurityConfiguration;
import com.exportdocs.catalog.api.CatalogController;
import com.exportdocs.catalog.api.CatalogDtos;
import com.exportdocs.catalog.api.CatalogRequestParser;
import com.exportdocs.catalog.application.CatalogService;
import com.exportdocs.party.api.OrganizationController;
import com.exportdocs.party.api.OrganizationRequestParser;
import com.exportdocs.party.application.OrganizationService;
import com.exportdocs.project.api.CommercialPartiesController;
import com.exportdocs.project.api.CommercialPartiesRequestParser;
import com.exportdocs.project.application.CommercialPartiesService;
import com.exportdocs.project.application.ProjectProblem;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrganizationController.class, CatalogController.class, CommercialPartiesController.class})
@Import({UuidWebBinding.class, AccountSecurityConfiguration.class, OrganizationRequestParser.class,
        CatalogRequestParser.class, CommercialPartiesRequestParser.class})
class UuidPathBindingTest {
    private static final UUID ID = UUID.fromString("a1234567-b123-c123-d123-e123456789ab");
    @Autowired MockMvc mvc;
    @MockitoBean AccountIdentity identity;
    @MockitoBean OrganizationService organizations;
    @MockitoBean CatalogService catalog;
    @MockitoBean CommercialPartiesService commercial;

    @BeforeEach
    void readyAccount() {
        when(identity.validate(any())).thenReturn(new AccountRecord(ID, "admin", "Fictional Admin", true,
                List.of(AccountRole.ADMIN), false, 0, "unused", 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1-1-1-1-1", "a1234567b123c123d123e123456789ab",
            "g1234567-b123-c123-d123-e123456789ab", " a1234567-b123-c123-d123-e123456789ab "})
    void malformedIdsAreRejectedBeforeBusinessLookup(String invalid) throws Exception {
        for (String path : List.of("/api/organizations/{id}", "/api/products/{id}",
                "/api/products/{id}/grades", "/api/products/" + ID + "/grades/{id}",
                "/api/projects/{id}/commercial-parties")) {
            mvc.perform(authenticated(get(path, invalid))).andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        verifyNoInteractions(organizations, catalog, commercial);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a1234567-b123-c123-d123-e123456789ab", "A1234567-B123-C123-D123-E123456789AB"})
    void fullLengthIdsAreAcceptedInEitherCase(String value) throws Exception {
        when(catalog.getProduct(ID)).thenReturn(new CatalogDtos.ProductResponse(ID, 0, "P", "Product",
                null, null, true));
        mvc.perform(authenticated(get("/api/products/{id}", value))).andExpect(status().isOk());
        verify(catalog).getProduct(ID);
    }

    @Test
    void wellFormedMissingIdKeepsNotFoundSemantics() throws Exception {
        when(catalog.getProduct(ID)).thenThrow(new ProjectProblem(404, "Product not found.", List.of()));
        mvc.perform(authenticated(get("/api/products/{id}", ID))).andExpect(status().isNotFound());
        verify(catalog).getProduct(ID);
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                new AccountPrincipal(ID, 0), null, List.of())));
    }
}

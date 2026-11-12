package com.exportdocs.commercial;

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
import com.exportdocs.party.api.OrganizationDtos;
import com.exportdocs.party.api.OrganizationRequestParser;
import com.exportdocs.party.application.OrganizationService;
import com.exportdocs.party.domain.OrganizationRole;
import com.exportdocs.project.api.CommercialPartiesController;
import com.exportdocs.project.api.CommercialPartiesRequestParser;
import com.exportdocs.project.api.CommercialPartiesResponse;
import com.exportdocs.project.application.CommercialPartiesService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrganizationController.class, CatalogController.class, CommercialPartiesController.class})
@Import({AccountSecurityConfiguration.class, OrganizationRequestParser.class,
        CatalogRequestParser.class, CommercialPartiesRequestParser.class})
class CommercialCsrfTransportTest {
    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRADE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    @Autowired MockMvc mvc;
    @MockitoBean AccountIdentity identity;
    @MockitoBean OrganizationService organizations;
    @MockitoBean CatalogService catalog;
    @MockitoBean CommercialPartiesService commercial;

    @BeforeEach
    void responses() {
        when(identity.validate(any())).thenReturn(new AccountRecord(ID, "admin", "Fictional Admin", true,
                List.of(AccountRole.ADMIN), false, 0, "unused", 0));
        var organization = new OrganizationDtos.Response(ID, 0, "Fictional Company", null, null, null,
                Set.of(OrganizationRole.CLIENT), true, Instant.EPOCH, Instant.EPOCH);
        when(organizations.create(any())).thenReturn(organization);
        when(organizations.patch(any(), any())).thenReturn(organization);
        var product = new CatalogDtos.ProductResponse(ID, 0, "P", "Fictional Product", null, null, true);
        when(catalog.createProduct(any())).thenReturn(product);
        when(catalog.patchProduct(any(), any())).thenReturn(product);
        var grade = new CatalogDtos.GradeResponse(GRADE, ID, 0, "G", "Fictional Grade", 0, null, null, null, true);
        when(catalog.createGrade(any(), any())).thenReturn(grade);
        when(catalog.patchGrade(any(), any(), any())).thenReturn(grade);
        when(commercial.patch(any(), any())).thenReturn(new CommercialPartiesResponse(ID, 0, null, null, null, null));
    }

    static Stream<Arguments> writes() {
        return Stream.of(
                Arguments.of("POST", "/api/organizations", "{\"legalName\":\"Company\",\"roles\":[\"CLIENT\"]}", 201),
                Arguments.of("PATCH", "/api/organizations/" + ID, "{\"expectedVersion\":0}", 200),
                Arguments.of("POST", "/api/products", "{\"code\":\"P\",\"name\":\"Product\"}", 201),
                Arguments.of("PATCH", "/api/products/" + ID, "{\"expectedVersion\":0}", 200),
                Arguments.of("POST", "/api/products/" + ID + "/grades", "{\"code\":\"G\",\"name\":\"Grade\"}", 201),
                Arguments.of("PATCH", "/api/products/" + ID + "/grades/" + GRADE, "{\"expectedVersion\":0}", 200),
                Arguments.of("PATCH", "/api/projects/" + ID + "/commercial-parties", "{\"expectedVersion\":0}", 200));
    }

    @ParameterizedTest
    @MethodSource("writes")
    void csrfHeaderDoesNotBecomeAnUnknownRequestParameter(String method, String path, String body, int expected) throws Exception {
        var result = mvc.perform(CommercialTestRequests.write(authenticated(method, path), body))
                .andExpect(status().is(expected)).andReturn();
        assertThat(result.getRequest().getHeader("X-XSRF-TOKEN")).isNotBlank();
        assertThat(result.getRequest().getParameter("_csrf")).isNull();
    }

    @ParameterizedTest
    @MethodSource("writes")
    void legitimateCsrfDoesNotAllowUnknownQueryParameters(String method, String path, String body, int expected) throws Exception {
        mvc.perform(CommercialTestRequests.write(authenticated(method, path).queryParam("unexpected", "value"), body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("unexpected"));
        verifyNoInteractions(organizations, catalog, commercial);
    }

    @ParameterizedTest
    @MethodSource("writes")
    void missingAndInvalidTokensRemainForbidden(String method, String path, String body, int expected) throws Exception {
        mvc.perform(authenticated(method, path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(authenticated(method, path).cookie(new Cookie("XSRF-TOKEN", "expected-token"))
                .header("X-XSRF-TOKEN", "different-token")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        verifyNoInteractions(organizations, catalog, commercial);
    }

    @Test
    void aCsrfNamedQueryParameterIsStillOutsideTheJsonApiContract() throws Exception {
        mvc.perform(CommercialTestRequests.write(authenticated("POST", "/api/products")
                .queryParam("_csrf", "unexpected"), "{\"code\":\"P\",\"name\":\"Product\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("_csrf"));
        verifyNoInteractions(organizations, catalog, commercial);
    }

    private MockHttpServletRequestBuilder authenticated(String method, String path) {
        return request(HttpMethod.valueOf(method), path).with(authentication(
                UsernamePasswordAuthenticationToken.authenticated(new AccountPrincipal(ID, 0), null, List.of())));
    }
}

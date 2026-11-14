package com.exportdocs.commercial;

import com.exportdocs.account.application.AccountIdentity;
import com.exportdocs.account.application.AccountPrincipal;
import com.exportdocs.account.application.AccountRecord;
import com.exportdocs.account.application.AccountRole;
import com.exportdocs.account.infrastructure.AccountSecurityConfiguration;
import com.exportdocs.catalog.api.CatalogController;
import com.exportdocs.catalog.api.CatalogDtos;
import com.exportdocs.catalog.api.CatalogRequestParser;
import com.exportdocs.catalog.api.TaxonomyController;
import com.exportdocs.catalog.application.CatalogService;
import com.exportdocs.geography.api.GeographyController;
import com.exportdocs.geography.api.GeographyDtos;
import com.exportdocs.geography.application.GeographyService;
import com.exportdocs.party.api.SetupListQuery;
import com.exportdocs.party.api.OrganizationController;
import com.exportdocs.party.api.OrganizationDtos;
import com.exportdocs.party.api.OrganizationRequestParser;
import com.exportdocs.party.application.OrganizationService;
import com.exportdocs.party.domain.OrganizationRole;
import com.exportdocs.party.domain.OrganizationAddress;
import com.exportdocs.project.api.CommercialPartiesController;
import com.exportdocs.project.api.CommercialPartiesRequestParser;
import com.exportdocs.project.api.CommercialPartiesResponse;
import com.exportdocs.project.application.CommercialPartiesService;
import com.exportdocs.project.application.ProjectProblem;
import com.exportdocs.project.api.ShipmentProductController;
import com.exportdocs.project.api.ShipmentProductRequestParser;
import com.exportdocs.project.api.ShipmentProductResponse;
import com.exportdocs.project.application.ShipmentProductService;
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
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrganizationController.class, CatalogController.class, CommercialPartiesController.class,
        GeographyController.class, TaxonomyController.class,
        ShipmentProductController.class})
@Import({AccountSecurityConfiguration.class, OrganizationRequestParser.class,
        CatalogRequestParser.class, CommercialPartiesRequestParser.class,
        ShipmentProductRequestParser.class})
class CommercialCsrfTransportTest {
    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRADE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    @Autowired MockMvc mvc;
    @MockitoBean AccountIdentity identity;
    @MockitoBean OrganizationService organizations;
    @MockitoBean CatalogService catalog;
    @MockitoBean CommercialPartiesService commercial;
    @MockitoBean GeographyService geography;
    @MockitoBean ShipmentProductService shipmentProducts;

    @BeforeEach
    void responses() {
        when(identity.validate(any())).thenReturn(new AccountRecord(ID, "admin", "Fictional Admin", true,
                List.of(AccountRole.ADMIN), false, 0, "unused", 0));
        var organization = new OrganizationDtos.Response(ID, 0, "Fictional Company", null, null, null,
                null, null, null, null,
                Set.of(OrganizationRole.CLIENT), true, Instant.EPOCH, Instant.EPOCH, List.of());
        when(organizations.create(any())).thenReturn(organization);
        when(organizations.patch(any(), any())).thenReturn(organization);
        var address = new OrganizationDtos.AddressResponse(GRADE, ID, OrganizationAddress.Kind.BUSINESS,
                "TN", null, "Tunis", "Road", null, "00123", true, 0);
        when(organizations.createAddress(any(), any())).thenReturn(address);
        when(organizations.patchAddress(any(), any(), any())).thenReturn(address);
        var product = new CatalogDtos.ProductResponse(ID, 0, "P", "Fictional Product", null, null, true,
                "Fictional Product", null, null, null, "KG", List.of(), List.of());
        when(catalog.createProduct(any())).thenReturn(product);
        when(catalog.patchProduct(any(), any())).thenReturn(product);
        var grade = new CatalogDtos.GradeResponse(GRADE, ID, 0, "G", "Fictional Grade", 0, null, null, null, true);
        when(catalog.createGrade(any(), any())).thenReturn(grade);
        when(catalog.patchGrade(any(), any(), any())).thenReturn(grade);
        var taxonomy = new CatalogDtos.TaxonomyResponse(GRADE, 0, "T", "Taxonomy", null, true);
        when(catalog.createTaxonomy(any())).thenReturn(taxonomy);
        when(catalog.patchTaxonomy(any(), any())).thenReturn(taxonomy);
        when(catalog.replaceCategories(any(), any())).thenReturn(List.of(taxonomy));
        when(commercial.patch(any(), any())).thenReturn(new CommercialPartiesResponse(ID, 0, null, null, null, null));
        when(shipmentProducts.create(any(), any())).thenReturn(new ShipmentProductResponse(ID, 0, ID, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, 1));
        when(shipmentProducts.patch(any(), any(), any())).thenReturn(new ShipmentProductResponse(ID, 1, ID, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, 2));
    }

    static Stream<Arguments> writes() {
        return Stream.of(
                Arguments.of("POST", "/api/organizations", "{\"legalName\":\"Company\",\"roles\":[\"CLIENT\"]}", 201),
                Arguments.of("PATCH", "/api/organizations/" + ID, "{\"expectedVersion\":0}", 200),
                Arguments.of("POST", "/api/organizations/" + ID + "/addresses",
                        "{\"kind\":\"BUSINESS\",\"countryCode\":\"TN\",\"manualCity\":\"Tunis\",\"addressLine1\":\"Road\"}", 201),
                Arguments.of("PATCH", "/api/organizations/" + ID + "/addresses/" + GRADE,
                        "{\"expectedVersion\":0,\"active\":false}", 200),
                Arguments.of("POST", "/api/products", "{\"code\":\"P\",\"name\":\"Product\"}", 201),
                Arguments.of("PATCH", "/api/products/" + ID, "{\"expectedVersion\":0}", 200),
                Arguments.of("POST", "/api/product-taxonomy-categories", "{\"code\":\"T\",\"name\":\"Taxonomy\"}", 201),
                Arguments.of("PATCH", "/api/product-taxonomy-categories/" + GRADE, "{\"expectedVersion\":0,\"active\":false}", 200),
                Arguments.of("PUT", "/api/products/" + ID + "/taxonomy-categories",
                        "{\"expectedProductVersion\":0,\"categoryIds\":[\"" + GRADE + "\"]}", 200),
                Arguments.of("POST", "/api/products/" + ID + "/grades", "{\"code\":\"G\",\"name\":\"Grade\"}", 201),
                Arguments.of("PATCH", "/api/products/" + ID + "/grades/" + GRADE, "{\"expectedVersion\":0}", 200),
                Arguments.of("PATCH", "/api/projects/" + ID + "/commercial-parties", "{\"expectedVersion\":0}", 200),
                Arguments.of("POST", "/api/projects/" + ID + "/products",
                        "{\"productId\":\"" + ID + "\",\"expectedProjectVersion\":0}", 201),
                Arguments.of("PATCH", "/api/projects/" + ID + "/products/" + GRADE,
                        "{\"expectedVersion\":0,\"expectedProjectVersion\":1}", 200));
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
        verifyNoInteractions(organizations, catalog, commercial, shipmentProducts);
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

    @ParameterizedTest
    @ValueSource(strings = {"/api/countries", "/api/countries/TN/cities"})
    void geographyRequiresAValidReadySession(String path) throws Exception {
        mvc.perform(request(HttpMethod.GET, path)).andExpect(status().isUnauthorized());
        when(identity.validate(any())).thenReturn(new AccountRecord(ID, "admin", "Fictional Admin", true,
                List.of(AccountRole.ADMIN), true, 0, "unused", 0));
        mvc.perform(authenticated("GET", path)).andExpect(status().isForbidden());
        when(identity.validate(any())).thenThrow(new ProjectProblem(
                401, "Session is no longer valid.", List.of()));
        mvc.perform(authenticated("GET", path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(geography);
    }

    @Test
    void geographySerializesContractAndReusesLiteralPrefixPagination() throws Exception {
        when(geography.countries()).thenReturn(List.of(new GeographyDtos.CountryResponse("TN", "Tunisia")));
        mvc.perform(authenticated("GET", "/api/countries")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TN")).andExpect(jsonPath("$[0].name").value("Tunisia"));
        when(geography.cities(any(), any())).thenReturn(new GeographyDtos.CityPage(
                List.of(new GeographyDtos.CityResponse(ID, "TN", "A_!%", true)), 2, 3, 7, 3));
        mvc.perform(authenticated("GET", "/api/countries/TN/cities").queryParam("q", " A_!% ")
                .queryParam("page", "2").queryParam("size", "3")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].countryCode").value("TN"))
                .andExpect(jsonPath("$.items[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.items[0].active").value(true))
                .andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(7)).andExpect(jsonPath("$.totalPages").value(3));
        verify(geography).cities("TN", new SetupListQuery(2, 3, "a!_!!!%%", false, null));
        mvc.perform(authenticated("GET", "/api/countries/TN/cities")).andExpect(status().isOk());
        verify(geography).cities("TN", new SetupListQuery(0, 20, null, false, null));
        mvc.perform(authenticated("GET", "/api/countries/TN/cities").queryParam("q", "a".repeat(200)))
                .andExpect(status().isOk());
    }

    @Test
    void geographyRejectsMalformedCountriesAndInvalidUnknownOrRepeatedQueries() throws Exception {
        for (String country : List.of("tn", "T", "TNN", "12")) {
            mvc.perform(authenticated("GET", "/api/countries/" + country + "/cities"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("countryCode"));
        }
        for (String[] invalid : List.of(new String[]{"q", ""}, new String[]{"q", " "},
                new String[]{"q", "a".repeat(201)}, new String[]{"q", " " + "a".repeat(200)},
                new String[]{"page", "-1"}, new String[]{"page", "1.0"},
                new String[]{"page", "2147483648"}, new String[]{"size", "0"}, new String[]{"size", "101"},
                new String[]{"size", ""}, new String[]{"includeInactive", "true"}, new String[]{"sort", "name"})) {
            mvc.perform(authenticated("GET", "/api/countries/TN/cities").queryParam(invalid[0], invalid[1]))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value(invalid[0]));
        }
        mvc.perform(authenticated("GET", "/api/countries/TN/cities").queryParam("q", "one", "two"))
                .andExpect(status().isBadRequest());
        mvc.perform(authenticated("GET", "/api/countries/TN/cities").queryParam("page", "2147483647")
                .queryParam("size", "100")).andExpect(status().isBadRequest());
        mvc.perform(authenticated("GET", "/api/countries").queryParam("page", "0"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(geography);
    }

    @Test
    void addressReadsRequireReadySessionsAndRejectQueryParameters() throws Exception {
        String path = "/api/organizations/" + ID + "/addresses";
        mvc.perform(request(HttpMethod.GET, path)).andExpect(status().isUnauthorized());
        var address = new OrganizationDtos.AddressResponse(GRADE, ID, OrganizationAddress.Kind.OTHER,
                "TN", null, "Tunis", "Road", null, "00123", false, 2);
        when(organizations.addresses(ID)).thenReturn(List.of(address));
        mvc.perform(authenticated("GET", path)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationId").value(ID.toString()))
                .andExpect(jsonPath("$[0].kind").value("OTHER"))
                .andExpect(jsonPath("$[0].cityId").isEmpty())
                .andExpect(jsonPath("$[0].postalCode").value("00123"))
                .andExpect(jsonPath("$[0].active").value(false));
        mvc.perform(authenticated("GET", path).queryParam("includeInactive", "true"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("includeInactive"));
        mvc.perform(authenticated("GET", path).queryParam("q", "one", "two"))
                .andExpect(status().isBadRequest());
        when(identity.validate(any())).thenReturn(new AccountRecord(ID, "admin", "Fictional Admin", true,
                List.of(AccountRole.ADMIN), true, 0, "unused", 0));
        mvc.perform(authenticated("GET", path)).andExpect(status().isForbidden());
    }





    @Test
    void taxonomyAndProductFiltersValidateQueriesAndKeepActiveOnlyDefaults() throws Exception {
        when(catalog.taxonomy(any())).thenReturn(new CatalogDtos.TaxonomyPage(List.of(), 0, 20, 0, 0));
        mvc.perform(authenticated("GET", "/api/product-taxonomy-categories").queryParam("q", " T_!% "))
                .andExpect(status().isOk());
        verify(catalog).taxonomy(new SetupListQuery(0, 20, "t!_!!!%%", false, null));
        mvc.perform(authenticated("GET", "/api/product-taxonomy-categories").queryParam("q", "x".repeat(201)))
                .andExpect(status().isBadRequest());
        mvc.perform(authenticated("GET", "/api/product-taxonomy-categories").queryParam("q", "one", "two"))
                .andExpect(status().isBadRequest());
        when(catalog.products(any(), any())).thenReturn(new CatalogDtos.ProductPage(List.of(), 0, 20, 0, 0));
        mvc.perform(authenticated("GET", "/api/products").queryParam("categoryId", GRADE.toString()))
                .andExpect(status().isOk());
        verify(catalog).products(GRADE, new SetupListQuery(0, 20, null, false, null));
        mvc.perform(authenticated("GET", "/api/products").queryParam("categoryId", "1-1-1-1-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(authenticated("GET", "/api/products/" + ID + "/taxonomy-categories").queryParam("q", "unexpected"))
                .andExpect(status().isBadRequest());
        mvc.perform(request(HttpMethod.GET, "/api/product-taxonomy-categories")).andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.GET, "/api/products/" + ID + "/taxonomy-categories")).andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder authenticated(String method, String path) {
        return request(HttpMethod.valueOf(method), path).with(authentication(
                UsernamePasswordAuthenticationToken.authenticated(new AccountPrincipal(ID, 0), null, List.of())));
    }
}

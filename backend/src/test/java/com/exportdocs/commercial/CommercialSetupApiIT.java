package com.exportdocs.commercial;

import com.exportdocs.account.application.AccountPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CommercialSetupApiIT {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    UUID admin, operator;

    @BeforeEach void fixtures() {
        jdbc.execute("TRUNCATE project, organization, product, app_user, audit_log CASCADE");
        admin = user("admin", "ADMIN"); operator = user("operator", "OPERATOR");
        user("viewer", "VIEWER"); user("reviewer", "REVIEWER");
    }

    @Test void minimalOrganizationAndProductExposeNullsDefaultsAndLocation() throws Exception {
        var result = write(post("/api/organizations"), "{\"legalName\":\"  Fictional Co  \",\"roles\":[\"CLIENT\",\"IMPORTER\"]}", "admin");
        status(result, 201); var org = body(result);
        assertThat(org.path("legalName").asText()).isEqualTo("Fictional Co");
        assertThat(org.path("version").asLong()).isZero(); assertThat(org.path("active").asBoolean()).isTrue();
        for (String field : List.of("countryCode", "addressText", "registrationNumber")) assertThat(org.get(field).isNull()).isTrue();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/organizations/" + org.path("id").asText());
        var product = product("MiXeD");
        assertThat(product.path("code").asText()).isEqualTo("MiXeD");
        assertThat(product.get("scientificName").isNull()).isTrue(); assertThat(product.get("hsCode").isNull()).isTrue();
        var grade = create("/api/products/" + product.path("id").asText() + "/grades", "{\"code\":\"50-80\",\"name\":\"Grade\"}");
        assertThat(grade.path("displayOrder").asInt()).isZero(); assertThat(grade.get("defaultCartonNetKg").isNull()).isTrue();
        assertThat(grade.get("defaultPricePerKg").isNull()).isTrue(); assertThat(grade.get("defaultCurrency").isNull()).isTrue();
    }

    @Test void exactDefaultsPatchMergeNoopAndParentVersion() throws Exception {
        var product = product("P"); String grades = "/api/products/" + product.path("id").asText() + "/grades";
        var grade = create(grades, "{\"code\":\"G\",\"name\":\"Grade\",\"defaultCartonNetKg\":\"999999999999999.123\",\"defaultPricePerKg\":\"99999999999999.1234\",\"defaultCurrency\":\"USD\"}");
        assertThat(grade.path("defaultCartonNetKg").asText()).isEqualTo("999999999999999.123");
        assertThat(grade.path("defaultPricePerKg").asText()).isEqualTo("99999999999999.1234");
        String route = grades + "/" + grade.path("id").asText(); long audits = audits();
        var same = write(patch(route), "{\"expectedVersion\":0,\"defaultCartonNetKg\":\"999999999999999.123\"}", "admin");
        status(same, 200); assertThat(body(same).path("version").asLong()).isZero(); assertThat(audits()).isEqualTo(audits);
        problem(write(patch(route), "{\"expectedVersion\":0,\"defaultPricePerKg\":null}", "admin"), 400, "defaultCurrency");
        assertThat(audits()).isEqualTo(audits);
        var updated = write(patch(route), "{\"expectedVersion\":0,\"defaultPricePerKg\":null,\"defaultCurrency\":null,\"defaultCartonNetKg\":\"12\"}", "admin");
        status(updated,200); assertThat(body(updated).path("defaultCartonNetKg").asText()).isEqualTo("12.000");
        assertThat(body(updated).path("version").asLong()).isEqualTo(1);
        assertThat(read("/api/products/" + product.path("id").asText(), "admin").path("version").asLong()).isZero();
        problem(write(patch(route), "{\"expectedVersion\":0}", "admin"), 409, "expectedVersion");
    }

    @ParameterizedTest @ValueSource(strings={"0", "\"0\"", "\"0.000\"", "\"1.0000\"", "\"1e2\"", "\"-1\"", "\"1000000000000000\"", "true"})
    void invalidBoxDefaultsNeverWrite(String value) throws Exception {
        var p = product("P"); long before = audits();
        problem(write(post("/api/products/" + p.path("id").asText() + "/grades"), "{\"code\":\"G\",\"name\":\"Grade\",\"defaultCartonNetKg\":" + value + "}", "admin"),400,"defaultCartonNetKg");
        assertThat(audits()).isEqualTo(before); assertThat(count("product_category")).isZero();
    }

    @ParameterizedTest @ValueSource(strings={"1.25", "\"1.00000\"", "\"1e2\"", "\"-1\"", "\"100000000000000\""})
    void invalidPricesNeverWrite(String value) throws Exception {
        var p=product("P"); long before=audits();
        problem(write(post("/api/products/"+p.path("id").asText()+"/grades"),"{\"code\":\"G\",\"name\":\"Grade\",\"defaultPricePerKg\":"+value+",\"defaultCurrency\":\"USD\"}","admin"),400,"defaultPricePerKg");
        assertThat(audits()).isEqualTo(before);
    }

    @Test void validatesCurrencyCountryHsRoleStringsAndUnknownFields() throws Exception {
        for (String input : List.of("{\"legalName\":\"X\",\"roles\":[1]}", "{\"legalName\":\"X\",\"roles\":[\"CLIENT\",\"CLIENT\"]}", "{\"legalName\":\"X\",\"roles\":[]}", "{\"legalName\":\"X\",\"roles\":[\"CLIENT\"],\"countryCode\":\"XX\"}","{\"legalName\":\"X\",\"roles\":[\"CLIENT\"],\"version\":0}")) {
            status(write(post("/api/organizations"),input,"admin"),400);
        }
        problem(write(post("/api/products"),"{\"code\":\"P\",\"name\":\"P\",\"hsCode\":\"12\"}","admin"),400,"hsCode");
        var p=product("P"); String route="/api/products/"+p.path("id").asText()+"/grades";
        for(String input:List.of("{\"defaultPricePerKg\":\"0\"}","{\"defaultPricePerKg\":\"0\",\"defaultCurrency\":\"ZZZ\"}","{\"defaultCurrency\":\"USD\"}")) {
            status(write(post(route),"{\"code\":\"G\",\"name\":\"G\","+input.substring(1),"admin"),400);
        }
        assertThat(count("organization")).isZero();
    }

    @Test void nonadminsReadAndCannotChangeAnyMasterDataEvenWithFormGrants() throws Exception {
        var org=organization(); var p=product("P"); String grades="/api/products/"+p.path("id").asText()+"/grades";
        var g=create(grades,"{\"code\":\"G\",\"name\":\"G\"}");
        var project=create("/api/projects","{\"reference\":\"EXP-1\"}"); grant(project,"INVOICE"); grantForm(project,"PACKING_LIST");
        long before=audits();
        for(String role:List.of("operator","viewer","reviewer")) {
            for(String route:List.of("/api/organizations","/api/products",grades,"/api/organizations/"+org.path("id").asText(),"/api/products/"+p.path("id").asText(),grades+"/"+g.path("id").asText())) status(mvc.perform(get(route).with(account(role))).andReturn(),200);
            problem(write(post("/api/organizations"),"{\"legalName\":\"New\",\"roles\":[\"CLIENT\"]}",role),403,null);
            problem(write(post("/api/products"),"{\"code\":\"NEW\",\"name\":\"New\"}",role),403,null);
            problem(write(post(grades),"{\"code\":\"NEW\",\"name\":\"New\"}",role),403,null);
            for(String route:List.of("/api/organizations/"+org.path("id").asText(),"/api/products/"+p.path("id").asText(),grades+"/"+g.path("id").asText())) problem(write(patch(route),"{\"expectedVersion\":0,\"active\":false}",role),403,null);
        }
        assertThat(audits()).isEqualTo(before);
    }

    @Test void organizationRolesUpdateOnceAndCannotRemoveUsedRole() throws Exception {
        var org=organization(); String route="/api/organizations/"+org.path("id").asText(); long before=audits();
        status(write(patch(route),"{\"expectedVersion\":0,\"roles\":[\"CONSIGNEE\",\"IMPORTER\",\"CLIENT\",\"SUPPLIER\"]}","admin"),200);
        assertThat(audits()).isEqualTo(before);
        var changed=write(patch(route),"{\"expectedVersion\":0,\"roles\":[\"SUPPLIER\",\"CLIENT\",\"IMPORTER\",\"CONSIGNEE\",\"EXPORTER\"]}","admin"); status(changed,200);
        assertThat(body(changed).path("version").asLong()).isEqualTo(1); assertThat(body(changed).path("updatedAt")).isNotEqualTo(org.path("updatedAt"));
        var project=create("/api/projects","{\"reference\":\"EXP-1\",\"consigneeId\":\""+org.path("id").asText()+"\"}");
        before=audits(); var removed=write(patch(route),"{\"expectedVersion\":1,\"roles\":[\"CLIENT\"]}","admin"); problem(removed,409,"roles");
        assertThat(body(removed).path("errors").findValuesAsText("code")).contains("role_in_use"); assertThat(audits()).isEqualTo(before);
        assertThat(read("/api/projects/"+project.path("id").asText(),"admin").path("consigneeId")).isEqualTo(org.path("id"));
    }

    @Test void listsUseActiveLiteralPrefixRoleAndStablePages() throws Exception {
        var org=organization(); var other=create("/api/organizations","{\"legalName\":\"Fake_One\",\"roles\":[\"CLIENT\",\"IMPORTER\"]}");
        create("/api/organizations","{\"legalName\":\"FakeXOne\",\"roles\":[\"CLIENT\"]}");
        var page=read("/api/organizations?q=fake_&role=CLIENT&size=1","operator"); assertThat(page.path("totalElements").asLong()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("id")).isEqualTo(other.path("id"));
        status(write(patch("/api/organizations/"+org.path("id").asText()),"{\"expectedVersion\":0,\"active\":false}","admin"),200);
        assertThat(read("/api/organizations","operator").path("totalElements").asLong()).isEqualTo(2);
        assertThat(read("/api/organizations?includeInactive=true","admin").path("totalElements").asLong()).isEqualTo(3);
        status(mvc.perform(get("/api/organizations?includeInactive=true").with(account("operator"))).andReturn(),403);
        assertThat(read("/api/organizations/"+org.path("id").asText(),"operator").path("active").asBoolean()).isFalse();
        assertThat(read("/api/organizations?q=missing","operator").path("totalPages").asInt()).isZero();
        for(String query:List.of("page=0&page=1","q=","size=101","page=2147483647&size=2","includeInactive=1","role=1","sort=id")) status(mvc.perform(get("/api/organizations?"+query).with(account("admin"))).andReturn(),400);
    }

    @Test void duplicateCodesAndGradeParentMismatchAreHandled() throws Exception {
        var p=product("P"); var other=product("p");
        problem(write(post("/api/products"),"{\"code\":\" P \",\"name\":\"Other\"}","admin"),409,"code");
        String route="/api/products/"+p.path("id").asText()+"/grades"; var g=create(route,"{\"code\":\"G\",\"name\":\"G\"}");
        problem(write(post(route),"{\"code\":\"G\",\"name\":\"Another\"}","admin"),409,"code");
        String wrong="/api/products/"+other.path("id").asText()+"/grades/"+g.path("id").asText();
        status(mvc.perform(get(wrong).with(account("admin"))).andReturn(),404); status(write(patch(wrong),"{\"expectedVersion\":0}","admin"),404);
    }

    @Test void commercialPartiesShareCanonicalConsigneeAndProjectVersion() throws Exception {
        var org=organization(); var project=create("/api/projects","{\"reference\":\"EXP-1\"}"); String route="/api/projects/"+project.path("id").asText();
        var empty=read(route+"/commercial-parties","admin"); for(String field:List.of("supplierId","clientId","importerId","consigneeId")) assertThat(empty.get(field).isNull()).isTrue();
        String id=org.path("id").asText();
        var result=write(patch(route+"/commercial-parties"),"{\"expectedVersion\":0,\"supplierId\":\""+id+"\",\"clientId\":\""+id+"\",\"importerId\":\""+id+"\",\"consigneeId\":\""+id+"\"}","admin"); status(result,200);
        assertThat(body(result).path("version").asLong()).isEqualTo(1); assertThat(count("project_party")).isEqualTo(4);
        assertThat(read(route,"admin").path("consigneeId").asText()).isEqualTo(id); assertThat(read(route,"admin").path("version").asLong()).isEqualTo(1);
        long before=audits(); status(write(patch(route+"/commercial-parties"),"{\"expectedVersion\":1,\"supplierId\":\""+id+"\"}","admin"),200); assertThat(audits()).isEqualTo(before);
        problem(write(patch(route+"/commercial-parties"),"{\"expectedVersion\":0}","admin"),409,"expectedVersion");
        status(write(patch(route),"{\"expectedVersion\":1,\"consigneeId\":null}","admin"),200);
        var latest=read(route+"/commercial-parties","admin"); assertThat(latest.get("consigneeId").isNull()).isTrue(); assertThat(latest.path("version").asLong()).isEqualTo(2);
    }

    @Test void commercialPermissionAndInvalidReferenceFailuresAreAtomic() throws Exception {
        var p=create("/api/projects","{\"reference\":\"EXP-1\"}"); String route="/api/projects/"+p.path("id").asText()+"/commercial-parties";
        status(mvc.perform(get(route).with(account("operator"))).andReturn(),404); grant(p,"INVOICE");
        problem(write(patch(route),"{\"expectedVersion\":0}","operator"),403,null); grantForm(p,"PROJECT_SETUP");
        var org=create("/api/organizations","{\"legalName\":\"Client\",\"roles\":[\"CLIENT\"]}"); long before=audits();
        for(String field:List.of("supplierId","importerId","consigneeId")) problem(write(patch(route),"{\"expectedVersion\":0,\"clientId\":\""+org.path("id").asText()+"\",\""+field+"\":\""+org.path("id").asText()+"\"}","operator"),422,field);
        assertThat(audits()).isEqualTo(before); assertThat(count("project_party")).isZero();
        status(write(patch(route),"{\"expectedVersion\":0,\"clientId\":\""+org.path("id").asText()+"\"}","operator"),200);
        status(write(patch("/api/organizations/"+org.path("id").asText()),"{\"expectedVersion\":0,\"active\":false}","admin"),200);
        status(write(patch(route),"{\"expectedVersion\":1,\"clientId\":null}","operator"),200);
        problem(write(patch(route),"{\"expectedVersion\":2,\"clientId\":\""+org.path("id").asText()+"\"}","operator"),422,"clientId");
    }

    @Test void concurrentCommercialAndV2WritesHaveOneSharedWinner() throws Exception {
        var p=create("/api/projects","{\"reference\":\"EXP-RACE\"}"); var org=organization(); String route="/api/projects/"+p.path("id").asText();
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var first=executor.submit(()->{start.await(); return write(patch(route+"/commercial-parties"),"{\"expectedVersion\":0,\"supplierId\":\""+org.path("id").asText()+"\"}","admin").getResponse().getStatus();});
            var second=executor.submit(()->{start.await(); return write(patch(route),"{\"expectedVersion\":0,\"consigneeId\":\""+org.path("id").asText()+"\"}","admin").getResponse().getStatus();});
            start.countDown(); assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(read(route,"admin").path("version").asLong()).isEqualTo(1);
    }

    private JsonNode organization() throws Exception { return create("/api/organizations","{\"legalName\":\"Fictional Co\",\"roles\":[\"SUPPLIER\",\"CLIENT\",\"IMPORTER\",\"CONSIGNEE\"]}"); }
    private JsonNode product(String code) throws Exception { return create("/api/products","{\"code\":\""+code+"\",\"name\":\"Fictional product\"}"); }
    private JsonNode create(String route,String input) throws Exception { var r=write(post(route),input,"admin"); status(r,201); return body(r); }
    private JsonNode read(String route,String subject) throws Exception {var r=mvc.perform(get(route).with(account(subject))).andReturn(); status(r,200); return body(r);}
    private MvcResult write(MockHttpServletRequestBuilder request,String input,String subject) throws Exception {return mvc.perform(request.with(account(subject)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(input)).andReturn();}
    private JsonNode body(MvcResult result) throws Exception {return json.readTree(result.getResponse().getContentAsString());}
    private void status(MvcResult result,int expected) throws Exception { assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(expected); }
    private void problem(MvcResult result,int expected,String field) throws Exception {status(result,expected);assertThat(result.getResponse().getContentType()).startsWith("application/problem+json"); if(field!=null)assertThat(body(result).path("errors").findValuesAsText("field")).contains(field);}
    private long count(String table) {if(!List.of("product_category","organization","project_party").contains(table))throw new IllegalArgumentException();return jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class);}
    private long audits(){return jdbc.queryForObject("SELECT count(*) FROM audit_log",Long.class);}
    private UUID user(String name,String role){UUID id=UUID.randomUUID();jdbc.update("INSERT INTO app_user(id,external_subject,display_name) VALUES (?,?,?)",id,name,name);jdbc.update("INSERT INTO role_assignment(user_id,role) VALUES (?,?)",id,role);jdbc.update("INSERT INTO local_account(user_id,username,password_hash,must_change_password) VALUES (?,?,?,false)",id,name,passwords.encode("FictionalTestPass1!"));return id;}
    private RequestPostProcessor account(String subject){UUID id=jdbc.queryForObject("SELECT user_id FROM local_account WHERE username=?",UUID.class,subject);return authentication(UsernamePasswordAuthenticationToken.authenticated(new AccountPrincipal(id,0),null,List.of()));}
    private void grant(JsonNode project,String form){UUID id=UUID.fromString(project.path("id").asText());jdbc.update("INSERT INTO project_access(project_id,user_id) VALUES (?,?)",id,operator);grantForm(project,form);}
    private void grantForm(JsonNode project,String form){jdbc.update("INSERT INTO project_form_assignment(project_id,user_id,form_key) VALUES (?,?,?)",UUID.fromString(project.path("id").asText()),operator,form);}
}

package com.exportdocs.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccountApiIT {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url",POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username",POSTGRES::getUsername);
        properties.add("spring.datasource.password",POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    private static final String PASSWORD="FictionalTestPass1!";
    private UUID adminId;
    private UUID operatorId;

    @BeforeEach void fixtures() {
        jdbc.execute("TRUNCATE app_user,organization,project,port,audit_log RESTART IDENTITY CASCADE");
        adminId=account("administrator","ADMIN",false);
        operatorId=account("operator","OPERATOR",false);
    }

    @Test void loginRequiresCsrfAndCredentialsAndRotatesSession() throws Exception {
        assertStatus(mvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody("administrator",PASSWORD))).andReturn(),403);
        Client client=new Client();client.session=new MockHttpSession();String oldId=client.session.getId();refresh(client);
        assertStatus(unsafe(client,post("/api/auth/login"),loginBody("administrator","wrong")),401);
        var result=unsafe(client,post("/api/auth/login"),loginBody("administrator",PASSWORD));assertStatus(result,200);
        client.session=(MockHttpSession)result.getRequest().getSession(false);
        assertThat(client.session.getId()).isNotEqualTo(oldId);
        assertThat(body(result).path("username").asText()).isEqualTo("administrator");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("passwordHash","password_hash",PASSWORD);
        assertStatus(read(client,get("/api/auth/me")),200);
    }

    @Test void logoutInvalidatesTheSessionAndRefreshesCsrf() throws Exception {
        var client=login("administrator",PASSWORD);
        assertStatus(unsafe(client,post("/api/auth/logout"),null),204);
        assertStatus(read(client,get("/api/auth/me")),401);
        Client anonymous=new Client();refresh(anonymous);
        assertStatus(unsafe(anonymous,post("/api/auth/logout"),null),204);
    }

    @Test void temporaryAccountsMustChangePasswordBeforeBusinessOrAdministration() throws Exception {
        var admin=login("administrator",PASSWORD);
        var user=createOperator(admin,"new.operator");
        var temporary=login("new.operator",PASSWORD);
        assertStatus(read(temporary,get("/api/auth/me")),200);
        assertStatus(read(temporary,get("/api/projects")),403);
        assertStatus(read(temporary,get("/api/admin/users")),403);
        assertStatus(unsafe(temporary,post("/api/auth/password"),"{\"currentPassword\":\""+PASSWORD+"\",\"newPassword\":\""+PASSWORD+"\"}"),400);
        assertThat(body(read(temporary,get("/api/auth/me"))).path("mustChangePassword").asBoolean()).isTrue();
        assertStatus(unsafe(temporary,post("/api/auth/password"),"{\"currentPassword\":\"wrong\",\"newPassword\":\"ChangedTestPass1!\"}"),401);
        assertStatus(unsafe(temporary,post("/api/auth/password"),"{\"currentPassword\":\""+PASSWORD+"\",\"newPassword\":\"ChangedTestPass1!\"}"),204);
        assertStatus(read(temporary,get("/api/auth/me")),401);
        assertStatus(read(login("new.operator","ChangedTestPass1!"),get("/api/projects")),200);
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM local_account WHERE user_id=?",Boolean.class,UUID.fromString(user.path("id").asText()))).isFalse();
    }

    @Test void adminCreatesListsReadsAndUpdatesAccountsWithOptimisticVersions() throws Exception {
        var admin=login("administrator",PASSWORD);
        var created=createOperator(admin,"  New.Operator  ");UUID id=UUID.fromString(created.path("id").asText());
        assertThat(created.path("username").asText()).isEqualTo("new.operator");
        assertThat(created.path("version").asLong()).isZero();
        var listed=read(admin,get("/api/admin/users?page=0&size=2"));assertStatus(listed,200);
        assertThat(body(listed).path("totalElements").asLong()).isEqualTo(3);
        assertStatus(read(admin,get("/api/admin/users/"+id)),200);
        var changed=unsafe(admin,patch("/api/admin/users/"+id),"{\"expectedVersion\":0,\"displayName\":\"Updated fictional user\"}");assertStatus(changed,200);
        assertThat(body(changed).path("version").asLong()).isEqualTo(1);
        var noOp=unsafe(admin,patch("/api/admin/users/"+id),"{\"expectedVersion\":1}");assertStatus(noOp,200);
        assertThat(body(noOp).path("version").asLong()).isEqualTo(1);
        assertStatus(unsafe(admin,patch("/api/admin/users/"+id),"{\"expectedVersion\":0,\"active\":false}"),409);
    }

    @Test void rejectsDuplicateUsernameUnknownFieldsNullsAndInvalidPasswordBytes() throws Exception {
        var admin=login("administrator",PASSWORD);
        createOperator(admin,"new.operator");
        var duplicate=unsafe(admin,post("/api/admin/users"),createBody("NEW.OPERATOR"));assertStatus(duplicate,409);
        assertThat(body(duplicate).path("errors").findValuesAsText("field")).contains("username");
        assertStatus(unsafe(admin,patch("/api/admin/users/"+operatorId),"{\"expectedVersion\":0,\"active\":null}"),400);
        assertStatus(unsafe(admin,patch("/api/admin/users/"+operatorId),"{\"expectedVersion\":0,\"displayName\":42}"),400);
        assertStatus(unsafe(admin,patch("/api/admin/users/"+operatorId),"{\"expectedVersion\":0,\"password_hash\":\"bad\"}"),400);
        var payload=json.createObjectNode().put("username","unicode.operator").put("displayName","Fictional Unicode User").put("initialPassword","é".repeat(37));
        payload.putArray("roles").add("OPERATOR");
        assertStatus(unsafe(admin,post("/api/admin/users"),payload.toString()),400);
        var hash=jdbc.queryForObject("SELECT password_hash FROM local_account WHERE user_id=?",String.class,operatorId);
        assertThat(hash).startsWith("{bcrypt}");assertThat(encoder.matches(PASSWORD,hash)).isTrue();
    }

    @Test void resetRenameAndDeactivationRevokeExistingSessions() throws Exception {
        var admin=login("administrator",PASSWORD);var operator=login("operator",PASSWORD);
        assertStatus(unsafe(admin,post("/api/admin/users/"+operatorId+"/password"),"{\"expectedVersion\":0,\"temporaryPassword\":\"NewTemporaryPass1!\"}"),200);
        assertStatus(read(operator,get("/api/auth/me")),401);
        var temporary=login("operator","NewTemporaryPass1!");
        assertThat(body(read(temporary,get("/api/auth/me"))).path("mustChangePassword").asBoolean()).isTrue();
        assertStatus(unsafe(admin,patch("/api/admin/users/"+operatorId),"{\"expectedVersion\":1,\"username\":\"renamed.operator\"}"),200);
        assertStatus(read(temporary,get("/api/auth/me")),401);
        var renamed=login("renamed.operator","NewTemporaryPass1!");
        assertStatus(unsafe(admin,patch("/api/admin/users/"+operatorId),"{\"expectedVersion\":2,\"active\":false}"),200);
        assertStatus(read(renamed,get("/api/auth/me")),401);
    }

    @Test void numericAndQuotedNumericEnumOrdinalsAreRejected() throws Exception {
        var admin=login("administrator",PASSWORD);UUID project=project(admin);
        for(String value:List.of("0","\"0\"")) {
            var create="{\"username\":\"bad.roles\",\"displayName\":\"Fictional User\",\"initialPassword\":\""+PASSWORD+"\",\"roles\":["+value+"]}";
            assertStatus(unsafe(admin,post("/api/admin/users"),create),400);
            assertStatus(unsafe(admin,put("/api/admin/projects/"+project+"/operators/"+operatorId),"{\"formKeys\":["+value+"]}"),400);
        }
    }

    @Test void oversizedCurrentPasswordIsRejectedWithoutMutationOrSessionRevocation() throws Exception {
        var operator=login("operator",PASSWORD);
        var input=json.createObjectNode().put("currentPassword","é".repeat(37)).put("newPassword","NewDifferentPass1!");
        assertStatus(unsafe(operator,post("/api/auth/password"),input.toString()),401);
        assertStatus(read(operator,get("/api/auth/me")),200);
        assertThat(jdbc.queryForObject("SELECT credential_version FROM local_account WHERE user_id=?",Long.class,operatorId)).isZero();
    }

    @Test void passwordChangesAreAuditedWithoutPasswordsOrHashes() throws Exception {
        var admin=login("administrator",PASSWORD);
        createOperator(admin,"audit.operator");
        assertStatus(unsafe(admin,post("/api/admin/users/"+operatorId+"/password"),"{\"expectedVersion\":0,\"temporaryPassword\":\"NewTemporaryPass1!\"}"),200);
        var audit=String.join("",jdbc.queryForList("SELECT coalesce(before_json::text,'')||coalesce(after_json::text,'') FROM audit_log WHERE entity_table='local_account'",String.class));
        assertThat(audit).contains("password_changed").doesNotContain("password_hash",PASSWORD,"NewTemporaryPass1!","{bcrypt}");
    }

    @Test void reactivationDoesNotReviveASessionThatWasNeverUsedWhileDisabled() throws Exception {
        var admin=login("administrator",PASSWORD);var operator=login("operator",PASSWORD);
        assertStatus(unsafe(admin,patch("/api/admin/users/"+operatorId),"{\"expectedVersion\":0,\"active\":false}"),200);
        assertStatus(unsafe(admin,patch("/api/admin/users/"+operatorId),"{\"expectedVersion\":1,\"active\":true}"),200);
        assertStatus(read(operator,get("/api/auth/me")),401);
        assertStatus(read(login("operator",PASSWORD),get("/api/auth/me")),200);
    }

    @Test void lastActiveAdministratorCannotBeRemovedEvenConcurrently() throws Exception {
        UUID other=account("second.admin","ADMIN",false);
        var first=login("administrator",PASSWORD);var second=login("second.admin",PASSWORD);
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->{start.await();return unsafe(first,patch("/api/admin/users/"+adminId),"{\"expectedVersion\":0,\"active\":false}").getResponse().getStatus();});
            var b=executor.submit(()->{start.await();return unsafe(second,patch("/api/admin/users/"+other),"{\"expectedVersion\":0,\"active\":false}").getResponse().getStatus();});
            start.countDown();assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user u JOIN role_assignment r ON r.user_id=u.id WHERE u.active AND r.role='ADMIN'",Long.class)).isEqualTo(1);
    }

    @Test void assignmentGrantsWholeShipmentReadAndOnlyNamedFormEditing() throws Exception {
        var admin=login("administrator",PASSWORD);var operator=login("operator",PASSWORD);
        UUID project=project(admin);String assignment="/api/admin/projects/"+project+"/operators/"+operatorId;
        assertStatus(read(operator,get("/api/projects/"+project)),404);
        assertStatus(unsafe(operator,post("/api/projects"),"{\"reference\":\"FORBIDDEN\"}"),403);
        assertStatus(unsafe(admin,put(assignment),"{\"formKeys\":[\"PACKING_LIST\"]}"),200);
        assertStatus(read(operator,get("/api/projects/"+project)),200);
        var permission=body(read(operator,get("/api/projects/"+project+"/permissions")));
        assertThat(permission.path("canRead").asBoolean()).isTrue();
        assertThat(permission.path("canManageAssignments").asBoolean()).isFalse();
        assertThat(permission.path("editableForms").toString()).isEqualTo("[\"PACKING_LIST\"]");
        assertStatus(unsafe(operator,patch("/api/projects/"+project),"{\"expectedVersion\":0,\"notes\":\"forbidden\"}"),403);
        assertStatus(unsafe(admin,put(assignment),"{\"formKeys\":[\"PROJECT_SETUP\"]}"),200);
        assertStatus(unsafe(operator,patch("/api/projects/"+project),"{\"expectedVersion\":0,\"notes\":\"permitted\"}"),200);
        assertStatus(unsafe(admin,put(assignment),"{\"formKeys\":[]}"),200);
        assertStatus(read(operator,get("/api/projects/"+project)),200);
        assertStatus(unsafe(operator,patch("/api/projects/"+project),"{\"expectedVersion\":1,\"notes\":\"forbidden again\"}"),403);
        assertStatus(unsafe(admin,delete(assignment),null),204);
        assertStatus(read(operator,get("/api/projects/"+project)),404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_form_assignment WHERE project_id=? AND user_id=?",Long.class,project,operatorId)).isZero();
    }

    @Test void assignmentOperationsAreAdminOnlyIdempotentAndDoNotReviseShipments() throws Exception {
        var admin=login("administrator",PASSWORD);var operator=login("operator",PASSWORD);
        UUID project=project(admin);String route="/api/admin/projects/"+project+"/operators/"+operatorId;
        assertStatus(unsafe(operator,put(route),"{\"formKeys\":[]}"),403);
        assertStatus(unsafe(admin,put(route),"{\"formKeys\":[\"INVOICE\",\"INVOICE\"]}"),400);
        assertStatus(unsafe(admin,put(route),"{\"formKeys\":[\"UNKNOWN\"]}"),400);
        assertStatus(unsafe(admin,put(route),"{\"formKeys\":[]}"),200);
        long before=jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE project_id=?",Long.class,project);
        assertStatus(unsafe(admin,put(route),"{\"formKeys\":[]}"),200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE project_id=?",Long.class,project)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT version FROM project WHERE id=?",Long.class,project)).isZero();
        assertStatus(read(admin,get("/api/admin/projects/"+project+"/operators")),200);
        assertStatus(unsafe(admin,delete(route),null),204);assertStatus(unsafe(admin,delete(route),null),204);
        assertStatus(unsafe(admin,put("/api/admin/projects/"+project+"/operators/"+adminId),"{\"formKeys\":[]}"),400);
    }

    @Test void databaseRoleChangesTakeEffectForAnExistingSession() throws Exception {
        var admin=login("administrator",PASSWORD);var operator=login("operator",PASSWORD);UUID project=project(admin);
        assertStatus(unsafe(admin,put("/api/admin/projects/"+project+"/operators/"+operatorId),"{\"formKeys\":[\"PROJECT_SETUP\"]}"),200);
        assertStatus(unsafe(admin,patch("/api/admin/users/"+operatorId),"{\"expectedVersion\":0,\"roles\":[\"VIEWER\"]}"),200);
        assertStatus(read(operator,get("/api/projects/"+project)),200);
        assertStatus(unsafe(operator,patch("/api/projects/"+project),"{\"expectedVersion\":0,\"notes\":\"blocked\"}"),403);
        assertThat(body(read(operator,get("/api/projects/"+project+"/permissions"))).path("editableForms").isEmpty()).isTrue();
        assertStatus(read(operator,get("/api/admin/users")),403);
    }

    private UUID project(Client admin) throws Exception {
        var result=unsafe(admin,post("/api/projects"),"{\"reference\":\"TEST-"+UUID.randomUUID()+"\"}");assertStatus(result,201);
        return UUID.fromString(body(result).path("id").asText());
    }
    private JsonNode createOperator(Client admin,String username) throws Exception {
        var result=unsafe(admin,post("/api/admin/users"),createBody(username));assertStatus(result,201);return body(result);
    }
    private String createBody(String username) {
        var node=json.createObjectNode().put("username",username).put("displayName","Fictional operator").put("initialPassword",PASSWORD);
        node.putArray("roles").add("OPERATOR");return node.toString();
    }
    private String loginBody(String username,String password) { return json.createObjectNode().put("username",username).put("password",password).toString(); }
    private Client login(String username,String password) throws Exception {
        Client client=new Client();refresh(client);
        var result=unsafe(client,post("/api/auth/login"),loginBody(username,password));assertStatus(result,200);
        client.session=(MockHttpSession)result.getRequest().getSession(false);client.csrf=null;refresh(client);return client;
    }
    private void refresh(Client client) throws Exception {
        var request=get("/api/auth/csrf");if(client.session!=null && !client.session.isInvalid()) request.session(client.session);
        var result=mvc.perform(request).andReturn();assertStatus(result,204);client.csrf=result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(client.csrf).isNotNull();
    }
    private MvcResult unsafe(Client client,MockHttpServletRequestBuilder request,String input) throws Exception {
        if(client.csrf==null) refresh(client);
        request.cookie(client.csrf).header("X-XSRF-TOKEN",client.csrf.getValue());
        if(input!=null) request.contentType("application/json").content(input);
        return read(client,request);
    }
    private MvcResult read(Client client,MockHttpServletRequestBuilder request) throws Exception {
        if(client.session!=null && !client.session.isInvalid()) request.session(client.session);
        return mvc.perform(request).andReturn();
    }
    private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    private void assertStatus(MvcResult result,int expected) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(expected);
        if(expected>=400) {
            assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
            assertThat(body(result).path("errors").isArray()).isTrue();
        }
    }
    private UUID account(String username,String role,boolean temporary) {
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO app_user(id,display_name) VALUES (?,?)",id,"Fictional "+username);
        jdbc.update("INSERT INTO role_assignment(user_id,role) VALUES (?,?)",id,role);
        jdbc.update("INSERT INTO local_account(user_id,username,password_hash,must_change_password) VALUES (?,?,?,?)",id,username,encoder.encode(PASSWORD),temporary);return id;
    }
    private static final class Client { private MockHttpSession session;private Cookie csrf; }
}

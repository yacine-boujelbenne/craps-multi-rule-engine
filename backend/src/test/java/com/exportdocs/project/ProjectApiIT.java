package com.exportdocs.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.exportdocs.project.infrastructure.ProjectDetailsStore;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectApiIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean ProjectDetailsStore detailsStore;
    private UUID operator;
    private UUID viewer;
    private UUID company;
    private UUID otherCompany;
    private UUID loading;
    private UUID discharge;

    @BeforeEach
    void fixtures() {
        jdbc.execute("TRUNCATE project, organization, app_user, port, audit_log RESTART IDENTITY CASCADE");
        operator = user("operator", "OPERATOR");
        viewer = user("viewer", "VIEWER");
        user("admin", "ADMIN");
        company = company("Fictional Export Cooperative");
        otherCompany = company("Fictional Import Cooperative");
        loading = port("Fictional Loading Port", "TN");
        discharge = port("Fictional Discharge Port", "FR");
    }

    @Test
    void createsReferenceOnlyDraftWithOneShipmentAndUserAudit() throws Exception {
        var result = request(post("/api/projects"), "{\"reference\":\"  exp-1  \"}", "operator");
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode project = body(result);
        assertThat(project.path("reference").asText()).isEqualTo("EXP-1");
        assertThat(project.path("status").asText()).isEqualTo("DRAFT_DATA");
        assertThat(project.path("version").asLong()).isZero();
        assertThat(project.path("producerIds").isEmpty()).isTrue();
        for (String field : List.of("exporterId", "consigneeId", "notifyPartyId", "freightForwarderId", "shippingLineId",
                "destinationCountryCode", "portOfLoadingId", "portOfDischargeId", "expectedDepartureDate", "notes")) {
            assertThat(project.has(field)).as(field).isTrue();
            assertThat(project.get(field).isNull()).as(field).isTrue();
        }
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/projects/" + project.path("id").asText());
        assertThat(count("shipment_transport")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entity_table='project' AND actor_id=? AND actor_kind='USER' AND request_id IS NOT NULL", Long.class, operator)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_access WHERE user_id=?", Long.class, operator)).isEqualTo(1);
    }

    @Test
    void fullCreationAndRetrievalRoundTripPreserveDistinctParties() throws Exception {
        var payload = fullRequest("EXP-FULL");
        var created = body(request(post("/api/projects"), payload.toString(), "operator"));
        assertThat(created.path("id").asText()).isNotBlank();
        long auditBefore = count("audit_log");
        var fetched = body(mvc.perform(get("/api/projects/{id}", created.path("id").asText())
                .with(jwt().jwt(token -> token.subject("operator")))).andExpect(status().isOk()).andReturn());
        for (String field : List.of("reference", "exporterId", "producerIds", "consigneeId", "notifyPartyId", "freightForwarderId",
                "shippingLineId", "destinationCountryCode", "portOfLoadingId", "portOfDischargeId", "expectedDepartureDate", "notes")) {
            assertThat(fetched.get(field)).as(field).isEqualTo(payload.get(field));
        }
        assertThat(count("audit_log")).isEqualTo(auditBefore);
        assertThat(fetched.get("consigneeId")).isNotEqualTo(fetched.get("notifyPartyId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"reference\":null}", "{\"reference\":\" \"}", "{\"reference\":\"has spaces\"}"})
    void rejectsMissingOrInvalidMandatoryReference(String input) throws Exception {
        assertProblem(request(post("/api/projects"), input, "operator"), 400, "reference");
        assertThat(count("project")).isZero();
        assertThat(projectAuditCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"exporterId", "consigneeId", "notifyPartyId", "freightForwarderId", "shippingLineId", "producerIds"})
    void rejectsUnknownCompaniesForEveryPartyRole(String field) throws Exception {
        ObjectNode payload = json.createObjectNode().put("reference", "EXP-1");
        if (field.equals("producerIds")) payload.putArray(field).add(UUID.randomUUID().toString());
        else payload.put(field, UUID.randomUUID().toString());
        assertProblem(request(post("/api/projects"), payload.toString(), "operator"), 422, field);
        assertThat(count("project")).isZero();
        assertThat(projectAuditCount()).isZero();
    }

    @Test
    void rejectsInactiveCompanyAndIncompatibleRole() throws Exception {
        jdbc.update("DELETE FROM organization_role WHERE organization_id=? AND role='EXPORTER'", company);
        var payload = json.createObjectNode().put("reference", "EXP-1").put("exporterId", company.toString());
        assertProblem(request(post("/api/projects"), payload.toString(), "operator"), 422, "exporterId");
        jdbc.update("UPDATE organization SET active=false WHERE id=?", otherCompany);
        payload.remove("exporterId");
        payload.put("consigneeId", otherCompany.toString());
        assertProblem(request(post("/api/projects"), payload.toString(), "operator"), 422, "consigneeId");
    }

    @Test
    void rejectsUnknownPortAndCountryCode() throws Exception {
        assertProblem(request(post("/api/projects"), "{\"reference\":\"EXP-1\",\"portOfLoadingId\":\"" + UUID.randomUUID() + "\"}", "operator"), 422, "portOfLoadingId");
        assertProblem(request(post("/api/projects"), "{\"reference\":\"EXP-1\",\"destinationCountryCode\":\"XX\"}", "operator"), 400, "destinationCountryCode");
    }

    @Test
    void duplicateReferenceIsCaseInsensitiveAndAtomic() throws Exception {
        create("EXP-DUP");
        assertProblem(request(post("/api/projects"), "{\"reference\":\" exp-dup \"}", "operator"), 409, "reference");
        assertThat(count("project")).isEqualTo(1);
        assertThat(count("shipment_transport")).isEqualTo(1);
        assertThat(projectAuditCount()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicateRequestsProduceOneCreationAndOneConflict() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return request(post("/api/projects"), "{\"reference\":\"EXP-RACE\"}", "operator").getResponse().getStatus(); });
            var second = executor.submit(() -> { start.await(); return request(post("/api/projects"), "{\"reference\":\"EXP-RACE\"}", "operator").getResponse().getStatus(); });
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(count("project")).isEqualTo(1);
        assertThat(projectAuditCount()).isEqualTo(1);
    }

    @Test
    void unknownAndImmutableWriteFieldsAreRejected() throws Exception {
        for (String field : List.of("id", "status", "version", "createdBy", "shipmentIds")) {
            assertProblem(request(post("/api/projects"), "{\"reference\":\"EXP-1\",\"" + field + "\":null}", "operator"), 400, field);
        }
        var created = create("EXP-1");
        assertProblem(request(patch("/api/projects/" + created.path("id").asText()), "{\"expectedVersion\":0,\"id\":null}", "operator"), 400, "id");
        assertProblem(request(patch("/api/projects/" + created.path("id").asText()), "{\"expectedVersion\":0,\"status\":\"IN_PROGRESS\"}", "operator"), 400, "status");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "[]", "null", "{\"reference\":\"EXP-1\",\"expectedDepartureDate\":\"2026-02-30\"}",
            "{\"reference\":\"EXP-1\",\"exporterId\":\"bad-id\"}"})
    void malformedRequestsReturnStructured400(String input) throws Exception {
        assertProblem(request(post("/api/projects"), input, "operator"), 400, null);
        assertThat(count("project")).isZero();
    }

    @Test
    void missingProjectsAndMalformedIdsHaveConsistentErrors() throws Exception {
        assertProblem(mvc.perform(get("/api/projects/{id}", UUID.randomUUID()).with(jwt().jwt(token -> token.subject("operator")))).andReturn(), 404, null);
        assertProblem(request(patch("/api/projects/" + UUID.randomUUID()), "{\"expectedVersion\":0}", "operator"), 404, null);
        assertProblem(mvc.perform(get("/api/projects/bad-id").with(jwt().jwt(token -> token.subject("operator")))).andReturn(), 400, null);
    }

    @Test
    void patchChangesAllowedFieldsClearsNullableAndIncrementsOnce() throws Exception {
        var createdResult = request(post("/api/projects"), fullRequest("EXP-PATCH").toString(), "operator");
        assertThat(createdResult.getResponse().getStatus()).isEqualTo(201);
        var created = body(createdResult);
        String id = created.path("id").asText();
        var input = json.createObjectNode().put("expectedVersion", 0).put("reference", "exp-renamed");
        input.putNull("consigneeId");
        input.putNull("portOfLoadingId");
        input.putArray("producerIds").add(company.toString()).add(otherCompany.toString());
        var result = request(patch("/api/projects/" + id), input.toString(), "operator");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        var updated = body(result);
        assertThat(updated.path("version").asLong()).isEqualTo(1);
        assertThat(updated.path("reference").asText()).isEqualTo("EXP-RENAMED");
        assertThat(updated.path("id")).isEqualTo(created.path("id"));
        assertThat(updated.path("createdAt")).isEqualTo(created.path("createdAt"));
        assertThat(updated.path("consigneeId").isNull()).isTrue();
        assertThat(updated.path("portOfLoadingId").isNull()).isTrue();
        assertThat(updated.path("notifyPartyId")).isEqualTo(created.path("notifyPartyId"));
        assertThat(updated.path("notes")).isEqualTo(created.path("notes"));
        assertThat(updated.path("producerIds").size()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entity_table='project' AND operation='UPDATE'", Long.class)).isEqualTo(1);
        var fetched = body(mvc.perform(get("/api/projects/" + id).with(jwt().jwt(token -> token.subject("operator")))).andReturn());
        assertThat(fetched.path("reference")).isEqualTo(updated.path("reference"));
        assertThat(count("shipment_transport")).isEqualTo(1);
    }

    @Test
    void versionOnlyAndEquivalentPatchesAreNoOpsAndStaleUpdatesConflict() throws Exception {
        var project = create("EXP-NOOP");
        String route = "/api/projects/" + project.path("id").asText();
        long audits = count("audit_log");
        var noOp = body(request(patch(route), "{\"expectedVersion\":0}", "operator"));
        assertThat(noOp.path("version").asLong()).isZero();
        var same = body(request(patch(route), "{\"expectedVersion\":0,\"reference\":\" exp-noop \",\"producerIds\":[]}", "operator"));
        assertThat(same.path("version").asLong()).isZero();
        assertThat(count("audit_log")).isEqualTo(audits);
        var changed = request(patch(route), "{\"expectedVersion\":0,\"notes\":\"Changed\"}", "operator");
        assertThat(changed.getResponse().getStatus()).isEqualTo(200);
        assertProblem(request(patch(route), "{\"expectedVersion\":0,\"notes\":\"Stale\"}", "operator"), 409, "expectedVersion");
        assertThat(jdbc.queryForObject("SELECT notes FROM project", String.class)).isEqualTo("Changed");
    }

    @Test
    void partyOnlyAndTransportOnlyChangesEachIncrementTheProjectVersion() throws Exception {
        var project = create("EXP-CHILDREN");
        String route = "/api/projects/" + project.path("id").asText();
        var party = body(request(patch(route), "{\"expectedVersion\":0,\"producerIds\":[\"" + company + "\"]}", "operator"));
        assertThat(party.path("version").asLong()).isEqualTo(1);
        var transport = body(request(patch(route), "{\"expectedVersion\":1,\"portOfLoadingId\":\"" + loading + "\"}", "operator"));
        assertThat(transport.path("version").asLong()).isEqualTo(2);
        long audits = count("audit_log");
        var unchanged = body(request(patch(route), "{\"expectedVersion\":2,\"producerIds\":[\"" + company + "\"]}", "operator"));
        assertThat(unchanged.path("version").asLong()).isEqualTo(2);
        assertThat(count("audit_log")).isEqualTo(audits);
        var cleared = body(request(patch(route), "{\"expectedVersion\":2,\"producerIds\":[]}", "operator"));
        assertThat(cleared.path("version").asLong()).isEqualTo(3);
        assertThat(cleared.path("producerIds").isEmpty()).isTrue();
        assertThat(cleared.path("portOfLoadingId").asText()).isEqualTo(loading.toString());
    }

    @Test
    void concurrentPatchesAcceptOnlyOneWriterAtTheExpectedVersion() throws Exception {
        var project = create("EXP-VERSION-RACE");
        String route = "/api/projects/" + project.path("id").asText();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { start.await(); return request(patch(route), "{\"expectedVersion\":0,\"producerIds\":[\"" + company + "\"]}", "operator").getResponse().getStatus(); });
            var second = executor.submit(() -> { start.await(); return request(patch(route), "{\"expectedVersion\":0,\"producerIds\":[\"" + otherCompany + "\"]}", "operator").getResponse().getStatus(); });
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT version FROM project", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_party", Long.class)).isEqualTo(1);
    }

    @Test
    void retrievalUsesOneSnapshotWhenAChildUpdateCommitsBetweenQueries() throws Exception {
        var project = create("EXP-SNAPSHOT");
        UUID id = UUID.fromString(project.path("id").asText());
        String route = "/api/projects/" + id;
        var readerSelected = new CountDownLatch(1);
        var writerCommitted = new CountDownLatch(1);
        doAnswer(invocation -> {
            if (Thread.currentThread().getName().equals("project-snapshot-reader")) {
                readerSelected.countDown();
                if (!writerCommitted.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Writer did not commit");
            }
            return invocation.callRealMethod();
        }).when(detailsStore).load(id);
        try (var executor = Executors.newSingleThreadExecutor(task -> new Thread(task, "project-snapshot-reader"))) {
            var reader = executor.submit(() -> body(mvc.perform(get(route).with(jwt().jwt(token -> token.subject("operator"))))
                    .andExpect(status().isOk()).andReturn()));
            try {
                assertThat(readerSelected.await(15, TimeUnit.SECONDS)).isTrue();
                var input = json.createObjectNode().put("expectedVersion", 0).put("portOfLoadingId", loading.toString());
                input.putArray("producerIds").add(company.toString());
                assertThat(request(patch(route), input.toString(), "operator").getResponse().getStatus()).isEqualTo(200);
            } finally {
                writerCommitted.countDown();
            }
            var snapshot = reader.get(15, TimeUnit.SECONDS);
            assertThat(snapshot.path("version").asLong()).isZero();
            assertThat(snapshot.path("producerIds").isEmpty()).isTrue();
            assertThat(snapshot.path("portOfLoadingId").isNull()).isTrue();
        }
        var latest = body(mvc.perform(get(route).with(jwt().jwt(token -> token.subject("operator")))).andReturn());
        assertThat(latest.path("version").asLong()).isEqualTo(1);
        assertThat(latest.path("producerIds").size()).isEqualTo(1);
        assertThat(latest.path("portOfLoadingId").asText()).isEqualTo(loading.toString());
    }

    @Test
    void invalidCompanyPatchAndDuplicateReferenceRollBackAllChanges() throws Exception {
        var created = create("EXP-ORIGINAL");
        create("EXP-EXISTING");
        String route = "/api/projects/" + created.path("id").asText();
        long audits = count("audit_log");
        assertProblem(request(patch(route), "{\"expectedVersion\":0,\"exporterId\":\"" + UUID.randomUUID() + "\",\"notes\":\"bad\"}", "operator"), 422, "exporterId");
        assertProblem(request(patch(route), "{\"expectedVersion\":0,\"reference\":\"EXP-EXISTING\",\"portOfLoadingId\":\"" + loading + "\"}", "operator"), 409, "reference");
        assertThat(count("audit_log")).isEqualTo(audits);
        var fetched = body(mvc.perform(get(route).with(jwt().jwt(token -> token.subject("operator")))).andReturn());
        assertThat(fetched.path("reference").asText()).isEqualTo("EXP-ORIGINAL");
        assertThat(fetched.path("portOfLoadingId").isNull()).isTrue();
        assertThat(fetched.path("version").asLong()).isZero();
    }

    @Test
    void listsPagesWithStatusAndLiteralCaseInsensitivePrefix() throws Exception {
        create("EXP_A"); create("EXP_B"); create("EXPXA");
        var page = body(mvc.perform(get("/api/projects?page=0&size=1&status=DRAFT_DATA&q=exp_")
                .with(jwt().jwt(token -> token.subject("operator")))).andExpect(status().isOk()).andReturn());
        assertThat(page.path("totalElements").asLong()).isEqualTo(2);
        assertThat(page.path("totalPages").asInt()).isEqualTo(2);
        assertThat(page.path("items").size()).isEqualTo(1);
        var second = body(mvc.perform(get("/api/projects?page=1&size=1&q=EXP_").with(jwt().jwt(token -> token.subject("operator")))).andReturn());
        assertThat(page.path("items").get(0).path("id")).isNotEqualTo(second.path("items").get(0).path("id"));
        var literalPercent = body(mvc.perform(get("/api/projects").param("q", "%").with(jwt().jwt(token -> token.subject("operator")))).andReturn());
        assertThat(literalPercent.path("totalElements").asLong()).isZero();
        var differentStatus = body(mvc.perform(get("/api/projects?status=ARCHIVED").with(jwt().jwt(token -> token.subject("operator")))).andReturn());
        assertThat(differentStatus.path("items").isEmpty()).isTrue();
    }

    @Test
    void listingEmptyAndOutOfRangePagesUseTheContractEnvelope() throws Exception {
        var empty = body(mvc.perform(get("/api/projects").with(jwt().jwt(token -> token.subject("operator")))).andReturn());
        assertThat(empty.path("totalElements").asLong()).isZero();
        assertThat(empty.path("totalPages").asInt()).isZero();
        assertThat(empty.path("items").isEmpty()).isTrue();
        create("EXP-1");
        var later = body(mvc.perform(get("/api/projects?page=3&size=1").with(jwt().jwt(token -> token.subject("operator")))).andReturn());
        assertThat(later.path("items").isEmpty()).isTrue();
        assertThat(later.path("totalElements").asLong()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=word", "q=", "status=INVALID", "sort=id", "page=0&page=1"})
    void invalidListingParametersAreStructured400(String parameters) throws Exception {
        assertProblem(mvc.perform(get("/api/projects?" + parameters).with(jwt().jwt(token -> token.subject("operator")))).andReturn(), 400, null);
    }

    @Test
    void authenticationRolesAndAccessAreEnforcedBeforePagination() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        assertProblem(request(post("/api/projects"), "{\"reference\":\"EXP-1\"}", "viewer"), 403, null);
        var project = create("EXP-PRIVATE");
        String route = "/api/projects/" + project.path("id").asText();
        assertProblem(mvc.perform(get(route).with(jwt().jwt(token -> token.subject("viewer")))).andReturn(), 404, null);
        var invisible = body(mvc.perform(get("/api/projects").with(jwt().jwt(token -> token.subject("viewer")))).andReturn());
        assertThat(invisible.path("totalElements").asLong()).isZero();
        jdbc.update("INSERT INTO project_access(project_id,user_id) VALUES (?,?)", UUID.fromString(project.path("id").asText()), viewer);
        mvc.perform(get(route).with(jwt().jwt(token -> token.subject("viewer")))).andExpect(status().isOk());
        assertProblem(request(patch(route), "{\"expectedVersion\":0,\"notes\":\"blocked\"}", "viewer"), 403, null);
        mvc.perform(get(route).with(jwt().jwt(token -> token.subject("admin")))).andExpect(status().isOk());
        assertProblem(mvc.perform(get(route).with(jwt().jwt(token -> token.subject("unregistered")))).andReturn(), 403, null);
    }

    @Test
    void auditFailureRollsBackProjectAndReturnsSafeServerError() throws Exception {
        jdbc.execute("CREATE FUNCTION test_reject_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private test SQL detail'; END $$");
        jdbc.execute("CREATE TRIGGER test_reject_audit BEFORE INSERT ON audit_log FOR EACH ROW WHEN (NEW.entity_table='project') EXECUTE FUNCTION test_reject_audit()");
        try {
            var result = request(post("/api/projects"), "{\"reference\":\"EXP-ROLLBACK\"}", "operator");
            assertProblem(result, 500, null);
            assertThat(result.getResponse().getContentAsString()).doesNotContain("private test SQL detail", "SQLException", "stackTrace");
            assertThat(count("project")).isZero();
            assertThat(count("shipment_transport")).isZero();
            assertThat(projectAuditCount()).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER test_reject_audit ON audit_log");
            jdbc.execute("DROP FUNCTION test_reject_audit()");
        }
    }

    private MvcResult request(MockHttpServletRequestBuilder request, String body, String subject) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body)
                .with(jwt().jwt(token -> token.subject(subject)))).andReturn();
    }

    private JsonNode create(String reference) throws Exception {
        var result = request(post("/api/projects"), json.createObjectNode().put("reference", reference).toString(), "operator");
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return body(result);
    }

    private ObjectNode fullRequest(String reference) {
        var request = json.createObjectNode().put("reference", reference).put("exporterId", company.toString())
                .put("consigneeId", otherCompany.toString()).put("notifyPartyId", company.toString())
                .put("freightForwarderId", company.toString()).put("shippingLineId", company.toString())
                .put("destinationCountryCode", "FR").put("portOfLoadingId", loading.toString())
                .put("portOfDischargeId", discharge.toString()).put("expectedDepartureDate", "2000-01-01").put("notes", "Fictional test shipment");
        request.putArray("producerIds").add(company.toString());
        return request;
    }

    private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }

    private void assertProblem(MvcResult result, int statusCode, String field) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(statusCode);
        assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
        JsonNode problem = body(result);
        assertThat(problem.path("status").asInt()).isEqualTo(statusCode);
        assertThat(problem.path("type").asText()).isNotBlank();
        assertThat(problem.path("title").asText()).isNotBlank();
        assertThat(problem.path("errors").isArray()).isTrue();
        if (field != null) assertThat(problem.path("errors").findValuesAsText("field")).contains(field);
    }

    private UUID user(String subject, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,external_subject,display_name) VALUES (?,?,?)", id, subject, "Fictional " + subject);
        jdbc.update("INSERT INTO role_assignment(user_id,role) VALUES (?,?)", id, role);
        return id;
    }

    private UUID company(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO organization(id,legal_name,country_code) VALUES (?,?,'TN')", id, name);
        for (String role : List.of("EXPORTER", "PRODUCER", "CONSIGNEE", "NOTIFY_PARTY", "FREIGHT_FORWARDER", "SHIPPING_LINE")) {
            jdbc.update("INSERT INTO organization_role(organization_id,role) VALUES (?,?)", id, role);
        }
        return id;
    }

    private UUID port(String name, String country) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO port(id,name,country_code) VALUES (?,?,?)", id, name, country);
        return id;
    }

    private long count(String table) {
        if (!List.of("project", "shipment_transport", "audit_log").contains(table)) throw new IllegalArgumentException("Unsupported test table");
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private long projectAuditCount() { return jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entity_table='project'", Long.class); }
}

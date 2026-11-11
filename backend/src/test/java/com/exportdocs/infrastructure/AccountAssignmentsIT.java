package com.exportdocs.infrastructure;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
class AccountAssignmentsIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void localAccountsDoNotNeedAnExternalIdentity() {
        UUID user = user();
        account(user, "fictional.operator");
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM local_account WHERE user_id=?", Boolean.class, user)).isTrue();
        assertThat(jdbc.queryForObject("SELECT external_subject FROM app_user WHERE id=?", String.class, user)).isNull();
    }

    @Test
    void credentialsAreExcludedFromAuditHistoryButChangesRemainAudited() {
        UUID user = user();
        account(user, "fictional.operator");
        jdbc.update("UPDATE local_account SET password_hash='sensitive-replacement-fixture',credential_version=credential_version+1 WHERE user_id=?", user);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE entity_table='local_account'", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT coalesce(string_agg(coalesce(before_json::text,'')||coalesce(after_json::text,''),''),'') FROM audit_log WHERE entity_table='local_account'", String.class))
                .doesNotContain("sensitive-initial-fixture", "sensitive-replacement-fixture", "password_hash");
        assertThat(jdbc.queryForObject("SELECT (after_json->>'password_changed')::boolean FROM audit_log WHERE entity_table='local_account' AND operation='UPDATE'", Boolean.class)).isTrue();
    }

    @Test
    void loginNamesAreCanonicalAndUnique() {
        UUID first = user();
        account(first, "fictional.operator");
        assertThatThrownBy(() -> account(user(), "fictional.operator"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void formEditAssignmentRequiresMembershipInTheSameProject() {
        UUID operator = user();
        UUID admin = user();
        UUID project = project();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO project_form_assignment(project_id,user_id,form_key,assigned_by) VALUES (?,?,'PACKING_LIST',?)", project, operator, admin))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void projectReadAccessDoesNotGrantFormEditing() {
        UUID operator = user();
        UUID admin = user();
        UUID project = project();
        jdbc.update("INSERT INTO project_access(project_id,user_id) VALUES (?,?)", project, operator);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM project_form_assignment WHERE project_id=? AND user_id=?", Long.class, project, operator)).isZero();
        jdbc.update("INSERT INTO project_form_assignment(project_id,user_id,form_key,assigned_by) VALUES (?,?,'PACKING_LIST',?)", project, operator, admin);
        assertThat(jdbc.queryForList("SELECT form_key FROM project_form_assignment WHERE project_id=? AND user_id=?", String.class, project, operator)).containsExactly("PACKING_LIST");
        assertThat(jdbc.queryForObject("SELECT version FROM project WHERE id=?", Long.class, project)).isZero();
    }

    @Test
    void unknownFormKeysAreRejected() {
        UUID operator = user();
        UUID admin = user();
        UUID project = project();
        jdbc.update("INSERT INTO project_access(project_id,user_id) VALUES (?,?)", project, operator);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO project_form_assignment(project_id,user_id,form_key,assigned_by) VALUES (?,?,'ARBITRARY_FORM',?)", project, operator, admin))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,display_name) VALUES (?,'Fictional account')", id);
        return id;
    }

    private UUID project() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO project(id,reference) VALUES (?,?)", id, "FIXTURE-" + id.toString().toUpperCase(java.util.Locale.ROOT));
        return id;
    }

    private void account(UUID user, String username) {
        // Synthetic strings prove redaction; the application must supply PasswordEncoder hashes.
        jdbc.update("INSERT INTO local_account(user_id,username,password_hash) VALUES (?,?,'sensitive-initial-fixture')", user, username);
    }
}

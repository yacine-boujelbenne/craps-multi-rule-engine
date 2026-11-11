package com.exportdocs.account.application;

import com.exportdocs.account.api.AccountDtos;
import com.exportdocs.account.infrastructure.AccountStore;
import com.exportdocs.project.application.ProjectProblem;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {
    private final JdbcTemplate jdbc;
    private final AccountIdentity identity;
    private final AccountStore accounts;
    public AssignmentService(JdbcTemplate jdbc,AccountIdentity identity,AccountStore accounts) {
        this.jdbc=jdbc;this.identity=identity;this.accounts=accounts;
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<AccountDtos.Assignment> list(UUID project) {
        identity.admin();project(project,false);
        return jdbc.query("""
                SELECT a.user_id FROM project_access a JOIN role_assignment r ON r.user_id=a.user_id
                WHERE a.project_id=? AND r.role='OPERATOR' ORDER BY a.user_id
                """,(rs,row)->rs.getObject("user_id",UUID.class),project).stream()
                .map(id->new AccountDtos.Assignment(project,id,forms(project,id))).toList();
    }
    @Transactional
    public AccountDtos.Assignment assign(UUID project,UUID user,AccountDtos.Assign request) {
        accounts.serializeAdministration();
        var actor=identity.admin();project(project,true);
        var target=accounts.find(user,true).orElseThrow(()->new ProjectProblem(404,"Operator not found.",List.of()));
        if(!target.active() || !target.roles().contains(AccountRole.OPERATOR))
            throw ProjectProblem.field(400,"userId","invalid_operator","Target must be an active operator.");
        if(new HashSet<>(request.formKeys()).size()!=request.formKeys().size())
            throw ProjectProblem.field(400,"formKeys","duplicate","Form keys must be unique.");
        accounts.audit(actor.id());
        jdbc.update("INSERT INTO project_access(project_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING",project,user);
        var previous=forms(project,user);
        for(var form:previous) if(!request.formKeys().contains(form))
            jdbc.update("DELETE FROM project_form_assignment WHERE project_id=? AND user_id=? AND form_key=?",project,user,form.name());
        for(var form:request.formKeys()) if(!previous.contains(form))
            jdbc.update("INSERT INTO project_form_assignment(project_id,user_id,form_key,assigned_by) VALUES (?,?,?,?)",project,user,form.name(),actor.id());
        return new AccountDtos.Assignment(project,user,forms(project,user));
    }
    @Transactional
    public void remove(UUID project,UUID user) {
        accounts.serializeAdministration();
        var actor=identity.admin();project(project,true);
        accounts.audit(actor.id());
        jdbc.update("DELETE FROM project_form_assignment WHERE project_id=? AND user_id=?",project,user);
        jdbc.update("DELETE FROM project_access WHERE project_id=? AND user_id=?",project,user);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public AccountDtos.Permissions permissions(UUID project) {
        var actor=identity.ready();
        if(!actor.admin() && (!hasReadRole(actor) || !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM project_access WHERE project_id=? AND user_id=?)",Boolean.class,project,actor.id()))))
            throw ProjectProblem.notFound();
        project(project,false);
        var editable=actor.admin()?Arrays.asList(FormKey.values()):actor.roles().contains(AccountRole.OPERATOR)?forms(project,actor.id()):List.<FormKey>of();
        return new AccountDtos.Permissions(project,true,actor.admin(),editable);
    }
    private boolean hasReadRole(AccountRecord actor) {
        return actor.roles().stream().anyMatch(role->role==AccountRole.OPERATOR || role==AccountRole.REVIEWER || role==AccountRole.VIEWER);
    }
    private List<FormKey> forms(UUID project,UUID user) {
        return jdbc.query("SELECT form_key FROM project_form_assignment WHERE project_id=? AND user_id=? ORDER BY form_key",
                (rs,row)->FormKey.valueOf(rs.getString("form_key")),project,user);
    }
    private void project(UUID project,boolean lock) {
        var ids=jdbc.query("SELECT id FROM project WHERE id=?"+(lock?" FOR UPDATE":""),(rs,row)->rs.getObject("id",UUID.class),project);
        if(ids.isEmpty()) throw ProjectProblem.notFound();
    }
}

package com.exportdocs.account.infrastructure;

import com.exportdocs.account.application.AccountRecord;
import com.exportdocs.account.application.AccountRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountStore {
    private final JdbcTemplate jdbc;
    private static final String SELECT = """
            SELECT u.id,u.display_name,u.active,u.version,a.username,a.password_hash,a.must_change_password,a.credential_version
            FROM app_user u JOIN local_account a ON a.user_id=u.id
            """;

    public AccountStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<AccountRecord> find(UUID id, boolean lock) {
        return query(" WHERE u.id=?" + (lock ? " FOR UPDATE OF u,a" : ""), id).stream().findFirst();
    }
    public Optional<AccountRecord> username(String username) {
        return query(" WHERE a.username=?", username).stream().findFirst();
    }
    public List<AccountRecord> page(int size, long offset) { return query(" ORDER BY a.username,u.id LIMIT ? OFFSET ?", size, offset); }
    private List<AccountRecord> query(String suffix, Object... arguments) {
        // Load roles after closing the account result set on the same transaction connection.
        return jdbc.query(SELECT + suffix, (rs,row) -> new AccountRecord(rs.getObject("id",UUID.class),
                rs.getString("username"),rs.getString("display_name"),rs.getBoolean("active"),List.of(),
                rs.getBoolean("must_change_password"),rs.getLong("version"),rs.getString("password_hash"),rs.getLong("credential_version")), arguments)
                .stream().map(a -> new AccountRecord(a.id(),a.username(),a.displayName(),a.active(),roles(a.id()),
                        a.mustChangePassword(),a.version(),a.passwordHash(),a.credentialVersion())).toList();
    }
    public List<AccountRole> roles(UUID id) {
        return jdbc.query("SELECT role FROM role_assignment WHERE user_id=? ORDER BY role",
                (rs,row) -> AccountRole.valueOf(rs.getString("role")), id);
    }
    public long count() { return jdbc.queryForObject("SELECT count(*) FROM local_account", Long.class); }
    public void serializeAdministration() { jdbc.execute("SELECT pg_advisory_xact_lock(1667330900)"); }
    public long activeAdmins() {
        return jdbc.queryForObject("SELECT count(*) FROM app_user u JOIN local_account a ON a.user_id=u.id JOIN role_assignment r ON r.user_id=u.id WHERE u.active AND r.role='ADMIN'", Long.class);
    }
    public void audit(UUID id) {
        jdbc.queryForObject("SELECT set_config('app.actor_id',?,true)",String.class,id.toString());
        jdbc.queryForObject("SELECT set_config('app.request_id',?,true)",String.class,UUID.randomUUID().toString());
    }
    public void insert(UUID id,String username,String name,String hash,List<AccountRole> roles) {
        jdbc.update("INSERT INTO app_user(id,display_name) VALUES (?,?)",id,name);
        jdbc.update("INSERT INTO local_account(user_id,username,password_hash) VALUES (?,?,?)",id,username,hash);
        replaceRoles(id,List.of(),roles);
    }
    public void update(UUID id,String username,String name,boolean active,boolean revokeCredentials) {
        jdbc.update("UPDATE app_user SET display_name=?,active=?,version=version+1 WHERE id=?",name,active,id);
        if (revokeCredentials) jdbc.update("UPDATE local_account SET username=?,credential_version=credential_version+1 WHERE user_id=?",username,id);
    }
    public void replaceRoles(UUID id,List<AccountRole> previous,List<AccountRole> next) {
        for (AccountRole role:previous) if (!next.contains(role)) jdbc.update("DELETE FROM role_assignment WHERE user_id=? AND role=?",id,role.name());
        for (AccountRole role:next) if (!previous.contains(role)) jdbc.update("INSERT INTO role_assignment(user_id,role) VALUES (?,?)",id,role.name());
    }
    public void password(UUID id,String hash,boolean temporary) {
        jdbc.update("UPDATE local_account SET password_hash=?,must_change_password=?,credential_version=credential_version+1,password_changed_at=now() WHERE user_id=?",hash,temporary,id);
        jdbc.update("UPDATE app_user SET version=version+1 WHERE id=?",id);
    }
}

package com.exportdocs.account.application;

import com.exportdocs.account.api.AccountDtos;
import com.exportdocs.account.api.AccountMapper;
import com.exportdocs.account.infrastructure.AccountStore;
import com.exportdocs.project.application.ProjectProblem;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final AccountStore accounts;
    private final AccountIdentity identity;
    private final PasswordEncoder encoder;
    private final AccountMapper mapper;
    private final String dummyHash;

    public AccountService(AccountStore accounts,AccountIdentity identity,PasswordEncoder encoder,AccountMapper mapper) {
        this.accounts=accounts;this.identity=identity;this.encoder=encoder;this.mapper=mapper;
        dummyHash=encoder.encode(UUID.randomUUID().toString());
    }
    @Transactional(readOnly=true)
    public AccountRecord authenticate(AccountDtos.Login request) {
        var account=accounts.username(request.username());
        boolean matches=matches(request.password(),account.map(AccountRecord::passwordHash).orElse(dummyHash));
        if(!matches || account.isEmpty() || !account.get().active())
            throw new ProjectProblem(401,"Invalid username or password.",List.of());
        return account.get();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public AccountDtos.User me() { return mapper.user(identity.current()); }
    @Transactional
    public AccountDtos.User create(AccountDtos.Create request) {
        accounts.serializeAdministration();
        var actor=identity.admin();
        accounts.audit(actor.id());
        return createAccount(request);
    }
    @Transactional
    public AccountDtos.User bootstrap(AccountDtos.Create request) {
        accounts.serializeAdministration();
        if(accounts.activeAdmins()!=0) throw new IllegalStateException("An active administrator already exists; bootstrap refused.");
        if(!request.roles().equals(List.of(AccountRole.ADMIN))) throw new IllegalArgumentException("Bootstrap requires ADMIN only.");
        return createAccount(request);
    }
    private AccountDtos.User createAccount(AccountDtos.Create request) {
        PasswordPolicy.validate(request.initialPassword(),"initialPassword");
        unique(request.roles(),"roles");
        UUID id=UUID.randomUUID();
        try { accounts.insert(id,request.username(),request.displayName(),encoder.encode(request.initialPassword()),request.roles()); }
        catch(DuplicateKeyException exception) { throw duplicateUsername(); }
        return mapper.user(find(id,false));
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public AccountDtos.User get(UUID id) { identity.admin();return mapper.user(find(id,false)); }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public AccountDtos.UserPage list(int page,int size) {
        identity.admin();
        if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE)
            throw ProjectProblem.field(400,"page","invalid","Use a nonnegative page and a size from 1 to 100.");
        long total=accounts.count();
        return new AccountDtos.UserPage(accounts.page(size,(long)page*size).stream().map(mapper::user).toList(),page,size,total,(int)((total+size-1)/size));
    }
    @Transactional
    public AccountDtos.User update(UUID id,AccountDtos.Update request,Set<String> supplied) {
        accounts.serializeAdministration();
        var actor=identity.admin();
        var current=find(id,true);
        version(current,request.expectedVersion());
        String username=supplied.contains("username")?request.username():current.username();
        String name=supplied.contains("displayName")?request.displayName():current.displayName();
        boolean active=supplied.contains("active")?request.active():current.active();
        var roles=supplied.contains("roles")?request.roles():current.roles();
        unique(roles,"roles");
        if(name.isBlank()) throw ProjectProblem.field(400,"displayName","invalid","Display name must not be blank.");
        if(current.active() && current.admin() && (!active || !roles.contains(AccountRole.ADMIN)) && accounts.activeAdmins()<=1)
            throw ProjectProblem.field(409,"roles","last_administrator","The last active administrator must be retained.");
        if(Objects.equals(username,current.username()) && Objects.equals(name,current.displayName()) && active==current.active() && new HashSet<>(roles).equals(new HashSet<>(current.roles())))
            return mapper.user(current);
        accounts.audit(actor.id());
        try { accounts.update(id,username,name,active,!username.equals(current.username()) || active!=current.active()); }
        catch(DuplicateKeyException exception) { throw duplicateUsername(); }
        accounts.replaceRoles(id,current.roles(),roles);
        return mapper.user(find(id,false));
    }
    @Transactional
    public AccountDtos.User reset(UUID id,AccountDtos.Reset request) {
        accounts.serializeAdministration();
        var actor=identity.admin();
        var current=find(id,true);
        version(current,request.expectedVersion());
        PasswordPolicy.validate(request.temporaryPassword(),"temporaryPassword");
        accounts.audit(actor.id());
        accounts.password(id,encoder.encode(request.temporaryPassword()),true);
        return mapper.user(find(id,false));
    }
    @Transactional
    public void changePassword(AccountDtos.ChangePassword request) {
        var actor=identity.current();
        var current=find(actor.id(),true);
        if(!current.active() || current.credentialVersion()!=actor.credentialVersion())
            throw new ProjectProblem(401,"Session is no longer valid.",List.of());
        if(!matches(request.currentPassword(),current.passwordHash()))
            throw new ProjectProblem(401,"Current password is incorrect.",List.of());
        PasswordPolicy.validate(request.newPassword(),"newPassword");
        if(matches(request.newPassword(),current.passwordHash()))
            throw ProjectProblem.field(400,"newPassword","password_unchanged","Choose a password different from the current password.");
        accounts.audit(actor.id());
        accounts.password(actor.id(),encoder.encode(request.newPassword()),false);
    }
    private AccountRecord find(UUID id,boolean lock) {
        return accounts.find(id,lock).orElseThrow(()->new ProjectProblem(404,"Account not found.",List.of()));
    }
    private void version(AccountRecord account,long expected) {
        if(account.version()!=expected) throw ProjectProblem.field(409,"expectedVersion","stale_version","Account changed; reload before updating.");
    }
    private void unique(List<?> values,String field) {
        if(new HashSet<>(values).size()!=values.size()) throw ProjectProblem.field(400,field,"duplicate","Values must be unique.");
    }
    private ProjectProblem duplicateUsername() {
        return ProjectProblem.field(409,"username","duplicate_username","Username already exists.");
    }
    private boolean matches(String password,String hash) {
        return password!=null && password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72 && encoder.matches(password,hash);
    }
}

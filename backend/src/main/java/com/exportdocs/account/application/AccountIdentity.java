package com.exportdocs.account.application;

import com.exportdocs.account.infrastructure.AccountStore;
import com.exportdocs.project.application.ProjectProblem;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccountIdentity {
    private final AccountStore accounts;
    public AccountIdentity(AccountStore accounts) { this.accounts=accounts; }
    public AccountRecord current() {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AccountPrincipal principal))
            throw new ProjectProblem(401,"Authentication required.",List.of());
        return validate(principal);
    }
    public AccountRecord validate(AccountPrincipal principal) {
        var account=accounts.find(principal.userId(),false).orElseThrow(()->new ProjectProblem(401,"Session is no longer valid.",List.of()));
        if(!account.active() || account.credentialVersion()!=principal.credentialVersion())
            throw new ProjectProblem(401,"Session is no longer valid.",List.of());
        return account;
    }
    public AccountRecord ready() {
        var account=current();
        if(account.mustChangePassword()) throw new ProjectProblem(403,"Change your temporary password before continuing.",List.of());
        return account;
    }
    public AccountRecord admin() {
        var account=ready();
        if(!account.admin()) throw new ProjectProblem(403,"Administrator permission required.",List.of());
        return account;
    }
}

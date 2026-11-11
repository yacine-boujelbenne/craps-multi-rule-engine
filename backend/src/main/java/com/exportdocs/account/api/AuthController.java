package com.exportdocs.account.api;

import com.exportdocs.account.application.AccountPrincipal;
import com.exportdocs.account.application.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import java.util.List;

@RestController
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accounts;
    private final AccountMapper mapper;
    private final SecurityContextRepository contexts;
    private final CookieCsrfTokenRepository csrf;
    public AuthController(AccountService accounts,AccountMapper mapper,SecurityContextRepository contexts,CookieCsrfTokenRepository csrf) {
        this.accounts=accounts;this.mapper=mapper;this.contexts=contexts;this.csrf=csrf;
    }
    @GetMapping("/csrf") public ResponseEntity<Void> csrf(HttpServletRequest request) {
        ((CsrfToken)request.getAttribute(CsrfToken.class.getName())).getToken();
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/login") public AccountDtos.User login(@Valid @RequestBody AccountDtos.Login body,HttpServletRequest request,HttpServletResponse response) {
        var account=accounts.authenticate(body);
        var authentication=UsernamePasswordAuthenticationToken.authenticated(new AccountPrincipal(account.id(),account.credentialVersion()),null,List.of());
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication,request,response);
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);contexts.saveContext(context,request,response);
        csrf.saveToken(null,request,response);
        return mapper.user(account);
    }
    @GetMapping("/me") public AccountDtos.User me() { return accounts.me(); }
    @PostMapping("/password") public ResponseEntity<Void> password(@Valid @RequestBody AccountDtos.ChangePassword body,HttpServletRequest request,HttpServletResponse response) {
        accounts.changePassword(body);clear(request,response);return ResponseEntity.noContent().build();
    }
    @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest request,HttpServletResponse response) {
        clear(request,response);return ResponseEntity.noContent().build();
    }
    private void clear(HttpServletRequest request,HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request,response,SecurityContextHolder.getContext().getAuthentication());
        csrf.saveToken(null,request,response);
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("JSESSIONID","").httpOnly(true).secure(request.isSecure())
                .sameSite("Lax").path(request.getContextPath().isEmpty()?"/":request.getContextPath()).maxAge(0).build().toString());
    }
}

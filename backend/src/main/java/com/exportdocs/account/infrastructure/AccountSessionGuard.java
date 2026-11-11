package com.exportdocs.account.infrastructure;

import com.exportdocs.account.application.AccountIdentity;
import com.exportdocs.account.application.AccountPrincipal;
import com.exportdocs.project.api.ProjectErrorHandler;
import com.exportdocs.project.application.ProjectProblem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class AccountSessionGuard extends OncePerRequestFilter {
    private static final Logger LOG=LoggerFactory.getLogger(AccountSessionGuard.class);
    private static final Set<String> PUBLIC=Set.of("/api/auth/csrf","/api/auth/login","/api/auth/logout");
    private static final Set<String> PASSWORD_ROUTES=Set.of("/api/auth/csrf","/api/auth/me","/api/auth/password","/api/auth/logout","/api/auth/login");
    private final AccountIdentity identity;
    private final ObjectMapper json;
    AccountSessionGuard(AccountIdentity identity,ObjectMapper json) { this.identity=identity;this.json=json; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(path.startsWith("/api/") && authentication!=null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            try {
                var account=identity.validate(principal);
                if(account.mustChangePassword() && !PASSWORD_ROUTES.contains(path)) {
                    problem(response,403,"Change your temporary password before continuing.",path);return;
                }
            } catch(ProjectProblem exception) {
                var session=request.getSession(false);
                if(session!=null) session.invalidate();
                SecurityContextHolder.clearContext();
                if(!PUBLIC.contains(path)) { problem(response,401,"Session is no longer valid.",path);return; }
            } catch(RuntimeException exception) {
                LOG.error("Session validation failed",exception);
                problem(response,500,"The request could not be completed.",path);return;
            }
        }
        chain.doFilter(request,response);
    }
    private void problem(HttpServletResponse response,int status,String message,String path) throws IOException {
        response.setStatus(status);response.setContentType("application/problem+json");
        json.writeValue(response.getOutputStream(),ProjectErrorHandler.problem(status,message,List.of(),path));
    }
}

package com.exportdocs.account.infrastructure;

import com.exportdocs.account.application.AccountIdentity;
import com.exportdocs.project.api.ProjectErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration(proxyBeanMethods=false)
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class AccountSecurityConfiguration {
    @Bean SecurityContextRepository accountContexts() { return new HttpSessionSecurityContextRepository(); }
    @Bean CookieCsrfTokenRepository accountCsrfTokens() {
        var repository=CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie->cookie.sameSite("Lax"));
        return repository;
    }
    @Bean SecurityFilterChain accountSecurity(HttpSecurity http,ObjectMapper json,AccountIdentity identity,
            SecurityContextRepository contexts,CookieCsrfTokenRepository csrfTokens) throws Exception {
        var entryPoint=(org.springframework.security.web.AuthenticationEntryPoint)(request,response,exception)->{
            response.setStatus(401);response.setContentType("application/problem+json");
            json.writeValue(response.getOutputStream(),ProjectErrorHandler.problem(401,"Authentication required.",List.of(),request.getRequestURI()));
        };
        var denied=(org.springframework.security.web.access.AccessDeniedHandler)(request,response,exception)->{
            response.setStatus(403);response.setContentType("application/problem+json");
            json.writeValue(response.getOutputStream(),ProjectErrorHandler.problem(403,"Access denied or CSRF token invalid.",List.of(),request.getRequestURI()));
        };
        return http.securityContext(context->context.requireExplicitSave(true).securityContextRepository(contexts))
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf->csrf.csrfTokenRepository(csrfTokens).csrfTokenRequestHandler(new SpaCsrfRequestHandler()))
                .formLogin(login->login.disable()).httpBasic(basic->basic.disable()).logout(logout->logout.disable())
                .authorizeHttpRequests(requests->requests
                        .requestMatchers("/actuator/health","/actuator/health/**","/api/auth/csrf","/api/auth/login","/api/auth/logout").permitAll()
                        .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
                .exceptionHandling(errors->errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .addFilterBefore(new AccountSessionGuard(identity,json),AuthorizationFilter.class).build();
    }
}

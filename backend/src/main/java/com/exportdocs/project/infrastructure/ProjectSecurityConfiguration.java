package com.exportdocs.project.infrastructure;

import com.exportdocs.project.api.ProjectErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class ProjectSecurityConfiguration {
    @Bean
    JwtDecoder projectJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwks,
            @Value("${exportdocs.security.audience:exportdocs-api}") String audience) {
        // An unconfigured installation can expose health without accepting any business token.
        if (issuer.isBlank()) return token -> { throw new BadJwtException("JWT issuer is not configured."); };
        NimbusJwtDecoder decoder = jwks.isBlank() ? JwtDecoders.fromIssuerLocation(issuer)
                : NimbusJwtDecoder.withJwkSetUri(jwks).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience.", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceValidator));
        return decoder;
    }

    @Bean
    SecurityFilterChain projectSecurity(HttpSecurity http, ObjectMapper json) throws Exception {
        var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, exception) -> {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType("application/problem+json");
            json.writeValue(response.getOutputStream(), ProjectErrorHandler.problem(401, "Authentication required.", List.of(), request.getRequestURI()));
        };
        var denied = (org.springframework.security.web.access.AccessDeniedHandler) (request, response, exception) -> {
            response.setStatus(403);
            response.setContentType("application/problem+json");
            json.writeValue(response.getOutputStream(), ProjectErrorHandler.problem(403, "Access denied.", List.of(), request.getRequestURI()));
        };
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/projects", "/api/projects/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> {}).authenticationEntryPoint(entryPoint).accessDeniedHandler(denied))
                .build();
    }
}

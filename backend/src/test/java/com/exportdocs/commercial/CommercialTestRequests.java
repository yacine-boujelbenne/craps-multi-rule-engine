package com.exportdocs.commercial;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class CommercialTestRequests {
    private CommercialTestRequests() {}

    static MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String body) {
        // Match the SPA transport without injecting a form parameter or replacing the configured CSRF repository.
        var token = CookieCsrfTokenRepository.withHttpOnlyFalse().generateToken(new MockHttpServletRequest());
        return request.cookie(new Cookie("XSRF-TOKEN", token.getToken()))
                .header(token.getHeaderName(), token.getToken())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }
}

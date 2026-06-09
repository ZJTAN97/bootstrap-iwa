package com.iwa.products.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

public class VeridiumPreAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

    static final String PRINCIPAL_HEADER = "X-Authenticated-User";

    public VeridiumPreAuthFilter() {
        setAuthenticationDetailsSource(VeridiumAuthDetails::new);
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        return request.getHeader(PRINCIPAL_HEADER);
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "N/A";
    }
}

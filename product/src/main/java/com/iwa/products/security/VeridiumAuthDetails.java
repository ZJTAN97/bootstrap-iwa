package com.iwa.products.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

class VeridiumAuthDetails extends WebAuthenticationDetails {

    static final String ROLES_HEADER = "X-User-Roles";

    private final List<GrantedAuthority> authorities;

    VeridiumAuthDetails(HttpServletRequest request) {
        super(request);
        String rolesHeader = request.getHeader(ROLES_HEADER);
        authorities = rolesHeader != null && !rolesHeader.isBlank()
                ? Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                        .toList()
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public Collection<? extends GrantedAuthority> getGrantedAuthorities() {
        return authorities;
    }
}

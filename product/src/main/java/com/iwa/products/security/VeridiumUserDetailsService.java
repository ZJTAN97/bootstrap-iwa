package com.iwa.products.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
class VeridiumUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        String samAccountName = (String) token.getPrincipal();

        if (samAccountName == null || samAccountName.isBlank()) {
            throw new UsernameNotFoundException("No samAccountName in pre-auth token");
        }

        Collection<? extends GrantedAuthority> authorities =
                token.getDetails() instanceof VeridiumAuthDetails details ? details.getGrantedAuthorities() : List.of();

        return User.withUsername(samAccountName)
                .password("")
                .authorities(authorities)
                .build();
    }
}

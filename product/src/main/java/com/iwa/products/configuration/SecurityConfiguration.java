package com.iwa.products.configuration;

import com.iwa.products.security.VeridiumPreAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration {

    private final AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService;

    SecurityConfiguration(
            AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(veridiumPreAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
                .build();
    }

    @Bean
    VeridiumPreAuthFilter veridiumPreAuthFilter() {
        VeridiumPreAuthFilter filter = new VeridiumPreAuthFilter();
        filter.setAuthenticationManager(authenticationManager());
        filter.setCheckForPrincipalChanges(true);
        return filter;
    }

    @Bean
    AuthenticationManager authenticationManager() {
        return new ProviderManager(preAuthProvider());
    }

    @Bean
    PreAuthenticatedAuthenticationProvider preAuthProvider() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(userDetailsService);
        return provider;
    }
}

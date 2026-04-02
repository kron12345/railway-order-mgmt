package com.ordermgmt.railway.infrastructure.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import com.vaadin.flow.spring.security.VaadinWebSecurity;

/** Configures Vaadin security and maps Keycloak roles to authorities. */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        setOAuth2LoginPage(http, "/login");
    }

    @Override
    protected void configure(WebSecurity web) throws Exception {
        super.configure(web);
        // SECURITY NOTE: API endpoints are excluded from Spring Security for the
        // Path Manager simulation tool. In production, these should be secured
        // with a separate SecurityFilterChain using Bearer token authentication.
        web.ignoring()
                .requestMatchers("/api/v1/pathmanager/**")
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**");
    }

    /** Maps Keycloak realm roles from the OIDC token to Spring Security GrantedAuthorities. */
    @Bean
    public GrantedAuthoritiesMapper keycloakGrantedAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            for (GrantedAuthority authority : authorities) {
                mappedAuthorities.add(authority);

                if (authority instanceof OidcUserAuthority oidcAuthority) {
                    Map<String, Object> claims = oidcAuthority.getIdToken().getClaims();
                    extractKeycloakRoles(claims)
                            .forEach(
                                    role ->
                                            mappedAuthorities.add(
                                                    new SimpleGrantedAuthority(
                                                            "ROLE_" + role.toUpperCase())));
                }
            }

            return mappedAuthorities;
        };
    }

    @SuppressWarnings("unchecked")
    private Collection<String> extractKeycloakRoles(Map<String, Object> claims) {
        Set<String> roles = new HashSet<>();

        // Realm roles
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        if (realmAccess != null) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                roles.addAll(realmRoles);
            }
        }

        // Client roles
        Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
        if (resourceAccess != null) {
            Map<String, Object> clientAccess =
                    (Map<String, Object>) resourceAccess.get("order-mgmt");
            if (clientAccess != null) {
                List<String> clientRoles = (List<String>) clientAccess.get("roles");
                if (clientRoles != null) {
                    roles.addAll(clientRoles);
                }
            }
        }

        return roles;
    }
}

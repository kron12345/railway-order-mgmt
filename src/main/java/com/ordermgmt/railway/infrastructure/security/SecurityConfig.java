package com.ordermgmt.railway.infrastructure.security;

import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
        );

        setOAuth2LoginPage(http, "/login");
    }

    /**
     * Maps Keycloak realm roles from the OIDC token to Spring Security GrantedAuthorities.
     */
    @Bean
    public GrantedAuthoritiesMapper keycloakGrantedAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            for (GrantedAuthority authority : authorities) {
                mappedAuthorities.add(authority);

                if (authority instanceof OidcUserAuthority oidcAuthority) {
                    Map<String, Object> claims = oidcAuthority.getIdToken().getClaims();
                    extractKeycloakRoles(claims).forEach(role ->
                            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    );
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
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("order-mgmt");
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

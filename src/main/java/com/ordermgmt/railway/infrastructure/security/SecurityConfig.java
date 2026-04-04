package com.ordermgmt.railway.infrastructure.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.spring.security.VaadinWebSecurity;

/** Configures Vaadin security and maps Keycloak roles to authorities. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig extends VaadinWebSecurity {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.api.security-enabled:false}")
    private boolean apiSecurityEnabled;

    /** Logs a warning at startup when API security is disabled (dev/demo mode). */
    @EventListener(ApplicationReadyEvent.class)
    public void logApiSecurityStatus() {
        if (!apiSecurityEnabled) {
            log.warn(
                    "API security is DISABLED (app.api.security-enabled=false). "
                            + "All /api/** endpoints are open. "
                            + "Set app.api.security-enabled=true for production!");
        }
    }

    /**
     * Separate filter chain for API and Swagger endpoints. Uses stateless sessions and no CSRF so
     * REST clients work without a Vaadin session. Takes precedence over the Vaadin filter chain.
     *
     * <p>When {@code app.api.security-enabled=true}, API endpoints require a valid JWT Bearer
     * token. Swagger UI and OpenAPI docs remain publicly accessible. When disabled (default for
     * dev/demo), all endpoints are open. See ADR-010 for details.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**", "/swagger-ui/**", "/v3/api-docs/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> {
                            auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                            if (apiSecurityEnabled) {
                                auth.requestMatchers("/api/v1/pathmanager/**")
                                        .hasAnyRole("ADMIN", "DISPATCHER");
                                auth.anyRequest().authenticated();
                            } else {
                                auth.anyRequest().permitAll();
                            }
                        })
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        if (apiSecurityEnabled) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        }
        return http.build();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        setOAuth2LoginPage(http, "/login");
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

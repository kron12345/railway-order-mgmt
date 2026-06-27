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
    private static final String API_PATTERN = "/api/**";
    private static final String PATH_MANAGER_API_PATTERN = "/api/v1/pathmanager/**";
    private static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
    private static final String OPEN_API_DOCS_PATTERN = "/v3/api-docs/**";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String DISPATCHER_ROLE = "DISPATCHER";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ORDER_MGMT_CLIENT = "order-mgmt";
    private static final String ROLES_CLAIM = "roles";

    @Value("${app.api.security-enabled:false}")
    private boolean apiSecurityEnabled;

    /** Logs a warning at startup when API security is disabled (dev/demo mode). */
    @EventListener(ApplicationReadyEvent.class)
    public void logApiSecurityStatus() {
        if (apiSecurityEnabled) {
            return;
        }

        log.warn(
                "API security is DISABLED (app.api.security-enabled=false). "
                        + "All /api/** endpoints are open. "
                        + "Set app.api.security-enabled=true for production!");
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
        http.securityMatcher(API_PATTERN, SWAGGER_UI_PATTERN, OPEN_API_DOCS_PATTERN)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> {
                            auth.requestMatchers(SWAGGER_UI_PATTERN, OPEN_API_DOCS_PATTERN)
                                    .permitAll();
                            if (apiSecurityEnabled) {
                                auth.requestMatchers(PATH_MANAGER_API_PATTERN)
                                        .hasAnyRole(ADMIN_ROLE, DISPATCHER_ROLE);
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
        return this::mapKeycloakAuthorities;
    }

    private Set<GrantedAuthority> mapKeycloakAuthorities(
            Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

        for (GrantedAuthority authority : authorities) {
            mappedAuthorities.add(authority);
            if (authority instanceof OidcUserAuthority oidcAuthority) {
                addKeycloakRoles(mappedAuthorities, oidcAuthority);
            }
        }

        return mappedAuthorities;
    }

    private void addKeycloakRoles(
            Set<GrantedAuthority> mappedAuthorities, OidcUserAuthority oidcAuthority) {
        Map<String, Object> claims = oidcAuthority.getIdToken().getClaims();
        extractKeycloakRoles(claims)
                .forEach(
                        role ->
                                mappedAuthorities.add(
                                        new SimpleGrantedAuthority(
                                                ROLE_PREFIX + role.toUpperCase())));
    }

    private Collection<String> extractKeycloakRoles(Map<String, Object> claims) {
        Set<String> roles = new HashSet<>();
        addRealmRoles(claims, roles);
        addClientRoles(claims, roles);
        return roles;
    }

    @SuppressWarnings("unchecked")
    private void addRealmRoles(Map<String, Object> claims, Set<String> roles) {
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get(REALM_ACCESS_CLAIM);
        if (realmAccess != null) {
            List<String> realmRoles = (List<String>) realmAccess.get(ROLES_CLAIM);
            if (realmRoles != null) {
                roles.addAll(realmRoles);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addClientRoles(Map<String, Object> claims, Set<String> roles) {
        Map<String, Object> resourceAccess =
                (Map<String, Object>) claims.get(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess != null) {
            Map<String, Object> clientAccess =
                    (Map<String, Object>) resourceAccess.get(ORDER_MGMT_CLIENT);
            if (clientAccess != null) {
                List<String> clientRoles = (List<String>) clientAccess.get(ROLES_CLAIM);
                if (clientRoles != null) {
                    roles.addAll(clientRoles);
                }
            }
        }
    }
}

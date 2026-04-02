package com.ordermgmt.railway.infrastructure.security;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** Resolves the current auditor from the Spring Security context for JPA auditing. */
@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(
                        auth -> {
                            if (auth.getPrincipal() instanceof OidcUser oidcUser) {
                                return oidcUser.getPreferredUsername();
                            }
                            return auth.getName();
                        });
    }
}

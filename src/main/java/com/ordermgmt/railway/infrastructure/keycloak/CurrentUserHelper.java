package com.ordermgmt.railway.infrastructure.keycloak;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Extracts current user info from the Spring Security OIDC context. */
public final class CurrentUserHelper {

    private CurrentUserHelper() {}

    /**
     * Whether the current user holds at least one of the given realm roles. Names may be passed
     * with or without the {@code ROLE_} prefix (Spring maps Keycloak realm roles to {@code
     * ROLE_<UPPER>}). Used to hide mutation controls in the UI; the service layer enforces the same
     * rule via {@code @PreAuthorize}, so this is defence-in-depth, not the only gate.
     */
    public static boolean hasAnyRole(String... roles) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (String role : roles) {
            String requiredAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if (requiredAuthority.equals(authority.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static OidcUser getOidcUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser;
        }
        return null;
    }

    public static String getUserId() {
        OidcUser user = getOidcUser();
        return user != null ? user.getSubject() : null;
    }

    public static String getUsername() {
        OidcUser user = getOidcUser();
        return user != null ? user.getPreferredUsername() : null;
    }

    public static String getFullName() {
        OidcUser user = getOidcUser();
        return user != null ? user.getFullName() : null;
    }

    public static String getEmail() {
        OidcUser user = getOidcUser();
        return user != null ? user.getEmail() : null;
    }
}

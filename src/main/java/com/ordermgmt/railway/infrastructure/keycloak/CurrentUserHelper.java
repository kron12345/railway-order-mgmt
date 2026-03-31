package com.ordermgmt.railway.infrastructure.keycloak;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Extracts current user info from the Spring Security OIDC context. */
public final class CurrentUserHelper {

    private CurrentUserHelper() {}

    public static OidcUser getOidcUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
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

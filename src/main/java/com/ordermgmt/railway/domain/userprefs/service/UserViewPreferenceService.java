package com.ordermgmt.railway.domain.userprefs.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.userprefs.model.UserViewPreference;
import com.ordermgmt.railway.domain.userprefs.repository.UserViewPreferenceRepository;

/**
 * Loads and stores per-user JSON payloads keyed by a {@code viewKey}. Resolves the
 * current user from the Spring Security context (Keycloak {@code preferred_username}),
 * falling back to {@code Authentication.getName()}.
 */
@Service
public class UserViewPreferenceService {

    private final UserViewPreferenceRepository repository;

    public UserViewPreferenceService(UserViewPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<String> loadJson(String viewKey) {
        return currentUserId().flatMap(uid -> repository.findByUserIdAndViewKey(uid, viewKey))
                .map(UserViewPreference::getPayload);
    }

    @Transactional
    public void saveJson(String viewKey, String payloadJson) {
        currentUserId().ifPresent(uid -> {
            UserViewPreference pref = repository.findByUserIdAndViewKey(uid, viewKey)
                    .orElseGet(() -> {
                        UserViewPreference fresh = new UserViewPreference();
                        fresh.setUserId(uid);
                        fresh.setViewKey(viewKey);
                        return fresh;
                    });
            pref.setPayload(payloadJson);
            pref.setUpdatedAt(Instant.now());
            repository.save(pref);
        });
    }

    @Transactional
    public void delete(String viewKey) {
        currentUserId().ifPresent(uid -> repository.deleteByUserIdAndViewKey(uid, viewKey));
    }

    private Optional<String> currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof OidcUser oidc) {
            return Optional.ofNullable(oidc.getPreferredUsername())
                    .or(() -> Optional.ofNullable(oidc.getSubject()));
        }
        return Optional.ofNullable(auth.getName());
    }
}

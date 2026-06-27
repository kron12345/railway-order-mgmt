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
 * Loads and stores per-user JSON payloads keyed by a {@code viewKey}. Resolves the current user
 * from the Spring Security context (Keycloak {@code preferred_username}), falling back to {@code
 * Authentication.getName()}.
 */
@Service
public class UserViewPreferenceService {

    private final UserViewPreferenceRepository userViewPreferenceRepository;

    public UserViewPreferenceService(UserViewPreferenceRepository userViewPreferenceRepository) {
        this.userViewPreferenceRepository = userViewPreferenceRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> loadJson(String viewKey) {
        return currentAuthenticatedUserId()
                .flatMap(
                        userId ->
                                userViewPreferenceRepository.findByUserIdAndViewKey(
                                        userId, viewKey))
                .map(UserViewPreference::getPayload);
    }

    @Transactional
    public void saveJson(String viewKey, String payloadJson) {
        currentAuthenticatedUserId()
                .ifPresent(userId -> saveJsonForUser(userId, viewKey, payloadJson));
    }

    @Transactional
    public void delete(String viewKey) {
        currentAuthenticatedUserId().ifPresent(userId -> deleteForUser(userId, viewKey));
    }

    private void saveJsonForUser(String userId, String viewKey, String payloadJson) {
        var preference = findOrCreatePreference(userId, viewKey);
        preference.setPayload(payloadJson);
        preference.setUpdatedAt(Instant.now());
        userViewPreferenceRepository.save(preference);
    }

    private void deleteForUser(String userId, String viewKey) {
        userViewPreferenceRepository.deleteByUserIdAndViewKey(userId, viewKey);
    }

    private UserViewPreference findOrCreatePreference(String userId, String viewKey) {
        return userViewPreferenceRepository
                .findByUserIdAndViewKey(userId, viewKey)
                .orElseGet(() -> newPreference(userId, viewKey));
    }

    private UserViewPreference newPreference(String userId, String viewKey) {
        var preference = new UserViewPreference();
        preference.setUserId(userId);
        preference.setViewKey(viewKey);
        return preference;
    }

    private Optional<String> currentAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return Optional.ofNullable(oidcUser.getPreferredUsername())
                    .or(() -> Optional.ofNullable(oidcUser.getSubject()));
        }

        return Optional.ofNullable(authentication.getName());
    }
}

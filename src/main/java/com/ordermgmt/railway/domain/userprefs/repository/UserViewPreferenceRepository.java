package com.ordermgmt.railway.domain.userprefs.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ordermgmt.railway.domain.userprefs.model.UserViewPreference;

public interface UserViewPreferenceRepository extends JpaRepository<UserViewPreference, UUID> {

    Optional<UserViewPreference> findByUserIdAndViewKey(String userId, String viewKey);

    void deleteByUserIdAndViewKey(String userId, String viewKey);
}

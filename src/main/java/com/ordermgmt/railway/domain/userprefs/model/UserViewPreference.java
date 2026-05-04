package com.ordermgmt.railway.domain.userprefs.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Generic per-user persistence record for any view's UI state — grid column layouts,
 * splitter positions, filter selections, etc. The {@code viewKey} namespaces the entry
 * (e.g. {@code "grid.business.list"}); {@code payload} holds the view-specific JSON.
 */
@Entity
@Table(name = "user_view_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_view",
                columnNames = {"user_id", "view_key"}))
@Getter
@Setter
@NoArgsConstructor
public class UserViewPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "view_key", nullable = false, length = 255)
    private String viewKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

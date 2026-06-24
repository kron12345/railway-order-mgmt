package com.ordermgmt.railway.domain.order.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Staging record for an inbound R2P order (a third party sends a timetable + a personnel/vehicle
 * request). Sits in the intake inbox until a planner consciously accepts it into an order.
 *
 * <p>Not {@code @Audited}: this is transient mock intake staging, not a business record — once
 * accepted, the resulting OrderPosition / ResourceNeeds / PurchasePositions carry the audited
 * trail.
 */
@Entity
@Table(name = "r2p_inbox_entries")
@Getter
@Setter
@NoArgsConstructor
public class R2pInboxEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 255)
    private String requester;

    @Column(name = "operational_train_number", length = 20)
    private String operationalTrainNumber;

    @Column(name = "from_location")
    private String fromLocation;

    @Column(name = "to_location")
    private String toLocation;

    @Column(name = "start_at")
    private LocalDateTime start;

    @Column(name = "end_at")
    private LocalDateTime end;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private R2pInboxStatus status = R2pInboxStatus.EINGEGANGEN;

    /** Order position this entry was accepted into; null while still pending. */
    private UUID linkedPositionId;

    /** Requested resources as a JSON array of {@link R2pResourceRequest}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requested_resources_json", columnDefinition = "jsonb")
    private String requestedResourcesJson;

    private Instant receivedAt;
}

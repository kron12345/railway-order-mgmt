package com.ordermgmt.railway.domain.pathmanager.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single journey location (stop) within a train version's itinerary. */
@Entity
@Table(
        name = "pm_journey_locations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"train_version_id", "sequence"}))
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PmJourneyLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_version_id", nullable = false)
    private PmTrainVersion trainVersion;

    @Column(nullable = false)
    private Integer sequence;

    @Column(length = 2)
    private String countryCodeIso;

    @Column(length = 10)
    private String locationPrimaryCode;

    @Column(length = 255)
    private String primaryLocationName;

    @Column(length = 20)
    private String uopid;

    @Column(length = 2)
    private String journeyLocationType;

    @Column(length = 8)
    private String arrivalTime;

    private Integer arrivalOffset = 0;

    @Column(length = 8)
    private String departureTime;

    private Integer departureOffset = 0;

    private Integer dwellTime;

    @Column(length = 3)
    private String arrivalQualifier;

    @Column(length = 3)
    private String departureQualifier;

    @Column(length = 10)
    private String subsidiaryCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String activities;

    @Column(name = "associated_train_otn", length = 20)
    private String associatedTrainOtn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String networkSpecificParams;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 100)
    private String updatedBy;

    @Version
    @Column(nullable = false)
    private Long version;
}

package com.ordermgmt.railway.domain.pathmanager.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Snapshot of train data at a specific point in the path lifecycle. */
@Entity
@Table(
        name = "pm_train_versions",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"reference_train_id", "version_number"}))
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PmTrainVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_train_id", nullable = false)
    private PmReferenceTrain referenceTrain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_id")
    private PmPath path;

    @Column(nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VersionType versionType;

    @Column(length = 255)
    private String label;

    @Column(length = 20)
    private String operationalTrainNumber;

    @Column(length = 2)
    private String trainType;

    @Column(length = 10)
    private String trafficTypeCode;

    private Integer trainWeight;

    private Integer trainLength;

    private Integer trainMaxSpeed;

    private LocalDate calendarStart;

    private LocalDate calendarEnd;

    @Column(columnDefinition = "text")
    private String calendarBitmap;

    private Integer offsetToReference = 0;

    @OneToMany(mappedBy = "trainVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<PmJourneyLocation> journeyLocations = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** TTT Reference Train — the central aggregate in the path management lifecycle. */
@Entity
@Table(name = "pm_reference_trains")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PmReferenceTrain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_year_id", nullable = false)
    private PmTimetableYear timetableYear;

    @Column(nullable = false, length = 4)
    private String tridCompany;

    @Column(nullable = false, length = 20)
    private String tridCore;

    @Column(nullable = false, length = 2)
    private String tridVariant = "01";

    @Column(nullable = false)
    private Integer tridTimetableYear;

    @Column(length = 20)
    private String operationalTrainNumber;

    @Column(length = 2)
    private String trainType;

    @Column(length = 10)
    private String trafficTypeCode;

    private Boolean pushPullTrain = false;

    private LocalDate calendarStart;

    private LocalDate calendarEnd;

    @Column(columnDefinition = "text")
    private String calendarBitmap;

    private Integer trainWeight;

    private Integer trainLength;

    private Integer trainMaxSpeed;

    @Column(length = 10)
    private String brakeType;

    private UUID sourcePositionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PathProcessState processState = PathProcessState.NEW;

    @OneToMany(mappedBy = "referenceTrain", cascade = CascadeType.ALL)
    private List<PmTrainVersion> trainVersions = new ArrayList<>();

    @OneToMany(mappedBy = "referenceTrain", cascade = CascadeType.ALL)
    private List<PmPathRequest> pathRequests = new ArrayList<>();

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

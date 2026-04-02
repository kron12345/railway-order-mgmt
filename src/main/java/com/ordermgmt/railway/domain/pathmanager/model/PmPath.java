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
import jakarta.persistence.Version;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** TTT Path — an offer or booking received from the infrastructure manager. */
@Entity
@Table(name = "pm_paths")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PmPath {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_request_id")
    private PmPathRequest pathRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_train_id", nullable = false)
    private PmReferenceTrain referenceTrain;

    @Column(nullable = false, length = 4)
    private String paidCompany;

    @Column(nullable = false, length = 20)
    private String paidCore;

    @Column(nullable = false, length = 2)
    private String paidVariant = "01";

    @Column(nullable = false)
    private Integer paidTimetableYear;

    @Column(nullable = false, length = 30)
    private String currentState = "DRAFT_OFFER";

    private Integer typeOfInformation;

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

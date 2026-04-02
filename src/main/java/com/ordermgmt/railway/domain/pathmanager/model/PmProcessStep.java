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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable audit record of a process step in the path lifecycle. */
@Entity
@Table(name = "pm_process_steps")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PmProcessStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_request_id")
    private PmPathRequest pathRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "path_id")
    private PmPath path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_train_id", nullable = false)
    private PmReferenceTrain referenceTrain;

    @Column(nullable = false, length = 30)
    private String stepType;

    @Column(length = 30)
    private String fromState;

    @Column(length = 30)
    private String toState;

    private Integer typeOfInformation;

    private Integer messageStatus;

    @Column(length = 2000)
    private String comment;

    @Column(length = 100)
    private String simulatedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

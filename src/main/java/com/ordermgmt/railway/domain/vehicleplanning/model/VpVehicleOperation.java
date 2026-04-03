package com.ordermgmt.railway.domain.vehicleplanning.model;

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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An operational detail for a rotation entry (e.g. coupling, decoupling at a location). */
@Entity
@Table(name = "vp_vehicle_operations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class VpVehicleOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotation_entry_id", nullable = false)
    private VpRotationEntry rotationEntry;

    @Column(length = 255)
    private String locationName;

    @Column(length = 50)
    private String activityCode;

    @Column(name = "associated_train_otn", length = 20)
    private String associatedTrainOtn;

    @Column(name = "composition_section", length = 50)
    private String compositionSection;

    @Column(length = 500)
    private String comment;

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

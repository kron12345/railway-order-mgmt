package com.ordermgmt.railway.domain.vehicleplanning.model;

import java.time.Instant;
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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maps a reference train to a vehicle on a specific day of the week. */
@Entity
@Table(name = "vp_rotation_entries")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class VpRotationEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VpVehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_train_id", nullable = false)
    private PmReferenceTrain referenceTrain;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "sequence_in_day", nullable = false)
    private Integer sequenceInDay = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupling_type", nullable = false, length = 20)
    private CouplingPosition couplingType = CouplingPosition.FULL;

    @OneToMany(mappedBy = "rotationEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VpVehicleOperation> operations = new ArrayList<>();

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

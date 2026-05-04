package com.ordermgmt.railway.domain.rollingstock.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

/**
 * Rolling stock master data (Fahrzeug-Stammdaten). Stores technical vehicle properties according to
 * TSI TAF/TAP specifications for use in rotation planning and order management.
 */
@Entity
@Table(name = "rs_rolling_stock")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RollingStockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- Identification ---

    /** European Vehicle Number (12 digits), e.g. "948505020019" */
    @Column(length = 12)
    private String evn;

    /** Type designation, e.g. "RABe 502", "Re 460", "Habbins 354" */
    @Column(nullable = false, length = 50)
    private String designation;

    /** Vehicle keeper marking, e.g. "SBB", "SBBCARGO", "DB" */
    @Column(length = 10)
    private String keeperCode;

    /** UIC country code, e.g. "85" = Switzerland, "80" = Germany */
    @Column(length = 2)
    private String ownerCountryCode;

    // --- Classification ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleCategory vehicleCategory;

    /** UIC wagon letter code for freight, e.g. "Habbins", "Eanos", "Sgns" */
    @Column(length = 20)
    private String uicLetterCode;

    /** TAF traction type code, e.g. "11" = electric loco, "13" = electric MU */
    @Column(length = 4)
    private String tractionType;

    // --- Technical data ---

    private Integer numberOfAxles;

    /** Length over buffers in mm */
    private Integer lengthOverBuffers;

    /** Tare weight in kg */
    private Integer weightEmpty;

    /** Maximum speed in km/h */
    private Integer maxSpeed;

    /** Power output in kW (traction units only) */
    private Integer powerOutput;

    /** Traction system, e.g. "15kV 16.7Hz AC" */
    @Column(length = 30)
    private String tractionSystem;

    /** Axle arrangement, e.g. "Bo'Bo'" */
    @Column(length = 30)
    private String axleArrangement;

    // --- Capacity (passenger) ---

    private Integer seats1stClass;
    private Integer seats2ndClass;

    // --- Freight ---

    /** Maximum payload in kg */
    private Integer maxPayload;

    // --- Coupling & braking ---

    /** Coupling type, e.g. "Screw", "Scharfenberg" */
    @Column(length = 30)
    private String couplingType;

    /** Brake type designation */
    @Column(length = 30)
    private String brakeType;

    // --- Status ---

    @Column(nullable = false)
    private Boolean active = true;

    // --- Audit ---

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

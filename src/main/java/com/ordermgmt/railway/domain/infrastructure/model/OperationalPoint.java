package com.ordermgmt.railway.domain.infrastructure.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "operational_points")
@Getter
@Setter
@NoArgsConstructor
public class OperationalPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String uopid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 3)
    private String country;

    private Integer opType;

    @Column(length = 20)
    private String tafTapCode;

    private Double longitude;

    private Double latitude;

    @Column(nullable = false)
    private Instant importedAt = Instant.now();
}

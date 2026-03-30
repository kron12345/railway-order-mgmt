package com.ordermgmt.railway.domain.infrastructure.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Railway section of line imported from RINF reference data. */
@Entity
@Table(name = "sections_of_line")
@Getter
@Setter
@NoArgsConstructor
public class SectionOfLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String solId;

    @Column(nullable = false, length = 20)
    private String startOpUopid;

    @Column(nullable = false, length = 20)
    private String endOpUopid;

    @Column(nullable = false, length = 3)
    private String country;

    private Double lengthMeters;

    @Column(length = 10)
    private String gauge;

    private Integer maxSpeed;

    private Boolean electrified;

    @Column(nullable = false)
    private Instant importedAt = Instant.now();
}

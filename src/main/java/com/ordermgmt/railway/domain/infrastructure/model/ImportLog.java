package com.ordermgmt.railway.domain.infrastructure.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "import_log")
@Getter
@Setter
@NoArgsConstructor
public class ImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(length = 3)
    private String country;

    private int recordCount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant finishedAt;
}

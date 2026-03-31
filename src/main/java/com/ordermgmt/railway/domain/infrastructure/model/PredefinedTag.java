package com.ordermgmt.railway.domain.infrastructure.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Predefined tag for categorizing orders and positions. */
@Entity
@Table(name = "predefined_tags")
@Getter
@Setter
@NoArgsConstructor
public class PredefinedTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @jakarta.validation.constraints.NotBlank
    @Column(nullable = false, length = 30)
    private String category = "GENERAL";

    @Column(length = 20)
    private String color;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}

package com.ordermgmt.railway.domain.order.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audited line item belonging to an order. */
@Entity
@Table(name = "order_positions")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class OrderPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 255)
    @Column(nullable = false)
    private String name;

    @jakarta.validation.constraints.NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PositionType type;

    private String tags;

    private LocalDateTime start;

    @Column(name = "\"end\"")
    private LocalDateTime end;

    @Column(length = 100)
    private String serviceType;

    /** Operational Train Number (OTN) — free text, e.g. "95345" or "95xxx". */
    @Column(length = 20)
    private String operationalTrainNumber;

    private String fromLocation;

    private String toLocation;

    @Column(length = 2000)
    private String comment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String validity;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PositionStatus internalStatus = PositionStatus.IN_BEARBEITUNG;

    /** Parent train identity when this position is an expression (Ausprägung); null for a train. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_of_id")
    private OrderPosition variantOf;

    /** Hierarchy role: ZUG (identity) or AUSPRAEGUNG (expression); null = legacy flat position. */
    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", length = 30)
    private PositionVariantType variantType;

    @org.hibernate.envers.NotAudited
    @OneToMany(mappedBy = "variantOf")
    private List<OrderPosition> children = new ArrayList<>();

    @org.hibernate.envers.NotAudited
    @OneToMany(mappedBy = "orderPosition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPositionVersion> versions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merge_target_id")
    private OrderPosition mergeTarget;

    @Column(length = 20)
    private String mergeStatus;

    /** Link to path manager reference train (if this position was sent to TTT). */
    private UUID pmReferenceTrainId;

    @OneToMany(mappedBy = "orderPosition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResourceNeed> resourceNeeds = new ArrayList<>();

    @OneToMany(mappedBy = "orderPosition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchasePosition> purchasePositions = new ArrayList<>();

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

    @Version private Long version;
}

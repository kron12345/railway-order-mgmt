package com.ordermgmt.railway.domain.order.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Purchased execution unit linked to an order position and resource need. */
@Entity
@Table(name = "purchase_positions")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class PurchasePosition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String positionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_position_id", nullable = false)
    private OrderPosition orderPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_need_id", nullable = false)
    private ResourceNeed resourceNeed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String validity;

    @Column(length = 50)
    private String debicode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus purchaseStatus = PurchaseStatus.OFFEN;

    private Instant orderedAt;

    private Instant statusTimestamp;

    @Column(length = 255)
    private String description;

    private UUID pmPathRequestId;

    private UUID pmPathId;

    @Column(length = 30)
    private String pmProcessState;

    @Column(length = 30)
    private String pmTtrPhase;

    private Instant pmLastSynced;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ttt_order_attributes", columnDefinition = "jsonb")
    private String tttOrderAttributes;
}

package com.ordermgmt.railway.domain.order.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Append-only record that a deadline rule auto-ordered a position, so a fired (rule, position) pair
 * is never triggered twice (idempotency).
 *
 * <p>Not {@code @Audited}: this is a mock automation log, not a business record (same rationale as
 * {@link R2pInboxEntry}).
 */
@Entity
@Table(
        name = "auto_order_log",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"order_position_id", "frist_regel_id"}))
@Getter
@Setter
@NoArgsConstructor
public class AutoOrderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_position_id", nullable = false)
    private UUID orderPositionId;

    @Column(name = "frist_regel_id", nullable = false)
    private UUID fristRegelId;

    @Column(nullable = false)
    private Instant triggeredAt;
}

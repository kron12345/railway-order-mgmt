package com.ordermgmt.railway.domain.order.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Records that a train-identity position carried a given OTN for a period, so the train stays
 * findable by its old number even after the OTN changes (the OTN is a label with history, while the
 * stable identity is the position/TRID).
 */
@Entity
@Table(name = "position_otn_history")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PositionOtnHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_position_id", nullable = false)
    private OrderPosition orderPosition;

    @Column(nullable = false, length = 20)
    private String otn;

    private LocalDate validFrom;

    /** {@code null} = currently valid OTN. */
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PositionChangeSource source;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(length = 100)
    private String createdBy;
}

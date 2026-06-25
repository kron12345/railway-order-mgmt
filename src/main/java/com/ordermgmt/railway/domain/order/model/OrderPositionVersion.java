package com.ordermgmt.railway.domain.order.model;

import java.time.Instant;
import java.time.LocalDate;
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

/**
 * A time-bounded change to an order-position expression (Ausprägung): the base configuration or a
 * later override (e.g. holiday vehicle, construction-site timing). {@link #source} distinguishes a
 * self-initiated {@code MODIFICATION} from an infrastructure {@code ALTERATION}; {@link
 * #validFrom}/ {@link #validTo} bound the days the override applies within the expression's
 * calendar.
 */
@Entity
@Table(name = "order_position_versions")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class OrderPositionVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_position_id", nullable = false)
    private OrderPosition orderPosition;

    @Column(nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PositionChangeSource source = PositionChangeSource.INITIAL;

    /** First day this version's configuration applies; {@code null} = the open base. */
    private LocalDate validFrom;

    /** Last day (inclusive) this version applies; {@code null} = open-ended. */
    private LocalDate validTo;

    @Column(length = 500)
    private String changeSummary;

    /** Optional JSON snapshot of the changed attributes (times, vehicle, route). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

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

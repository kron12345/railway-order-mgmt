package com.ordermgmt.railway.domain.order.model;

import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.envers.Audited;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resource demand attached to an order position. */
@Entity
@Table(name = "resource_needs")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class ResourceNeed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_position_id", nullable = false)
    private OrderPosition orderPosition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoverageType coverageType;

    @Column(length = 30)
    private String status;

    private UUID linkedFahrplanId;
}

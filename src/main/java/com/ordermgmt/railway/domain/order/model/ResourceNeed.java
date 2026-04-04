package com.ordermgmt.railway.domain.order.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resource demand attached to an order position. */
@Entity
@Table(name = "resource_needs")
@Audited
@EntityListeners(AuditingEntityListener.class)
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

    @Column(length = 255)
    private String description;

    @Column(columnDefinition = "INT DEFAULT 1")
    private Integer quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id")
    private ResourceCatalogItem catalogItem;

    private LocalDate validFrom;

    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ResourcePriority priority = ResourcePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResourceOrigin origin = ResourceOrigin.MANUAL;

    @CreatedBy
    @Column(length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(length = 100)
    private String updatedBy;
}

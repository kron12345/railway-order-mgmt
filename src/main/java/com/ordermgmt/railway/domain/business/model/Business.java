package com.ordermgmt.railway.domain.business.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audited business work item tracked in the order management domain. */
@Entity
@Table(name = "businesses")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BusinessStatus status = BusinessStatus.IN_BEARBEITUNG;

    @Column(length = 30)
    private String assignmentType;

    private String assignmentName;

    private String team;

    private LocalDate validFrom;

    private LocalDate validTo;

    private LocalDate dueDate;

    private String tags;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessDocument> documents = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "business_order_positions",
            joinColumns = @JoinColumn(name = "business_id"),
            inverseJoinColumns = @JoinColumn(name = "order_position_id"))
    private List<com.ordermgmt.railway.domain.order.model.OrderPosition> orderPositions =
            new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "business_purchase_positions",
            joinColumns = @JoinColumn(name = "business_id"),
            inverseJoinColumns = @JoinColumn(name = "purchase_position_id"))
    private List<com.ordermgmt.railway.domain.order.model.PurchasePosition> purchasePositions =
            new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version private Long version;
}

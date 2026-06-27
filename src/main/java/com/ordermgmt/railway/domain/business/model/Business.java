package com.ordermgmt.railway.domain.business.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;

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

    // @Audited on the collections so link/unlink is captured by Envers (audit join tables
    // business_order_positions_audit / business_purchase_positions_audit, V28/V29).
    // Set (not List/bag): link/unlink does a targeted INSERT/DELETE of one join row instead of a
    // full DELETE+re-INSERT of all rows, and dedups automatically.
    @Audited
    @ManyToMany
    @JoinTable(
            name = "business_order_positions",
            joinColumns = @JoinColumn(name = "business_id"),
            inverseJoinColumns = @JoinColumn(name = "order_position_id"))
    private Set<OrderPosition> orderPositions = new LinkedHashSet<>();

    @Audited
    @ManyToMany
    @JoinTable(
            name = "business_purchase_positions",
            joinColumns = @JoinColumn(name = "business_id"),
            inverseJoinColumns = @JoinColumn(name = "purchase_position_id"))
    private Set<PurchasePosition> purchasePositions = new LinkedHashSet<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version private Long version;
}

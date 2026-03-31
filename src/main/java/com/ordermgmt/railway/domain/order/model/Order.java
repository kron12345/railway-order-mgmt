package com.ordermgmt.railway.domain.order.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ordermgmt.railway.domain.customer.model.Customer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audited order aggregate root for customer work. */
@Entity
@Table(name = "orders")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 255)
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @jakarta.validation.constraints.Size(max = 2000)
    @Column(length = 2000)
    private String comment;

    private String tags;

    private LocalDate validFrom;

    private LocalDate validTo;

    @Column(length = 50)
    private String timetableYearLabel;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ProcessStatus processStatus = ProcessStatus.AUFTRAG;

    @Column(length = 50)
    private String internalStatus;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPosition> positions = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy private String updatedBy;

    @Version private Long version;
}

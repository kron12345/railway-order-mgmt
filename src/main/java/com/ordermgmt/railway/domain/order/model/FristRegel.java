package com.ordermgmt.railway.domain.order.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A configurable deadline rule that drives an "automatic business": a member filter selects
 * positions, an anchor + offset computes each member's effective deadline (absolute like a
 * Fahrplanjahr cut-off, or rolling like ±N days around the trip), and an action (show vs.
 * auto-order) fires when the deadline is reached or a TTT status (e.g. Final Offer) is hit.
 */
@Entity
@Table(name = "frist_regeln")
@Audited
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class FristRegel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberFilter memberFilter = MemberFilter.NICHT_BESTELLT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Anchor anchor = Anchor.FAHRT;

    /** Fixed deadline for {@link Anchor#ABSOLUT}. */
    private LocalDate absoluteDate;

    /** ± days for the rolling anchors ({@link Anchor#FAHRT}, {@link Anchor#FAHRPLANJAHR_START}). */
    private Integer offsetDays = 0;

    /** Days before the deadline at which a member counts as "due soon". */
    private Integer warnThresholdDays = 7;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Trigger triggerType = Trigger.DATUM;

    /** TTT process state that fires a {@link Trigger#STATUS} rule (e.g. {@code FINAL_OFFER}). */
    @Column(length = 40)
    private String triggerStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Action action = Action.ANZEIGEN;

    @Column(nullable = false)
    private boolean enabled = true;

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

    /** Where the deadline is anchored. */
    public enum Anchor {
        ABSOLUT,
        FAHRT,
        FAHRPLANJAHR_START
    }

    /** What happens when the rule fires. */
    public enum Action {
        ANZEIGEN,
        AUTO_BESTELLEN
    }

    /** Which positions are members of the rule (and its automatic business). */
    public enum MemberFilter {
        NICHT_BESTELLT,
        ALLE_FAHRPLAN
    }

    /** What fires the rule. */
    public enum Trigger {
        DATUM,
        STATUS
    }
}

package com.ordermgmt.railway.domain.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.event.TttStatusChangedEvent;
import com.ordermgmt.railway.domain.order.model.AutoOrderLog;
import com.ordermgmt.railway.domain.order.model.FristRegel;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.repository.AutoOrderLogRepository;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Fires {@code AUTO_BESTELLEN} deadline rules: when a member's trigger condition is met (deadline
 * reached, or a configured TTT status), the position is auto-ordered via {@link
 * PurchaseOrderService} (capacity/path via TTT, the rest via R2P) and marked {@code BEANTRAGT},
 * exactly once. Runs are driven by the manual button, a background schedule, and — for STATUS
 * triggers — a {@link TttStatusChangedEvent}. The {@link AutoOrderLog} guarantees idempotency and
 * is the audit trail; disabling a rule is the kill-switch. System-invoked runs (schedule/event)
 * elevate to an ADMIN security context so the {@code @PreAuthorize} order triggers pass.
 */
@Service
@RequiredArgsConstructor
public class FristAutoOrderService {

    private static final Logger log = LoggerFactory.getLogger(FristAutoOrderService.class);

    private final FristService fristService;
    private final AutoOrderLogRepository logRepository;
    private final OrderPositionRepository positionRepository;
    private final PurchaseOrderService purchaseOrderService;

    /** Evaluates the rules and auto-orders newly-eligible members; returns how many fired. */
    @Transactional
    public int runOnce() {
        return withAdminContextIfUnauthenticated(this::doRunOnce);
    }

    private int doRunOnce() {
        LocalDate today = LocalDate.now();
        int fired = 0;
        for (FristService.FristEntry entry : fristService.evaluate()) {
            FristRegel rule = entry.regel();
            if (rule.getAction() != FristRegel.Action.AUTO_BESTELLEN) {
                continue;
            }
            UUID positionId = entry.position().getId();
            // Idempotency: the pre-check skips the common case; the AutoOrderLog unique
            // (position, rule) constraint is the atomic backstop against a concurrent run.
            if (logRepository.existsByOrderPositionIdAndFristRegelId(positionId, rule.getId())) {
                continue;
            }
            OrderPosition position = positionRepository.findById(positionId).orElse(null);
            if (position == null || !triggerFires(rule, entry.deadline(), position, today)) {
                continue;
            }

            int ordered = purchaseOrderService.triggerOrderForPosition(positionId);
            position.setInternalStatus(PositionStatus.BEANTRAGT);
            positionRepository.save(position);

            logRepository.save(auditEntry(rule, positionId, entry.deadline(), ordered));
            fired++;
        }
        if (fired > 0) {
            log.info("Auto-order: triggered {} order(s) from deadline rules", fired);
        }
        return fired;
    }

    private AutoOrderLog auditEntry(
            FristRegel rule, UUID positionId, LocalDate deadline, int ordered) {
        AutoOrderLog logEntry = new AutoOrderLog();
        logEntry.setOrderPositionId(positionId);
        logEntry.setFristRegelId(rule.getId());
        logEntry.setTriggeredAt(Instant.now());
        logEntry.setTriggerType(rule.getTriggerType().name());
        logEntry.setRuleName(rule.getName());
        logEntry.setDetail(
                "Frist "
                        + (deadline != null ? deadline : "—")
                        + " · "
                        + ordered
                        + " Bestellposition(en) bestellt");
        return logEntry;
    }

    private boolean triggerFires(
            FristRegel rule, LocalDate deadline, OrderPosition position, LocalDate today) {
        if (rule.getTriggerType() == FristRegel.Trigger.DATUM) {
            return deadline != null && !deadline.isAfter(today); // due today or overdue
        }
        // STATUS trigger: a linked purchase carries the configured TTT process state (e.g.
        // FINAL_OFFERED).
        if (rule.getTriggerStatus() == null || position.getPurchasePositions() == null) {
            return false;
        }
        return position.getPurchasePositions().stream()
                .anyMatch(
                        purchasePosition ->
                                rule.getTriggerStatus()
                                        .equalsIgnoreCase(purchasePosition.getPmProcessState()));
    }

    /**
     * Background pass; long delay so the demo is driven mainly by the manual button.
     * {@code @Transactional} so the self-invoked call runs in a transaction — the single-threaded
     * scheduler plus the {@code AutoOrderLog} unique constraint keep firings idempotent even if a
     * manual run overlaps.
     */
    @Transactional
    @Scheduled(
            initialDelayString = "${app.frist.auto.initial-delay-ms:600000}",
            fixedDelayString = "${app.frist.auto.fixed-delay-ms:600000}")
    public void scheduledRun() {
        runOnce();
    }

    /** STATUS-triggered rules fire immediately when a purchase's TTT state changes. */
    @EventListener
    @Transactional
    public void onTttStatusChanged(TttStatusChangedEvent event) {
        runOnce();
    }

    /**
     * Runs the action with an elevated ADMIN context when the current thread is unauthenticated
     * (scheduler/event); preserves the caller's context (the FristenView button runs as the user).
     */
    private int withAdminContextIfUnauthenticated(java.util.function.IntSupplier action) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        boolean elevate = current == null || !current.isAuthenticated();
        if (!elevate) {
            return action.getAsInt();
        }
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "system",
                                "N/A",
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            return action.getAsInt();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

package com.ordermgmt.railway.domain.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.AutoOrderLog;
import com.ordermgmt.railway.domain.order.model.FristRegel;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.repository.AutoOrderLogRepository;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Fires {@code AUTO_BESTELLEN} deadline rules: when a member's trigger condition is met (deadline
 * reached, or a configured TTT status), the position is auto-ordered (mock effect: marked
 * BEANTRAGT) exactly once. A run is driven by the manual button and a background schedule; the
 * {@link AutoOrderLog} guarantees idempotency, and disabling a rule is the kill-switch.
 */
@Service
@RequiredArgsConstructor
public class FristAutoOrderService {

    private static final Logger log = LoggerFactory.getLogger(FristAutoOrderService.class);

    private final FristService fristService;
    private final AutoOrderLogRepository logRepository;
    private final OrderPositionRepository positionRepository;

    /** Evaluates the rules and auto-orders newly-eligible members; returns how many fired. */
    @Transactional
    public int runOnce() {
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
            position.setInternalStatus(PositionStatus.BEANTRAGT);
            positionRepository.save(position);

            AutoOrderLog logEntry = new AutoOrderLog();
            logEntry.setOrderPositionId(positionId);
            logEntry.setFristRegelId(rule.getId());
            logEntry.setTriggeredAt(Instant.now());
            logRepository.save(logEntry);
            fired++;
        }
        if (fired > 0) {
            log.info("Auto-order: triggered {} order(s) from deadline rules", fired);
        }
        return fired;
    }

    private boolean triggerFires(
            FristRegel rule, LocalDate deadline, OrderPosition position, LocalDate today) {
        if (rule.getTriggerType() == FristRegel.Trigger.DATUM) {
            return deadline != null && !deadline.isAfter(today); // due today or overdue
        }
        // STATUS trigger: a linked purchase carries the configured TTT process state (e.g.
        // FINAL_OFFER).
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
     * {@code @Transactional} here (not only on {@link #runOnce()}) so the self-invoked call runs in
     * a transaction — the single-threaded scheduler plus the {@code AutoOrderLog} unique constraint
     * keep firings idempotent even if a manual run overlaps.
     */
    @Transactional
    @Scheduled(
            initialDelayString = "${app.frist.auto.initial-delay-ms:600000}",
            fixedDelayString = "${app.frist.auto.fixed-delay-ms:600000}")
    public void scheduledRun() {
        runOnce();
    }
}

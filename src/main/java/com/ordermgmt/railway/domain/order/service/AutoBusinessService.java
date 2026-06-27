package com.ordermgmt.railway.domain.order.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.domain.business.repository.BusinessRepository;
import com.ordermgmt.railway.domain.order.model.FristRegel;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.repository.FristRegelRepository;
import com.ordermgmt.railway.domain.order.service.FristService.FristEntry;

import lombok.RequiredArgsConstructor;

/**
 * Materializes each deadline rule (Frist-Regel) as a real, auto-managed {@link Business}: the
 * rule's current members become the business's linked order positions and its earliest member
 * deadline becomes the business due date — so "automatic businesses" show up in the normal
 * Geschäfte list (marked "⚙ automatisch") with live member counts and deadlines, without a separate
 * timer construct.
 *
 * <p>Membership is recomputed (positions are linked, never physically moved). A disabled rule is
 * the kill-switch: its business is emptied. Sync runs at startup (for seeded rules), on a
 * background schedule, and on demand after rule edits.
 */
@Service
@RequiredArgsConstructor
public class AutoBusinessService {

    private static final Logger log = LoggerFactory.getLogger(AutoBusinessService.class);

    private final FristService fristService;
    private final FristRegelRepository regelRepository;
    private final BusinessRepository businessRepository;

    /** Deletes the automatic business of a rule that is being removed. */
    @Transactional
    public void removeFor(UUID ruleId) {
        businessRepository.findByFristRegelId(ruleId).ifPresent(businessRepository::delete);
    }

    /** Reconciles every rule's automatic business; returns how many were created or changed. */
    @Transactional
    public int syncAll() {
        Map<UUID, List<FristEntry>> membersByRule = new LinkedHashMap<>();
        for (FristEntry entry : fristService.evaluate()) {
            membersByRule
                    .computeIfAbsent(entry.regel().getId(), ruleId -> new ArrayList<>())
                    .add(entry);
        }

        int changed = 0;
        for (FristRegel rule : regelRepository.findAll()) {
            Business business = businessRepository.findByFristRegelId(rule.getId()).orElse(null);
            if (rule.isEnabled()) {
                if (business == null) {
                    business = newAutoBusiness(rule);
                }
                if (applyActiveState(
                        business, rule, membersByRule.getOrDefault(rule.getId(), List.of()))) {
                    businessRepository.save(business);
                    changed++;
                }
            } else if (business != null && deactivate(business)) {
                businessRepository.save(business);
                changed++;
            }
        }
        if (changed > 0) {
            log.info("Auto-business sync: {} automatic business(es) created/updated", changed);
        }
        return changed;
    }

    /** Immediate sync once the app is up, so seeded rules surface as businesses without a click. */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        syncAll();
    }

    /**
     * Background refresh; long delay so the demo is driven mainly by edits and the manual button.
     */
    @Scheduled(
            initialDelayString = "${app.frist.auto.initial-delay-ms:600000}",
            fixedDelayString = "${app.frist.auto.fixed-delay-ms:600000}")
    public void scheduledSync() {
        syncAll();
    }

    private Business newAutoBusiness(FristRegel rule) {
        Business business = new Business();
        business.setAutomatic(true);
        business.setFristRegelId(rule.getId());
        business.setStatus(BusinessStatus.IN_BEARBEITUNG);
        business.setDescription("⚙ " + rule.getName());
        return business;
    }

    /** Aligns title, membership and due date to the rule; returns true if anything changed. */
    private boolean applyActiveState(Business business, FristRegel rule, List<FristEntry> members) {
        boolean changed = false;
        if (!Objects.equals(rule.getName(), business.getTitle())) {
            business.setTitle(rule.getName());
            changed = true;
        }
        if (syncMembers(business, members)) {
            changed = true;
        }
        LocalDate dueDate = computeDueDate(rule, members);
        if (!Objects.equals(dueDate, business.getDueDate())) {
            business.setDueDate(dueDate);
            changed = true;
        }
        return changed;
    }

    private boolean syncMembers(Business business, List<FristEntry> members) {
        Set<UUID> wanted =
                members.stream()
                        .map(entry -> entry.position().getId())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> current =
                business.getOrderPositions().stream()
                        .map(OrderPosition::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (wanted.equals(current)) {
            return false;
        }
        business.getOrderPositions().clear();
        members.forEach(entry -> business.getOrderPositions().add(entry.position()));
        return true;
    }

    /** Earliest (most urgent) member deadline; the fixed date itself for absolute rules. */
    private LocalDate computeDueDate(FristRegel rule, List<FristEntry> members) {
        if (rule.getAnchor() == FristRegel.Anchor.ABSOLUT) {
            return rule.getAbsoluteDate();
        }
        return members.stream()
                .map(FristEntry::deadline)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /** Kill-switch effect for a disabled rule: empty its business. Returns true if it changed. */
    private boolean deactivate(Business business) {
        boolean changed = false;
        if (!business.getOrderPositions().isEmpty()) {
            business.getOrderPositions().clear();
            changed = true;
        }
        if (business.getDueDate() != null) {
            business.setDueDate(null);
            changed = true;
        }
        return changed;
    }
}

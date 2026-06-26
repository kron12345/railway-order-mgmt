package com.ordermgmt.railway.domain.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;
import com.ordermgmt.railway.domain.order.repository.ResourceNeedRepository;
import com.ordermgmt.railway.domain.pathmanager.model.PathAction;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.TtrPhase;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.pathmanager.service.PathProcessEngine;
import com.ordermgmt.railway.domain.pathmanager.service.TtrPhaseResolver;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;

import lombok.RequiredArgsConstructor;

/** Manages purchase positions and their TTT ordering lifecycle. */
@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);
    private static final String POSITION_PREFIX = "BP-";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PurchasePositionRepository purchasePositionRepository;
    private final ResourceNeedRepository resourceNeedRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTimetableYearRepository timetableYearRepository;
    private final PathManagerService pathManagerService;
    private final PathProcessEngine pathProcessEngine;
    private final TtrPhaseResolver ttrPhaseResolver;
    private final TimetableArchiveService timetableArchiveService;

    /**
     * Creates a new purchase position for a resource need.
     *
     * @param resourceNeedId the resource need to purchase
     * @param description optional description
     * @param validity optional validity JSON
     * @return the persisted purchase position
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public PurchasePosition createPurchasePosition(
            UUID resourceNeedId, String description, String validity) {
        ResourceNeed need =
                resourceNeedRepository
                        .findById(resourceNeedId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Resource need not found: " + resourceNeedId));

        OrderPosition orderPosition = need.getOrderPosition();
        PurchasePosition pp = new PurchasePosition();
        pp.setPositionNumber(generatePositionNumber());
        pp.setOrderPosition(orderPosition);
        pp.setResourceNeed(need);
        pp.setDescription(description);
        pp.setValidity(resolvePurchaseValidity(validity, need, orderPosition));
        pp.setPurchaseStatus(PurchaseStatus.OFFEN);

        PurchasePosition saved = purchasePositionRepository.save(pp);
        // Keep the parent's managed collection in sync. OrderPosition.purchasePositions uses
        // orphanRemoval=true, so a later save of a stale OrderPosition (its in-memory collection
        // not
        // containing this row) would otherwise silently delete the freshly created purchase.
        if (orderPosition != null
                && orderPosition.getPurchasePositions() != null
                && !orderPosition.getPurchasePositions().contains(saved)) {
            orderPosition.getPurchasePositions().add(saved);
        }
        return saved;
    }

    /**
     * Mock "R²P" order for a non-capacity external need (e.g. a Lokführer). The real R²P interface
     * is not yet defined, so for the demo this simply marks the purchase as {@code BESTELLT} —
     * mirroring the visible status of the TTT flow without creating any reference train or path
     * request.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void triggerR2pOrder(UUID purchasePositionId) {
        PurchasePosition pp =
                purchasePositionRepository
                        .findById(purchasePositionId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Purchase position not found: "
                                                        + purchasePositionId));
        pp.setPurchaseStatus(PurchaseStatus.BESTELLT);
        pp.setOrderedAt(Instant.now());
        pp.setStatusTimestamp(Instant.now());
        purchasePositionRepository.save(pp);
    }

    /**
     * Triggers a TTT path request order for a purchase position. Creates or finds the reference
     * train, stores the path request ID, and updates purchase status to BESTELLT.
     *
     * @param purchasePositionId the purchase position to order
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void triggerTttOrder(UUID purchasePositionId) {
        triggerTttOrder(purchasePositionId, null);
    }

    /**
     * Triggers a TTT path request order for a purchase position with optional TTT attributes.
     * Stores the attributes JSON, creates or finds the reference train, stores the path request ID,
     * and updates purchase status to BESTELLT.
     *
     * @param purchasePositionId the purchase position to order
     * @param tttAttributes optional JSON string with TTT order attributes from the dialog
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void triggerTttOrder(UUID purchasePositionId, String tttAttributes) {
        PurchasePosition pp =
                purchasePositionRepository
                        .findById(purchasePositionId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Purchase position not found: "
                                                        + purchasePositionId));

        if (tttAttributes != null && !tttAttributes.isBlank()) {
            pp.setTttOrderAttributes(tttAttributes);
            extractDebitCode(pp, tttAttributes);
        }

        OrderPosition position = pp.getOrderPosition();

        TimetableArchive archive = timetableArchiveService.findArchive(position).orElse(null);
        List<TimetableRowData> rows =
                archive != null ? timetableArchiveService.readRows(archive) : List.of();

        PmReferenceTrain train =
                findExistingTrain(position)
                        .orElseGet(
                                () ->
                                        pathManagerService.createTrainFromOrderPosition(
                                                position, archive, rows));

        // Execute SEND_REQUEST transition to create a proper path request
        if (train.getProcessState() == PathProcessState.NEW) {
            pathProcessEngine.executeTransition(
                    train.getId(), PathAction.SEND_REQUEST, "TTT order triggered");
            // Reload train to get updated pathRequests
            train =
                    referenceTrainRepository
                            .findById(train.getId())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Train not found after transition"));
        }

        // Link order position to the reference train
        if (position.getPmReferenceTrainId() == null) {
            position.setPmReferenceTrainId(train.getId());
            orderPositionRepository.save(position);
        }

        pp.setPmPathRequestId(resolvePathRequestId(train));
        pp.setPurchaseStatus(PurchaseStatus.BESTELLT);
        pp.setOrderedAt(Instant.now());

        purchasePositionRepository.save(pp);
    }

    /**
     * Triggers TTT orders for all CAPACITY/EXTERNAL resource needs of an order position. Creates
     * purchase positions if they don't exist yet.
     *
     * @param orderPositionId the order position whose capacity needs should be ordered
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void triggerAllCapacityOrders(UUID orderPositionId) {
        List<ResourceNeed> capacityNeeds =
                resourceNeedRepository.findByOrderPositionIdAndResourceTypeAndCoverageType(
                        orderPositionId, ResourceType.CAPACITY, CoverageType.EXTERNAL);

        for (ResourceNeed need : capacityNeeds) {
            PurchasePosition pp = findOrCreatePurchasePosition(need);
            if (pp.getPurchaseStatus() == PurchaseStatus.OFFEN) {
                triggerTttOrder(pp.getId());
            }
        }
    }

    /**
     * Synchronizes the TTT status for a single purchase position from the path manager data.
     *
     * @param purchasePositionId the purchase position to sync
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void syncTttStatus(UUID purchasePositionId) {
        PurchasePosition pp =
                purchasePositionRepository
                        .findById(purchasePositionId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Purchase position not found: "
                                                        + purchasePositionId));

        if (pp.getPmPathRequestId() == null) {
            return;
        }

        OrderPosition position = pp.getOrderPosition();
        PmReferenceTrain train = findExistingTrain(position).orElse(null);
        if (train == null) {
            return;
        }

        // Update process state from reference train
        pp.setPmProcessState(train.getProcessState().name());

        // Resolve TTR phase for context
        resolveTtrPhase(position).ifPresent(phase -> pp.setPmTtrPhase(phase.name()));

        pp.setPmLastSynced(Instant.now());

        // Map PM process state to purchase status
        PurchaseStatus mappedStatus = mapProcessStateToPurchaseStatus(train.getProcessState());
        if (mappedStatus != null) {
            pp.setPurchaseStatus(mappedStatus);
            pp.setStatusTimestamp(Instant.now());
        }

        purchasePositionRepository.save(pp);
    }

    /**
     * Synchronizes TTT status for all purchase positions of an order position that have a path
     * request ID.
     *
     * @param orderPositionId the order position to sync all purchases for
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void syncAllTttStatuses(UUID orderPositionId) {
        List<PurchasePosition> positions =
                purchasePositionRepository.findByOrderPositionIdAndPmPathRequestIdIsNotNull(
                        orderPositionId);

        for (PurchasePosition pp : positions) {
            syncTttStatus(pp.getId());
        }
    }

    /**
     * Order-side response to a RailOpt-announced path alteration: accepts or rejects the offered
     * alteration on the linked reference train, then re-syncs the purchase status.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void respondToAlteration(UUID orderPositionId, boolean accept) {
        OrderPosition position =
                orderPositionRepository
                        .findById(orderPositionId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Order position not found: " + orderPositionId));
        UUID trainId = position.getPmReferenceTrainId();
        if (trainId == null) {
            return;
        }
        pathProcessEngine.executeTransition(
                trainId,
                accept ? PathAction.ACCEPT_ALTERATION : PathAction.REJECT_ALTERATION,
                accept ? "Alteration accepted (order)" : "Alteration rejected (order)");
        syncAllTttStatuses(orderPositionId);
    }

    private String generatePositionNumber() {
        return POSITION_PREFIX + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Extracts the debitCode from the TTT attributes JSON and stores it on the purchase position.
     */
    private void extractDebitCode(PurchasePosition pp, String tttAttributes) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(tttAttributes);
            JsonNode debitNode = root.get("debitCode");
            if (debitNode != null && !debitNode.isNull() && !debitNode.asText().isBlank()) {
                pp.setDebicode(debitNode.asText());
            }
        } catch (Exception e) {
            log.warn("Failed to extract debitCode from TTT attributes: {}", e.getMessage());
        }
    }

    /**
     * The validity a purchase position inherits: the Bedarf's own Verkehrstage when it has them —
     * so an R²P variation like "2 attendants on weekends, 1 on weekdays" is captured per demand on
     * the same path — else the caller's explicit value, else the order position's overall validity.
     */
    private static String resolvePurchaseValidity(
            String explicit, ResourceNeed need, OrderPosition orderPosition) {
        if (need.getValidity() != null && !need.getValidity().isBlank()) {
            return need.getValidity();
        }
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        return orderPosition != null ? orderPosition.getValidity() : null;
    }

    private PurchasePosition findOrCreatePurchasePosition(ResourceNeed need) {
        List<PurchasePosition> existing =
                purchasePositionRepository.findByResourceNeedId(need.getId());
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        return createPurchasePosition(need.getId(), null, need.getOrderPosition().getValidity());
    }

    private java.util.Optional<PmReferenceTrain> findExistingTrain(OrderPosition position) {
        List<PmReferenceTrain> trains =
                referenceTrainRepository.findBySourcePositionId(position.getId());
        return trains.isEmpty()
                ? java.util.Optional.empty()
                : java.util.Optional.of(trains.getFirst());
    }

    private UUID resolvePathRequestId(PmReferenceTrain train) {
        if (train.getPathRequests() != null && !train.getPathRequests().isEmpty()) {
            return train.getPathRequests().getFirst().getId();
        }
        return train.getId();
    }

    private java.util.Optional<TtrPhase> resolveTtrPhase(OrderPosition position) {
        int year = resolveYear(position);
        return timetableYearRepository
                .findByYear(year)
                .map(ttYear -> ttrPhaseResolver.resolvePhase(ttYear, LocalDate.now()));
    }

    private int resolveYear(OrderPosition position) {
        if (position.getStart() != null) {
            LocalDate startDate = position.getStart().toLocalDate();
            if (startDate.getMonthValue() == 12 && startDate.getDayOfMonth() >= 14) {
                return startDate.getYear() + 1;
            }
            return startDate.getYear();
        }
        return LocalDate.now().getYear();
    }

    private PurchaseStatus mapProcessStateToPurchaseStatus(PathProcessState state) {
        return switch (state) {
            case DRAFT_OFFERED,
                    FINAL_OFFERED,
                    RECEIPT_CONFIRMED,
                    CREATED,
                    MODIFIED,
                    REVISION_REQUESTED ->
                    PurchaseStatus.BESTELLT;
            case BOOKED, ALTERATION_ANNOUNCED, ALTERATION_OFFERED, MODIFICATION_REQUESTED ->
                    PurchaseStatus.BESTAETIGT;
            case NO_ALTERNATIVE, CANCELED -> PurchaseStatus.ABGELEHNT;
            case WITHDRAWN, SUPERSEDED -> PurchaseStatus.STORNIERT;
            default -> null; // NEW: not yet ordered
        };
    }
}

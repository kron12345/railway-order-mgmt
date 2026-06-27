package com.ordermgmt.railway.domain.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
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
    private static final String DEBIT_CODE_FIELD = "debitCode";
    private static final int POSITION_NUMBER_RANDOM_LENGTH = 8;
    private static final int TTR_YEAR_ROLLOVER_DAY = 14;
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
        PurchasePosition purchasePosition = new PurchasePosition();
        purchasePosition.setPositionNumber(generatePositionNumber());
        purchasePosition.setOrderPosition(orderPosition);
        purchasePosition.setResourceNeed(need);
        purchasePosition.setDescription(description);
        purchasePosition.setValidity(resolvePurchaseValidity(validity, need, orderPosition));
        purchasePosition.setPurchaseStatus(PurchaseStatus.OFFEN);

        PurchasePosition saved = purchasePositionRepository.save(purchasePosition);
        // Keep the parent's managed collection in sync. OrderPosition.purchasePositions uses
        // orphanRemoval=true, so a later save of a stale OrderPosition (its in-memory collection
        // not containing this row) would otherwise silently delete the freshly created purchase.
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
        PurchasePosition purchasePosition = getPurchasePosition(purchasePositionId);
        purchasePosition.setPurchaseStatus(PurchaseStatus.BESTELLT);
        purchasePosition.setOrderedAt(Instant.now());
        purchasePosition.setStatusTimestamp(Instant.now());
        purchasePositionRepository.save(purchasePosition);
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
        PurchasePosition purchasePosition = getPurchasePosition(purchasePositionId);

        if (tttAttributes != null && !tttAttributes.isBlank()) {
            purchasePosition.setTttOrderAttributes(tttAttributes);
            extractDebitCode(purchasePosition, tttAttributes);
        }

        OrderPosition position = purchasePosition.getOrderPosition();
        PmReferenceTrain train = prepareReferenceTrain(position);
        linkReferenceTrain(position, train);

        purchasePosition.setPmPathRequestId(resolvePathRequestId(train));
        purchasePosition.setPurchaseStatus(PurchaseStatus.BESTELLT);
        purchasePosition.setOrderedAt(Instant.now());

        purchasePositionRepository.save(purchasePosition);
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
            PurchasePosition purchasePosition = findOrCreatePurchasePosition(need);
            if (purchasePosition.getPurchaseStatus() == PurchaseStatus.OFFEN) {
                triggerTttOrder(purchasePosition.getId());
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
        PurchasePosition purchasePosition = getPurchasePosition(purchasePositionId);

        if (purchasePosition.getPmPathRequestId() == null) {
            return;
        }

        OrderPosition position = purchasePosition.getOrderPosition();
        PmReferenceTrain train = findExistingTrain(position).orElse(null);
        if (train == null) {
            return;
        }

        purchasePosition.setPmProcessState(train.getProcessState().name());
        resolveTtrPhase(position).ifPresent(phase -> purchasePosition.setPmTtrPhase(phase.name()));
        purchasePosition.setPmLastSynced(Instant.now());

        PurchaseStatus mappedStatus = mapProcessStateToPurchaseStatus(train.getProcessState());
        if (mappedStatus != null) {
            purchasePosition.setPurchaseStatus(mappedStatus);
            purchasePosition.setStatusTimestamp(Instant.now());
        }

        purchasePositionRepository.save(purchasePosition);
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

        for (PurchasePosition purchasePosition : positions) {
            syncTttStatus(purchasePosition.getId());
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
        return POSITION_PREFIX
                + UUID.randomUUID()
                        .toString()
                        .substring(0, POSITION_NUMBER_RANDOM_LENGTH)
                        .toUpperCase();
    }

    private PurchasePosition getPurchasePosition(UUID purchasePositionId) {
        return purchasePositionRepository
                .findById(purchasePositionId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Purchase position not found: " + purchasePositionId));
    }

    private PmReferenceTrain prepareReferenceTrain(OrderPosition position) {
        TimetableArchive archive = timetableArchiveService.findArchive(position).orElse(null);
        List<TimetableRowData> rows =
                archive != null ? timetableArchiveService.readRows(archive) : List.of();
        PmReferenceTrain train =
                findExistingTrain(position)
                        .orElseGet(
                                () ->
                                        pathManagerService.createTrainFromOrderPosition(
                                                position, archive, rows));
        return sendPathRequestIfNeeded(train);
    }

    private PmReferenceTrain sendPathRequestIfNeeded(PmReferenceTrain train) {
        if (train.getProcessState() != PathProcessState.NEW) {
            return train;
        }
        pathProcessEngine.executeTransition(
                train.getId(), PathAction.SEND_REQUEST, "TTT order triggered");
        return referenceTrainRepository
                .findById(train.getId())
                .orElseThrow(() -> new IllegalStateException("Train not found after transition"));
    }

    private void linkReferenceTrain(OrderPosition position, PmReferenceTrain train) {
        if (position.getPmReferenceTrainId() != null) {
            return;
        }
        position.setPmReferenceTrainId(train.getId());
        orderPositionRepository.save(position);
    }

    private void extractDebitCode(PurchasePosition purchasePosition, String tttAttributes) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(tttAttributes);
            JsonNode debitNode = root.get(DEBIT_CODE_FIELD);
            if (debitNode != null && !debitNode.isNull() && !debitNode.asText().isBlank()) {
                purchasePosition.setDebicode(debitNode.asText());
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

    private Optional<PmReferenceTrain> findExistingTrain(OrderPosition position) {
        List<PmReferenceTrain> trains =
                referenceTrainRepository.findBySourcePositionId(position.getId());
        return trains.isEmpty() ? Optional.empty() : Optional.of(trains.getFirst());
    }

    private UUID resolvePathRequestId(PmReferenceTrain train) {
        if (train.getPathRequests() != null && !train.getPathRequests().isEmpty()) {
            return train.getPathRequests().getFirst().getId();
        }
        return train.getId();
    }

    private Optional<TtrPhase> resolveTtrPhase(OrderPosition position) {
        int year = resolveYear(position);
        return timetableYearRepository
                .findByYear(year)
                .map(ttYear -> ttrPhaseResolver.resolvePhase(ttYear, LocalDate.now()));
    }

    private int resolveYear(OrderPosition position) {
        if (position.getStart() != null) {
            LocalDate startDate = position.getStart().toLocalDate();
            if (startDate.getMonth() == Month.DECEMBER
                    && startDate.getDayOfMonth() >= TTR_YEAR_ROLLOVER_DAY) {
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

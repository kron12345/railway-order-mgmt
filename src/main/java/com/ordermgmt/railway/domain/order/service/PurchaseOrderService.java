package com.ordermgmt.railway.domain.order.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;
import com.ordermgmt.railway.domain.order.repository.ResourceNeedRepository;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.TtrPhase;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
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

    private static final String POSITION_PREFIX = "BP-";

    private final PurchasePositionRepository purchasePositionRepository;
    private final ResourceNeedRepository resourceNeedRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTimetableYearRepository timetableYearRepository;
    private final PathManagerService pathManagerService;
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

        PurchasePosition pp = new PurchasePosition();
        pp.setPositionNumber(generatePositionNumber());
        pp.setOrderPosition(need.getOrderPosition());
        pp.setResourceNeed(need);
        pp.setDescription(description);
        pp.setValidity(validity);
        pp.setPurchaseStatus(PurchaseStatus.OFFEN);

        return purchasePositionRepository.save(pp);
    }

    /**
     * Triggers a TTT path request order for a purchase position. Creates or finds the reference
     * train, stores the path request ID, and updates purchase status to BESTELLT.
     *
     * @param purchasePositionId the purchase position to order
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public void triggerTttOrder(UUID purchasePositionId) {
        PurchasePosition pp =
                purchasePositionRepository
                        .findById(purchasePositionId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Purchase position not found: "
                                                        + purchasePositionId));

        ResourceNeed need = pp.getResourceNeed();
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

    private String generatePositionNumber() {
        long count = purchasePositionRepository.countByPositionNumberStartingWith(POSITION_PREFIX);
        return POSITION_PREFIX + String.format("%05d", count + 1);
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
            case DRAFT_OFFERED, FINAL_OFFERED, RECEIPT_CONFIRMED, CREATED, MODIFIED ->
                    PurchaseStatus.BESTELLT;
            case BOOKED -> PurchaseStatus.BESTAETIGT;
            case NO_ALTERNATIVE, CANCELED -> PurchaseStatus.ABGELEHNT;
            case WITHDRAWN, SUPERSEDED -> PurchaseStatus.STORNIERT;
            default -> null;
        };
    }
}

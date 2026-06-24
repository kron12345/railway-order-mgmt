package com.ordermgmt.railway.domain.order.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.R2pInboxEntry;
import com.ordermgmt.railway.domain.order.model.R2pInboxStatus;
import com.ordermgmt.railway.domain.order.model.R2pResourceRequest;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceOrigin;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;
import com.ordermgmt.railway.domain.order.repository.R2pInboxEntryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Mock inbound R2P intake: a third party "sends a timetable and orders personnel/vehicle". Such
 * orders land in an inbox ({@link R2pInboxEntry}) and are consciously accepted by a planner, which
 * attaches the requested resources to an existing FAHRPLAN position (matched by OTN) or creates a
 * new position under a chosen order. R2P is out of scope of the SOB Fachkonzept, so this is a
 * freely designed demo mock.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class R2pIntakeService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final R2pInboxEntryRepository inboxRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final OrderRepository orderRepository;
    private final ResourceNeedService resourceNeedService;
    private final PurchaseOrderService purchaseOrderService;

    private static final List<R2pSample> SAMPLES =
            List.of(
                    new R2pSample(
                            "Eventzug Openair St. Gallen",
                            "92110",
                            "St. Gallen",
                            "Sargans",
                            List.of(
                                    new R2pResourceRequest(
                                            ResourceType.PERSONNEL,
                                            "Lokführer (Sonderfahrt)",
                                            1,
                                            null),
                                    new R2pResourceRequest(
                                            ResourceType.VEHICLE, "Triebzug Traverso", 1, null))),
                    new R2pSample(
                            "Charter HC Ambri",
                            "92455",
                            "Arth-Goldau",
                            "Bellinzona",
                            List.of(
                                    new R2pResourceRequest(
                                            ResourceType.PERSONNEL, "Kundenbegleitung", 2, null),
                                    new R2pResourceRequest(
                                            ResourceType.VEHICLE, "Verstärkungsmodul", 1, null))),
                    new R2pSample(
                            "Baustellen-Ersatz Voralpen",
                            "92780",
                            "Luzern",
                            "Romanshorn",
                            List.of(
                                    new R2pResourceRequest(
                                            ResourceType.PERSONNEL, "Lokführer", 1, null))));

    /** Demo trigger: drops a fresh inbound R2P order into the inbox. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public R2pInboxEntry simulateIncoming() {
        R2pSample sample = SAMPLES.get((int) (inboxRepository.count() % SAMPLES.size()));
        R2pInboxEntry entry = new R2pInboxEntry();
        entry.setRequester(sample.requester());
        entry.setOperationalTrainNumber(sample.otn());
        entry.setFromLocation(sample.from());
        entry.setToLocation(sample.to());
        entry.setStatus(R2pInboxStatus.EINGEGANGEN);
        entry.setReceivedAt(Instant.now());
        entry.setRequestedResourcesJson(writeJson(sample.resources()));
        return inboxRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<R2pInboxEntry> findPending() {
        return inboxRepository.findByStatusOrderByReceivedAtDesc(R2pInboxStatus.EINGEGANGEN);
    }

    /**
     * Existing FAHRPLAN position matching the entry's OTN (the preferred attach target), if any.
     */
    @Transactional(readOnly = true)
    public Optional<OrderPosition> findMatchingPosition(R2pInboxEntry entry) {
        if (entry.getOperationalTrainNumber() == null) {
            return Optional.empty();
        }
        List<OrderPosition> matches =
                orderPositionRepository
                        .findByOperationalTrainNumber(entry.getOperationalTrainNumber())
                        .stream()
                        .filter(p -> p.getType() == PositionType.FAHRPLAN)
                        .toList();
        // Only auto-attach on an unambiguous single match; with 0 or several matches defer to a
        // manual target-order choice instead of silently picking one.
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    public List<R2pResourceRequest> resourcesOf(R2pInboxEntry entry) {
        if (entry.getRequestedResourcesJson() == null
                || entry.getRequestedResourcesJson().isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(
                    entry.getRequestedResourcesJson(),
                    new TypeReference<List<R2pResourceRequest>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid R2P resource payload", e);
        }
    }

    /**
     * Accepts an inbound entry: attaches its resources to a FAHRPLAN position matched by OTN, or —
     * if none — creates a new LEISTUNG position under {@code fallbackOrderId}. Each requested
     * resource becomes an EXTERNAL ResourceNeed (origin {@link ResourceOrigin#R2P}) with one OFFEN
     * purchase.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public OrderPosition accept(UUID entryId, UUID fallbackOrderId) {
        R2pInboxEntry entry = inboxRepository.findById(entryId).orElseThrow();
        if (entry.getStatus() == R2pInboxStatus.UEBERNOMMEN) {
            throw new IllegalStateException("R2P entry already accepted");
        }
        OrderPosition target =
                findMatchingPosition(entry).orElseGet(() -> createPosition(entry, fallbackOrderId));

        for (R2pResourceRequest req : resourcesOf(entry)) {
            ResourceNeed need =
                    resourceNeedService.addResource(
                            target.getId(),
                            req.resourceType(),
                            req.catalogItemId(),
                            Math.max(1, req.quantity()),
                            CoverageType.EXTERNAL,
                            req.description(),
                            ResourceOrigin.R2P);
            purchaseOrderService.createPurchasePosition(need.getId(), req.description(), null);
        }

        entry.setStatus(R2pInboxStatus.UEBERNOMMEN);
        entry.setLinkedPositionId(target.getId());
        inboxRepository.save(entry);
        return target;
    }

    private OrderPosition createPosition(R2pInboxEntry entry, UUID fallbackOrderId) {
        if (fallbackOrderId == null) {
            throw new IllegalArgumentException(
                    "A target order is required when no OTN match exists");
        }
        Order order = orderRepository.findById(fallbackOrderId).orElseThrow();
        OrderPosition position = new OrderPosition();
        position.setOrder(order);
        position.setType(PositionType.LEISTUNG);
        position.setName(
                "R2P "
                        + (entry.getOperationalTrainNumber() != null
                                ? "OTN " + entry.getOperationalTrainNumber()
                                : entry.getRequester()));
        position.setOperationalTrainNumber(entry.getOperationalTrainNumber());
        position.setFromLocation(entry.getFromLocation());
        position.setToLocation(entry.getToLocation());
        position.setStart(entry.getStart());
        position.setEnd(entry.getEnd());
        return orderPositionRepository.save(position);
    }

    private String writeJson(List<R2pResourceRequest> resources) {
        try {
            return OBJECT_MAPPER.writeValueAsString(resources);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize R2P resources", e);
        }
    }

    private record R2pSample(
            String requester,
            String otn,
            String from,
            String to,
            List<R2pResourceRequest> resources) {}
}

package com.ordermgmt.railway.domain.pathmanager.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceOrigin;
import com.ordermgmt.railway.domain.order.model.ResourcePriority;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.model.ValidityJsonCodec;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;
import com.ordermgmt.railway.domain.order.repository.ResourceNeedRepository;
import com.ordermgmt.railway.domain.pathmanager.mapping.PmTimetableTranslator;
import com.ordermgmt.railway.domain.pathmanager.mapping.PmTrainAssembler;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmPlanningStatus;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmRoute;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.TrainHeaderUpdate;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmRouteRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.repository.TimetableArchiveRepository;
import com.ordermgmt.railway.domain.timetable.service.TttDraftBuilder;

import lombok.RequiredArgsConstructor;

/**
 * CRUD and mapping operations for the Path Manager domain. Pure timetable-row ⇄ entity translation
 * lives in {@link PmTimetableTranslator}; entity construction (route, initial version, timetable
 * year) in {@link PmTrainAssembler}. This service owns the repositories, transactions and the
 * capture/sync flows that wire those helpers together.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PathManagerService {

    private static final String TIMETABLE_TYPE_FAHRPLAN = "FAHRPLAN";
    private static final String ROUTE_SUMMARY_SEPARATOR = " → ";
    private static final int CAPACITY_NEED_QUANTITY = 1;

    // Keep child tables before parents; the mock reset intentionally bypasses cascades.
    private static final List<String> MOCK_TABLES_TO_CLEAR =
            List.of(
                    "vp_vehicle_operations",
                    "vp_rotation_entries",
                    "vp_vehicles",
                    "vp_rotation_sets",
                    "pm_journey_locations",
                    "pm_train_versions",
                    "pm_process_steps",
                    "pm_paths",
                    "pm_path_requests",
                    "pm_routes",
                    "pm_reference_trains",
                    "timetable_archives");

    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTrainVersionRepository trainVersionRepository;
    private final PmJourneyLocationRepository journeyLocationRepository;
    private final PmRouteRepository routeRepository;
    private final PmTimetableYearRepository timetableYearRepository;
    private final IdentifierGenerator identifierGenerator;
    private final TttDraftBuilder tttDraftBuilder;
    private final OrderRepository orderRepository;
    private final OrderPositionRepository orderPositionRepository;
    private final TimetableArchiveRepository timetableArchiveRepository;
    private final ResourceNeedRepository resourceNeedRepository;

    @PersistenceContext private EntityManager entityManager;

    /**
     * Wipes <em>all</em> Path Manager state (trains, versions, journey locations, paths, routes,
     * process steps) and the timetable archive entries linked to FAHRPLAN positions. Intended as a
     * "reset" shortcut while the path manager is still a mock — the upstream system would not
     * permit this in production.
     *
     * <p>Will fail if any current OrderPosition still references a timetable archive (FAHRPLAN
     * positions); in that case the user should delete those positions first.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void clearAllMockData() {
        MOCK_TABLES_TO_CLEAR.forEach(
                tableName ->
                        entityManager
                                .createNativeQuery("DELETE FROM " + tableName)
                                .executeUpdate());
    }

    /** Reference trains in RailOpt not yet captured as an order position (Fahrplanmanager). */
    @Transactional(readOnly = true)
    public List<PmReferenceTrain> findUnassignedTrains() {
        return referenceTrainRepository.findBySourcePositionIdIsNull();
    }

    /**
     * Mock capture: turns an unassigned RailOpt reference train into a FAHRPLAN order position
     * under the given order and back-links the train ({@code sourcePositionId}). Maps OTN, von/nach
     * and validity, and builds a {@link TimetableArchive} + CAPACITY {@link ResourceNeed} from the
     * journey locations so the captured position has a viewable timetable and is TTT-orderable.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public OrderPosition captureUnassignedTrainAsPosition(UUID trainId, UUID orderId) {
        PmReferenceTrain train = referenceTrainRepository.findById(trainId).orElseThrow();
        if (train.getSourcePositionId() != null) {
            throw new IllegalStateException("Reference train is already assigned to a position");
        }
        Order order = orderRepository.findById(orderId).orElseThrow();

        List<PmJourneyLocation> locations = latestJourneyLocations(train);
        OrderPosition position = createCapturedPosition(order, train, locations);

        OrderPosition saved = orderPositionRepository.save(position);
        enrichWithTimetableArchive(saved, locations);
        train.setSourcePositionId(saved.getId());
        referenceTrainRepository.save(train);
        return saved;
    }

    private OrderPosition createCapturedPosition(
            Order order, PmReferenceTrain train, List<PmJourneyLocation> locations) {
        OrderPosition position = new OrderPosition();
        position.setOrder(order);
        position.setType(PositionType.FAHRPLAN);
        position.setName(positionName(train));
        position.setOperationalTrainNumber(train.getOperationalTrainNumber());
        position.setPmReferenceTrainId(train.getId());
        applyCapturedRoute(position, locations);
        applyCapturedCalendar(position, train);
        return position;
    }

    private void applyCapturedRoute(OrderPosition position, List<PmJourneyLocation> locations) {
        if (locations.isEmpty()) {
            return;
        }
        position.setFromLocation(locations.getFirst().getPrimaryLocationName());
        position.setToLocation(locations.getLast().getPrimaryLocationName());
    }

    private void applyCapturedCalendar(OrderPosition position, PmReferenceTrain train) {
        if (train.getCalendarStart() != null) {
            position.setStart(train.getCalendarStart().atStartOfDay());
        }
        if (train.getCalendarEnd() != null) {
            position.setEnd(train.getCalendarEnd().atStartOfDay());
        }
        if (train.getCalendarStart() == null || train.getCalendarEnd() == null) {
            return;
        }
        // Mock capture treats the full calendar window as operating days for purchase calendars.
        position.setValidity(
                ValidityJsonCodec.toJson(
                        train.getCalendarStart()
                                .datesUntil(train.getCalendarEnd().plusDays(1))
                                .toList()));
    }

    private String positionName(PmReferenceTrain train) {
        String otn = train.getOperationalTrainNumber();
        return otn != null && !otn.isBlank() ? "OTN " + otn : train.getTridCore();
    }

    private List<PmJourneyLocation> latestJourneyLocations(PmReferenceTrain train) {
        return train.getTrainVersions().stream()
                .max(Comparator.comparing(PmTrainVersion::getVersionNumber))
                .map(PmTrainVersion::getJourneyLocations)
                .orElseGet(List::of)
                .stream()
                .sorted(Comparator.comparing(PmJourneyLocation::getSequence))
                .toList();
    }

    /**
     * Builds a {@link TimetableArchive} from the train's journey locations and links it via a new
     * CAPACITY {@link ResourceNeed}, so a captured position behaves like a normally-built FAHRPLAN
     * position ("Fahrplan öffnen" + TTT-orderable). No-op when the train has no journey locations.
     */
    private void enrichWithTimetableArchive(
            OrderPosition position, List<PmJourneyLocation> locations) {
        if (locations.isEmpty()) {
            return;
        }
        List<TimetableRowData> rows = PmTimetableTranslator.toTimetableRows(locations);
        TimetableArchive archive = createTimetableArchive(position, rows);
        createCapacityNeed(position, archive);
    }

    private TimetableArchive createTimetableArchive(
            OrderPosition position, List<TimetableRowData> rows) {
        TimetableArchive archive = new TimetableArchive();
        archive.setTimetableType(TIMETABLE_TYPE_FAHRPLAN);
        archive.setOperationalTrainNumber(position.getOperationalTrainNumber());
        archive.setRouteSummary(
                rows.getFirst().getName() + ROUTE_SUMMARY_SEPARATOR + rows.getLast().getName());
        archive.setTableData(PmTimetableTranslator.writeRows(rows));
        return timetableArchiveRepository.save(archive);
    }

    private void createCapacityNeed(OrderPosition position, TimetableArchive archive) {
        ResourceNeed need = new ResourceNeed();
        need.setOrderPosition(position);
        need.setResourceType(ResourceType.CAPACITY);
        need.setCoverageType(CoverageType.EXTERNAL);
        need.setQuantity(CAPACITY_NEED_QUANTITY);
        need.setOrigin(ResourceOrigin.AUTO);
        need.setPriority(ResourcePriority.MEDIUM);
        need.setValidFrom(position.getStart() != null ? position.getStart().toLocalDate() : null);
        need.setValidTo(position.getEnd() != null ? position.getEnd().toLocalDate() : null);
        need.setLinkedFahrplanId(archive.getId());
        resourceNeedRepository.save(need);
    }

    /**
     * Creates a new reference train from an order position and its timetable data.
     *
     * @param position the source order position
     * @param archive the linked timetable archive
     * @param rows the parsed timetable row data
     * @return the persisted reference train with initial version and journey locations
     */
    public PmReferenceTrain createTrainFromOrderPosition(
            OrderPosition position, TimetableArchive archive, List<TimetableRowData> rows) {

        PmTimetableYear year = resolveOrCreateTimetableYear(position);

        PmReferenceTrain train = new PmReferenceTrain();
        train.setTimetableYear(year);
        train.setTridCompany(identifierGenerator.company());
        train.setTridCore(identifierGenerator.generateCore());
        train.setTridVariant(identifierGenerator.initialVariant());
        train.setTridTimetableYear(year.getYear());
        train.setOperationalTrainNumber(position.getOperationalTrainNumber());
        train.setSourcePositionId(position.getId());
        train.setProcessState(PathProcessState.NEW);

        if (archive != null) {
            train.setOperationalTrainNumber(archive.getOperationalTrainNumber());
        }

        train = referenceTrainRepository.save(train);

        PmRoute route = PmTrainAssembler.createRoute(train, rows, identifierGenerator);
        routeRepository.save(route);

        PmTrainVersion initialVersion =
                PmTrainAssembler.createInitialVersion(train, rows, tttDraftBuilder);
        trainVersionRepository.save(initialVersion);

        return train;
    }

    /**
     * Syncs timetable data to the path manager. Creates a new reference train if none exists for
     * the position, or updates the existing one's journey locations.
     */
    public PmReferenceTrain syncFromTimetable(
            OrderPosition position, TimetableArchive archive, List<TimetableRowData> rows) {
        if (position.getPmReferenceTrainId() != null) {
            return updateExistingTrain(position.getPmReferenceTrainId(), archive, rows);
        }
        PmReferenceTrain train = createTrainFromOrderPosition(position, archive, rows);
        position.setPmReferenceTrainId(train.getId());
        return train;
    }

    private PmReferenceTrain updateExistingTrain(
            UUID trainId, TimetableArchive archive, List<TimetableRowData> rows) {
        PmReferenceTrain train =
                referenceTrainRepository
                        .findById(trainId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Reference train not found: " + trainId));
        if (archive != null) {
            train.setOperationalTrainNumber(archive.getOperationalTrainNumber());
        }

        updateRoutePoints(train, rows);
        replaceLatestJourneyLocations(train, rows);

        return referenceTrainRepository.save(train);
    }

    private void updateRoutePoints(PmReferenceTrain train, List<TimetableRowData> rows) {
        List<PmRoute> routes = routeRepository.findByReferenceTrainId(train.getId());
        if (!routes.isEmpty()) {
            PmRoute route = routes.getFirst();
            route.setRoutePoints(PmTimetableTranslator.routePointsToJson(rows));
            routeRepository.save(route);
        }
    }

    private void replaceLatestJourneyLocations(
            PmReferenceTrain train, List<TimetableRowData> rows) {
        List<PmTrainVersion> versions =
                trainVersionRepository.findByReferenceTrainIdOrderByVersionNumberDesc(
                        train.getId());
        if (versions.isEmpty()) {
            return;
        }

        // Repository method is newest-first by name: ...OrderByVersionNumberDesc.
        PmTrainVersion latestVersion = versions.getFirst();
        journeyLocationRepository.deleteAll(latestVersion.getJourneyLocations());
        latestVersion.getJourneyLocations().clear();
        latestVersion
                .getJourneyLocations()
                .addAll(
                        PmTimetableTranslator.buildJourneyLocations(
                                rows, latestVersion, tttDraftBuilder));
        trainVersionRepository.save(latestVersion);
    }

    @Transactional(readOnly = true)
    public PmReferenceTrain findById(UUID id) {
        return referenceTrainRepository
                .findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException("Reference train not found: " + id));
    }

    /**
     * Like {@link #findById(UUID)} but eagerly initializes the version history and each version's
     * journey locations, so callers (e.g. the deviation detector) can read them after the
     * transaction closes.
     */
    @Transactional(readOnly = true)
    public PmReferenceTrain findByIdWithVersions(UUID id) {
        PmReferenceTrain train = findById(id);
        train.getTrainVersions().forEach(version -> version.getJourneyLocations().size());
        return train;
    }

    /**
     * Updates the planning status of a reference train, mirroring what RailOpt's rotation/resource
     * planning reports back. Orthogonal to the TTT path-process state.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public PmReferenceTrain updatePlanningStatus(UUID trainId, PmPlanningStatus status) {
        PmReferenceTrain train = findById(trainId);
        train.setPlanningStatus(status);
        return referenceTrainRepository.save(train);
    }

    @Transactional(readOnly = true)
    public List<PmReferenceTrain> findByYear(int year) {
        return referenceTrainRepository.findByTimetableYearYearOrderByOperationalTrainNumberAsc(
                year);
    }

    public void updateTrainHeader(TrainHeaderUpdate update) {
        PmReferenceTrain train = findById(update.trainId());
        if (update.operationalTrainNumber() != null) {
            train.setOperationalTrainNumber(update.operationalTrainNumber());
        }
        if (update.trainType() != null) {
            train.setTrainType(update.trainType());
        }
        if (update.trafficTypeCode() != null) {
            train.setTrafficTypeCode(update.trafficTypeCode());
        }
        if (update.trainWeight() != null) {
            train.setTrainWeight(update.trainWeight());
        }
        if (update.trainLength() != null) {
            train.setTrainLength(update.trainLength());
        }
        if (update.trainMaxSpeed() != null) {
            train.setTrainMaxSpeed(update.trainMaxSpeed());
        }
        if (update.brakeType() != null) {
            train.setBrakeType(update.brakeType());
        }
        referenceTrainRepository.save(train);
    }

    public void updateJourneyLocation(
            UUID locationId,
            String arrivalTime,
            String departureTime,
            Integer dwellTime,
            String arrivalQualifier,
            String departureQualifier,
            String activities,
            String journeyLocationType,
            String subsidiaryCode,
            String associatedTrainOtn) {
        PmJourneyLocation location =
                journeyLocationRepository
                        .findById(locationId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Journey location not found: " + locationId));
        if (arrivalTime != null) {
            location.setArrivalTime(arrivalTime);
        }
        if (departureTime != null) {
            location.setDepartureTime(departureTime);
        }
        if (dwellTime != null) {
            location.setDwellTime(dwellTime);
        }
        if (arrivalQualifier != null) {
            location.setArrivalQualifier(arrivalQualifier);
        }
        if (departureQualifier != null) {
            location.setDepartureQualifier(departureQualifier);
        }
        if (activities != null) {
            location.setActivities(activities);
        }
        if (journeyLocationType != null) {
            location.setJourneyLocationType(journeyLocationType);
        }
        if (subsidiaryCode != null) {
            location.setSubsidiaryCode(subsidiaryCode);
        }
        if (associatedTrainOtn != null) {
            location.setAssociatedTrainOtn(associatedTrainOtn);
        }
        journeyLocationRepository.save(location);
    }

    private PmTimetableYear resolveOrCreateTimetableYear(OrderPosition position) {
        int year = PmTrainAssembler.resolveYear(position);
        return timetableYearRepository
                .findByYear(year)
                .orElseGet(
                        () ->
                                timetableYearRepository.save(
                                        PmTrainAssembler.newTimetableYear(year)));
    }
}

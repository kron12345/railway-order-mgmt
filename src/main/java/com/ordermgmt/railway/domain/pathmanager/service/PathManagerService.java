package com.ordermgmt.railway.domain.pathmanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmPlanningStatus;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmRoute;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.TrainHeaderUpdate;
import com.ordermgmt.railway.domain.pathmanager.model.VersionType;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmRouteRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.timetable.model.JourneyLocationType;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.model.TttJourneyLocationDraft;
import com.ordermgmt.railway.domain.timetable.model.TttPathRequestDraft;
import com.ordermgmt.railway.domain.timetable.repository.TimetableArchiveRepository;
import com.ordermgmt.railway.domain.timetable.service.TttDraftBuilder;

import lombok.RequiredArgsConstructor;

/** CRUD and mapping operations for the Path Manager domain. */
@Service
@RequiredArgsConstructor
@Transactional
public class PathManagerService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Default label for the first version of a train created from an order position. */
    private static final String INITIAL_VERSION_LABEL = "Initial v1";

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
        // Order matters: child rows before parents.
        entityManager.createNativeQuery("DELETE FROM vp_vehicle_operations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM vp_rotation_entries").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM vp_vehicles").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM vp_rotation_sets").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM pm_journey_locations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM pm_train_versions").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM pm_process_steps").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM pm_paths").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM pm_path_requests").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM pm_routes").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM pm_reference_trains").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM timetable_archives").executeUpdate();
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

        OrderPosition position = new OrderPosition();
        position.setOrder(order);
        position.setType(PositionType.FAHRPLAN);
        position.setName(positionName(train));
        position.setOperationalTrainNumber(train.getOperationalTrainNumber());
        position.setPmReferenceTrainId(train.getId());
        if (!locations.isEmpty()) {
            position.setFromLocation(locations.get(0).getPrimaryLocationName());
            position.setToLocation(locations.get(locations.size() - 1).getPrimaryLocationName());
        }
        if (train.getCalendarStart() != null) {
            position.setStart(train.getCalendarStart().atStartOfDay());
        }
        if (train.getCalendarEnd() != null) {
            position.setEnd(train.getCalendarEnd().atStartOfDay());
        }
        if (train.getCalendarStart() != null && train.getCalendarEnd() != null) {
            // Mock: treat the whole calendar window as operating days so validity-based features
            // (purchase calendar) are populated.
            position.setValidity(
                    ValidityJsonCodec.toJson(
                            train.getCalendarStart()
                                    .datesUntil(train.getCalendarEnd().plusDays(1))
                                    .toList()));
        }

        OrderPosition saved = orderPositionRepository.save(position);
        // Enrich with a timetable archive + CAPACITY need so the captured position has a viewable
        // timetable and is TTT-orderable (not just a thin OTN/route stub).
        enrichWithTimetableArchive(saved, locations);
        train.setSourcePositionId(saved.getId());
        referenceTrainRepository.save(train);
        return saved;
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
        List<TimetableRowData> rows = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            PmJourneyLocation loc = locations.get(i);
            TimetableRowData row = new TimetableRowData();
            row.setSequence(loc.getSequence() != null ? loc.getSequence() : i + 1);
            row.setUopid(loc.getUopid());
            row.setName(loc.getPrimaryLocationName());
            row.setCountry(loc.getCountryCodeIso());
            if (loc.getJourneyLocationType() != null) {
                row.setJourneyLocationType(loc.getJourneyLocationType());
            }
            row.setEstimatedArrival(loc.getArrivalTime());
            row.setEstimatedDeparture(loc.getDepartureTime());
            row.setCommercialArrival(loc.getArrivalTime());
            row.setCommercialDeparture(loc.getDepartureTime());
            rows.add(row);
        }

        TimetableArchive archive = new TimetableArchive();
        archive.setTimetableType("FAHRPLAN");
        archive.setOperationalTrainNumber(position.getOperationalTrainNumber());
        archive.setRouteSummary(rows.getFirst().getName() + " → " + rows.getLast().getName());
        archive.setTableData(writeRows(rows));
        archive = timetableArchiveRepository.save(archive);

        ResourceNeed need = new ResourceNeed();
        need.setOrderPosition(position);
        need.setResourceType(ResourceType.CAPACITY);
        need.setCoverageType(CoverageType.EXTERNAL);
        need.setQuantity(1);
        need.setOrigin(ResourceOrigin.AUTO);
        need.setPriority(ResourcePriority.MEDIUM);
        need.setValidFrom(position.getStart() != null ? position.getStart().toLocalDate() : null);
        need.setValidTo(position.getEnd() != null ? position.getEnd().toLocalDate() : null);
        need.setLinkedFahrplanId(archive.getId());
        resourceNeedRepository.save(need);
    }

    private String writeRows(List<TimetableRowData> rows) {
        try {
            return OBJECT_MAPPER.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize captured timetable rows", e);
        }
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

        PmRoute route = createRouteFromRows(train, rows);
        routeRepository.save(route);

        PmTrainVersion initialVersion = createInitialVersion(train, rows);
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

        // Update route points
        List<PmRoute> routes = routeRepository.findByReferenceTrainId(train.getId());
        if (!routes.isEmpty()) {
            PmRoute route = routes.getFirst();
            route.setRoutePoints(routePointsToJson(rows));
            routeRepository.save(route);
        }

        // Replace journey locations on the latest version
        List<PmTrainVersion> versions =
                trainVersionRepository.findByReferenceTrainIdOrderByVersionNumberDesc(
                        train.getId());
        if (!versions.isEmpty()) {
            PmTrainVersion version = versions.getLast();
            journeyLocationRepository.deleteAll(version.getJourneyLocations());
            version.getJourneyLocations().clear();
            Map<Integer, TttJourneyLocationDraft> draftBySequence =
                    tttDraftBuilder.indexBySequence(tttDraftBuilder.fromRows(rows));
            for (TimetableRowData row : rows) {
                PmJourneyLocation location =
                        mapRowToJourneyLocation(
                                row, version, draftBySequence.get(row.getSequence()));
                version.getJourneyLocations().add(location);
            }
            trainVersionRepository.save(version);
        }

        return referenceTrainRepository.save(train);
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
        int year = resolveYear(position);
        return timetableYearRepository.findByYear(year).orElseGet(() -> createTimetableYear(year));
    }

    private int resolveYear(OrderPosition position) {
        if (position.getStart() != null) {
            LocalDate startDate = position.getStart().toLocalDate();
            // Timetable year starts mid-December of previous year
            if (startDate.getMonthValue() == 12 && startDate.getDayOfMonth() >= 14) {
                return startDate.getYear() + 1;
            }
            return startDate.getYear();
        }
        return LocalDate.now().getYear();
    }

    private PmTimetableYear createTimetableYear(int year) {
        PmTimetableYear ttYear = new PmTimetableYear();
        ttYear.setYear(year);
        ttYear.setLabel("Fahrplanjahr " + year);
        ttYear.setStartDate(LocalDate.of(year - 1, 12, 14));
        ttYear.setEndDate(LocalDate.of(year, 12, 12));
        return timetableYearRepository.save(ttYear);
    }

    private PmRoute createRouteFromRows(PmReferenceTrain train, List<TimetableRowData> rows) {
        PmRoute route = new PmRoute();
        route.setReferenceTrain(train);
        route.setRoidCompany(identifierGenerator.company());
        route.setRoidCore(identifierGenerator.generateCore());
        route.setRoidVariant(identifierGenerator.initialVariant());
        route.setRoidTimetableYear(train.getTridTimetableYear());
        route.setRoutePoints(routePointsToJson(rows));
        return route;
    }

    private String routePointsToJson(List<TimetableRowData> rows) {
        try {
            List<Map<String, Object>> points = new ArrayList<>();
            for (TimetableRowData row : rows) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("sequence", row.getSequence());
                point.put("uopid", row.getUopid() != null ? row.getUopid() : "");
                point.put("name", row.getName() != null ? row.getName() : "");
                points.add(point);
            }
            return OBJECT_MAPPER.writeValueAsString(points);
        } catch (Exception e) {
            return "[]";
        }
    }

    private PmTrainVersion createInitialVersion(
            PmReferenceTrain train, List<TimetableRowData> rows) {
        PmTrainVersion version = new PmTrainVersion();
        version.setReferenceTrain(train);
        version.setVersionNumber(1);
        version.setVersionType(VersionType.INITIAL);
        version.setLabel(INITIAL_VERSION_LABEL);
        version.setOperationalTrainNumber(train.getOperationalTrainNumber());
        version.setTrainType(train.getTrainType());
        version.setTrafficTypeCode(train.getTrafficTypeCode());
        version.setTrainWeight(train.getTrainWeight());
        version.setTrainLength(train.getTrainLength());
        version.setTrainMaxSpeed(train.getTrainMaxSpeed());
        version.setCalendarStart(train.getCalendarStart());
        version.setCalendarEnd(train.getCalendarEnd());
        version.setCalendarBitmap(train.getCalendarBitmap());

        TttPathRequestDraft draft = tttDraftBuilder.fromRows(rows);
        Map<Integer, TttJourneyLocationDraft> draftBySequence =
                tttDraftBuilder.indexBySequence(draft);
        for (TimetableRowData row : rows) {
            TttJourneyLocationDraft locationDraft = draftBySequence.get(row.getSequence());
            PmJourneyLocation location = mapRowToJourneyLocation(row, version, locationDraft);
            version.getJourneyLocations().add(location);
        }

        return version;
    }

    private PmJourneyLocation mapRowToJourneyLocation(
            TimetableRowData row, PmTrainVersion version, TttJourneyLocationDraft draft) {
        PmJourneyLocation location = new PmJourneyLocation();
        location.setTrainVersion(version);
        location.setSequence(row.getSequence());
        location.setPrimaryLocationName(row.getName());
        location.setUopid(row.getUopid());
        location.setCountryCodeIso(row.getCountry());
        location.setJourneyLocationType(
                JourneyLocationType.fromString(row.getJourneyLocationType()).code());

        // Times: prefer constraint times, fall back to estimated
        location.setArrivalTime(resolveTime(row, true));
        location.setDepartureTime(resolveTime(row, false));
        location.setArrivalOffset(row.getArrivalOffset() == null ? 0 : row.getArrivalOffset());
        location.setDepartureOffset(
                row.getDepartureOffset() == null ? 0 : row.getDepartureOffset());
        location.setDwellTime(row.getDwellMinutes());

        // Timing qualifiers from constraint mode
        location.setArrivalQualifier(toTttQualifier(row.getArrivalMode(), true));
        location.setDepartureQualifier(toTttQualifier(row.getDepartureMode(), false));

        // Activity codes → activities JSON (preserve all selected codes, not just the first)
        List<String> activityCodes =
                row.getActivityCodes() != null && !row.getActivityCodes().isEmpty()
                        ? row.getActivityCodes()
                        : (row.getActivityCode() != null && !row.getActivityCode().isBlank()
                                ? List.of(row.getActivityCode())
                                : List.of());
        if (!activityCodes.isEmpty()) {
            location.setActivities(
                    activityCodes.stream()
                            .map(c -> "\"" + c + "\"")
                            .collect(java.util.stream.Collectors.joining(",", "[", "]")));
        }

        location.setAssociatedTrainOtn(row.getAssociatedTrainOtn());
        location.setTttPayload(writeTttPayload(draft));
        return location;
    }

    private String writeTttPayload(TttJourneyLocationDraft draft) {
        if (draft == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(draft);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize TTT draft payload.", exception);
        }
    }

    private String resolveTime(TimetableRowData row, boolean arrival) {
        if (arrival) {
            TimeConstraintMode mode =
                    row.getArrivalMode() == null ? TimeConstraintMode.NONE : row.getArrivalMode();
            if (mode == TimeConstraintMode.EXACT) {
                return row.getArrivalExact();
            }
            if (mode == TimeConstraintMode.WINDOW || mode == TimeConstraintMode.AFTER) {
                return row.getArrivalEarliest();
            }
            if (mode == TimeConstraintMode.BEFORE) {
                return row.getArrivalLatest();
            }
            if (mode == TimeConstraintMode.COMMERCIAL) {
                return row.getCommercialArrival();
            }
            return row.getEstimatedArrival();
        }
        TimeConstraintMode mode =
                row.getDepartureMode() == null ? TimeConstraintMode.NONE : row.getDepartureMode();
        if (mode == TimeConstraintMode.EXACT) {
            return row.getDepartureExact();
        }
        if (mode == TimeConstraintMode.WINDOW || mode == TimeConstraintMode.AFTER) {
            return row.getDepartureEarliest();
        }
        if (mode == TimeConstraintMode.BEFORE) {
            return row.getDepartureLatest();
        }
        if (mode == TimeConstraintMode.COMMERCIAL) {
            return row.getCommercialDeparture();
        }
        return row.getEstimatedDeparture();
    }

    /** Maps a TimeConstraintMode to a TTT TimingQualifierCode. */
    private String toTttQualifier(TimeConstraintMode mode, boolean arrival) {
        if (mode == null || mode == TimeConstraintMode.NONE) {
            return null;
        }
        return switch (mode) {
            case EXACT -> arrival ? "ALA" : "ALD";
            case WINDOW -> arrival ? "ELA" : "ELD";
            case AFTER -> arrival ? "ELA" : "ELD";
            case BEFORE -> arrival ? "LLA" : "LLD";
            case COMMERCIAL -> arrival ? "PLA" : "PLD";
            case NONE -> null;
        };
    }
}

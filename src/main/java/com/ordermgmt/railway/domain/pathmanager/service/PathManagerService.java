package com.ordermgmt.railway.domain.pathmanager.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
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

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Wipes <em>all</em> Path Manager state (trains, versions, journey locations, paths,
     * routes, process steps), the depending Vehicle Planning rotations, and the
     * timetable archive entries linked to FAHRPLAN positions. Intended as a "reset"
     * shortcut while the path manager is still a mock — the upstream system would not
     * permit this in production.
     *
     * <p>Will fail if any current OrderPosition still references a timetable archive
     * (FAHRPLAN positions); in that case the user should delete those positions first.
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
     * Syncs timetable data to the path manager. Creates a new reference train if none exists for the
     * position, or updates the existing one's journey locations.
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
            for (TimetableRowData row : rows) {
                PmJourneyLocation location = mapRowToJourneyLocation(row, version);
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

        for (TimetableRowData row : rows) {
            PmJourneyLocation location = mapRowToJourneyLocation(row, version);
            version.getJourneyLocations().add(location);
        }

        return version;
    }

    private PmJourneyLocation mapRowToJourneyLocation(
            TimetableRowData row, PmTrainVersion version) {
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
        location.setDwellTime(row.getDwellMinutes());

        // Timing qualifiers from constraint mode
        location.setArrivalQualifier(toTttQualifier(row.getArrivalMode(), true));
        location.setDepartureQualifier(toTttQualifier(row.getDepartureMode(), false));

        // Activity code → activities JSON
        if (row.getActivityCode() != null && !row.getActivityCode().isBlank()) {
            location.setActivities("[\"" + row.getActivityCode() + "\"]");
        }

        location.setAssociatedTrainOtn(row.getAssociatedTrainOtn());
        return location;
    }

    private String resolveTime(TimetableRowData row, boolean arrival) {
        if (arrival) {
            if (row.getArrivalMode() == TimeConstraintMode.EXACT
                    && row.getArrivalExact() != null) {
                return row.getArrivalExact();
            }
            return row.getEstimatedArrival();
        }
        if (row.getDepartureMode() == TimeConstraintMode.EXACT
                && row.getDepartureExact() != null) {
            return row.getDepartureExact();
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
            case COMMERCIAL -> arrival ? "PLA" : "PLD";
            default -> null;
        };
    }
}

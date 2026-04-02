package com.ordermgmt.railway.domain.pathmanager.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.pathmanager.model.PathProcessState;
import com.ordermgmt.railway.domain.pathmanager.model.PmJourneyLocation;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmRoute;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.VersionType;
import com.ordermgmt.railway.domain.pathmanager.repository.PmJourneyLocationRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmReferenceTrainRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmRouteRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTimetableYearRepository;
import com.ordermgmt.railway.domain.pathmanager.repository.PmTrainVersionRepository;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

import lombok.RequiredArgsConstructor;

/** CRUD and mapping operations for the Path Manager domain. */
@Service
@RequiredArgsConstructor
@Transactional
public class PathManagerService {

    private final PmReferenceTrainRepository referenceTrainRepository;
    private final PmTrainVersionRepository trainVersionRepository;
    private final PmJourneyLocationRepository journeyLocationRepository;
    private final PmRouteRepository routeRepository;
    private final PmTimetableYearRepository timetableYearRepository;
    private final IdentifierGenerator identifierGenerator;

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

    public void updateTrainHeader(
            UUID trainId,
            String operationalTrainNumber,
            String trainType,
            String trafficTypeCode,
            Integer trainWeight,
            Integer trainLength,
            Integer trainMaxSpeed,
            String brakeType) {
        PmReferenceTrain train = findById(trainId);
        train.setOperationalTrainNumber(operationalTrainNumber);
        train.setTrainType(trainType);
        train.setTrafficTypeCode(trafficTypeCode);
        train.setTrainWeight(trainWeight);
        train.setTrainLength(trainLength);
        train.setTrainMaxSpeed(trainMaxSpeed);
        train.setBrakeType(brakeType);
        referenceTrainRepository.save(train);
    }

    public void updateJourneyLocation(
            UUID locationId,
            String arrivalTime,
            String departureTime,
            Integer dwellTime,
            String arrivalQualifier,
            String departureQualifier,
            String activities) {
        PmJourneyLocation location =
                journeyLocationRepository
                        .findById(locationId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Journey location not found: " + locationId));
        location.setArrivalTime(arrivalTime);
        location.setDepartureTime(departureTime);
        location.setDwellTime(dwellTime);
        location.setArrivalQualifier(arrivalQualifier);
        location.setDepartureQualifier(departureQualifier);
        location.setActivities(activities);
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

        StringBuilder routePointsJson = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            TimetableRowData row = rows.get(i);
            if (i > 0) {
                routePointsJson.append(",");
            }
            routePointsJson
                    .append("{\"sequence\":")
                    .append(row.getSequence())
                    .append(",\"uopid\":\"")
                    .append(row.getUopid() != null ? row.getUopid() : "")
                    .append("\",\"name\":\"")
                    .append(row.getName() != null ? row.getName() : "")
                    .append("\"}");
        }
        routePointsJson.append("]");
        route.setRoutePoints(routePointsJson.toString());

        return route;
    }

    private PmTrainVersion createInitialVersion(
            PmReferenceTrain train, List<TimetableRowData> rows) {
        PmTrainVersion version = new PmTrainVersion();
        version.setReferenceTrain(train);
        version.setVersionNumber(1);
        version.setVersionType(VersionType.INITIAL);
        version.setLabel("Initial v1");
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
        location.setJourneyLocationType(mapLocationType(row.getJourneyLocationType()));
        location.setArrivalTime(row.getEstimatedArrival());
        location.setDepartureTime(row.getEstimatedDeparture());
        location.setDwellTime(row.getDwellMinutes());
        return location;
    }

    private String mapLocationType(String journeyLocationType) {
        if (journeyLocationType == null) {
            return "02";
        }
        return switch (journeyLocationType.toUpperCase()) {
            case "ORIGIN" -> "01";
            case "INTERMEDIATE" -> "02";
            case "DESTINATION" -> "03";
            case "HANDOVER" -> "04";
            case "INTERCHANGE" -> "05";
            default -> "02";
        };
    }
}

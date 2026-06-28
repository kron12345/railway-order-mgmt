package com.ordermgmt.railway.domain.pathmanager.mapping;

import java.time.LocalDate;
import java.util.List;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.model.PmRoute;
import com.ordermgmt.railway.domain.pathmanager.model.PmTimetableYear;
import com.ordermgmt.railway.domain.pathmanager.model.PmTrainVersion;
import com.ordermgmt.railway.domain.pathmanager.model.VersionType;
import com.ordermgmt.railway.domain.pathmanager.service.IdentifierGenerator;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TttDraftBuilder;

/**
 * Constructs the Path-Manager entity graph (timetable year, route, initial train version with its
 * journey locations) from an order position and its timetable rows. The injected collaborators
 * ({@link IdentifierGenerator}, {@link TttDraftBuilder}) are passed in as parameters so this stays
 * a stateless helper alongside {@link PmTimetableTranslator}; {@code PathManagerService} owns the
 * beans and the persistence.
 */
public final class PmTrainAssembler {

    private static final String INITIAL_VERSION_LABEL = "Initial v1";
    private static final int FIRST_VERSION_NUMBER = 1;
    private static final int TIMETABLE_YEAR_START_MONTH = 12;
    private static final int TIMETABLE_YEAR_START_DAY = 14;
    private static final int TIMETABLE_YEAR_END_MONTH = 12;
    private static final int TIMETABLE_YEAR_END_DAY = 12;

    private PmTrainAssembler() {}

    /** Builds a {@link PmRoute} (unsaved) from the train and its timetable rows. */
    public static PmRoute createRoute(
            PmReferenceTrain train,
            List<TimetableRowData> rows,
            IdentifierGenerator identifierGenerator) {
        PmRoute route = new PmRoute();
        route.setReferenceTrain(train);
        route.setRoidCompany(identifierGenerator.company());
        route.setRoidCore(identifierGenerator.generateCore());
        route.setRoidVariant(identifierGenerator.initialVariant());
        route.setRoidTimetableYear(train.getTridTimetableYear());
        route.setRoutePoints(PmTimetableTranslator.routePointsToJson(rows));
        return route;
    }

    /** Builds the initial {@link PmTrainVersion} (unsaved) with its journey locations. */
    public static PmTrainVersion createInitialVersion(
            PmReferenceTrain train, List<TimetableRowData> rows, TttDraftBuilder tttDraftBuilder) {
        PmTrainVersion version = new PmTrainVersion();
        version.setReferenceTrain(train);
        version.setVersionNumber(FIRST_VERSION_NUMBER);
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
        version.getJourneyLocations()
                .addAll(
                        PmTimetableTranslator.buildJourneyLocations(
                                rows, version, tttDraftBuilder));
        return version;
    }

    /**
     * Resolves the timetable year for a position: the start date's year, rolled to the next year
     * once the new timetable period has begun (on/after 14 December). Falls back to the current
     * year.
     */
    public static int resolveYear(OrderPosition position) {
        if (position.getStart() != null) {
            LocalDate startDate = position.getStart().toLocalDate();
            if (startDate.getMonthValue() == TIMETABLE_YEAR_START_MONTH
                    && startDate.getDayOfMonth() >= TIMETABLE_YEAR_START_DAY) {
                return startDate.getYear() + 1;
            }
            return startDate.getYear();
        }
        return LocalDate.now().getYear();
    }

    /** Builds a {@link PmTimetableYear} (unsaved) with its start/end window for the given year. */
    public static PmTimetableYear newTimetableYear(int year) {
        PmTimetableYear timetableYear = new PmTimetableYear();
        timetableYear.setYear(year);
        timetableYear.setLabel("Fahrplanjahr " + year);
        timetableYear.setStartDate(
                LocalDate.of(year - 1, TIMETABLE_YEAR_START_MONTH, TIMETABLE_YEAR_START_DAY));
        timetableYear.setEndDate(
                LocalDate.of(year, TIMETABLE_YEAR_END_MONTH, TIMETABLE_YEAR_END_DAY));
        return timetableYear;
    }
}

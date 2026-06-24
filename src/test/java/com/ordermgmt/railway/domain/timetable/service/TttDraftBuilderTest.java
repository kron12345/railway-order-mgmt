package com.ordermgmt.railway.domain.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

class TttDraftBuilderTest {

    private final TttDraftBuilder builder = new TttDraftBuilder();

    @Test
    void fromRows_windowDepartureCreatesEldAndLldTimings() {
        TimetableRowData origin = origin();
        origin.setDepartureMode(TimeConstraintMode.WINDOW);
        origin.setDepartureEarliest("10:00");
        origin.setDepartureLatest("10:15");
        origin.setDepartureOffset(1);
        origin.setUserEnteredDepartureEarliest(true);
        origin.setUserEnteredDepartureLatest(true);

        var draft = builder.fromRows(List.of(origin, destination()));

        assertThat(draft.journeyLocations().getFirst().timings())
                .extracting("qualifierCode", "time", "offset")
                .containsExactly(tuple("ELD", "10:00", 1), tuple("LLD", "10:15", 1));
    }

    @Test
    void fromRows_afterDepartureCreatesSingleEldTiming() {
        TimetableRowData origin = origin();
        origin.setDepartureMode(TimeConstraintMode.AFTER);
        origin.setDepartureEarliest("10:00");
        origin.setUserEnteredDepartureEarliest(true);

        var draft = builder.fromRows(List.of(origin, destination()));

        assertThat(draft.journeyLocations().getFirst().timings())
                .extracting("qualifierCode", "time", "offset")
                .containsExactly(tuple("ELD", "10:00", 0));
    }

    @Test
    void fromRows_mapsUiOnlyTttFieldsIntoDraftPayload() {
        TimetableRowData border = new TimetableRowData();
        border.setSequence(2);
        border.setName("Basel Grenze");
        border.setUopid("CH00002");
        border.setCountry("CHE");
        border.setJourneyLocationType("NETWORK_BORDER");
        border.setTttRelevant(true);
        border.setActivityCodes(List.of("0040", "CH11"));
        border.setLocationSubsidiaryCode("TRACK-7");
        border.setNetworkSpecificParametersText("isb=SBB\npriority=pilot");

        var draft = builder.fromRows(List.of(origin(), border, destination()));
        var location = draft.journeyLocations().get(1);

        assertThat(location.exportedToTtt()).isTrue();
        assertThat(location.journeyLocationTypeCode()).isEqualTo("09");
        assertThat(location.trainActivityTypes()).containsExactly("0040", "CH11");
        assertThat(location.locationSubsidiaryCode()).isEqualTo("TRACK-7");
        assertThat(location.networkSpecificParameters())
                .containsEntry("isb", "SBB")
                .containsEntry("priority", "pilot");
    }

    private TimetableRowData origin() {
        TimetableRowData row = new TimetableRowData();
        row.setSequence(1);
        row.setName("Leipzig Hbf");
        row.setUopid("DE00001");
        row.setCountry("DEU");
        row.setRoutePointRole(RoutePointRole.ORIGIN);
        row.setJourneyLocationType("ORIGIN");
        return row;
    }

    private TimetableRowData destination() {
        TimetableRowData row = new TimetableRowData();
        row.setSequence(2);
        row.setName("Karlsruhe Hbf");
        row.setUopid("DE00002");
        row.setCountry("DEU");
        row.setRoutePointRole(RoutePointRole.DESTINATION);
        row.setJourneyLocationType("DESTINATION");
        return row;
    }
}

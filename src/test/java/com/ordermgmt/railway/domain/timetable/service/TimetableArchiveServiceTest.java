package com.ordermgmt.railway.domain.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimeConstraintMode;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.repository.TimetableArchiveRepository;

@DataJpaTest
@Import(TimetableArchiveService.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TimetableArchiveServiceTest {

    @Autowired private TimetableArchiveService timetableArchiveService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderPositionRepository orderPositionRepository;
    @Autowired private TimetableArchiveRepository timetableArchiveRepository;

    @MockitoBean private PathManagerService pathManagerService;

    @Test
    void savesArchiveAndLinkedCapacityNeedForTimetablePosition() {
        Instant now = Instant.parse("2026-03-31T16:15:00Z");

        Order order = new Order();
        order.setOrderNumber("TEST-FAHRPLAN-001");
        order.setName("Persistenztest Fahrplan");
        order.setValidFrom(LocalDate.of(2026, 4, 1));
        order.setValidTo(LocalDate.of(2026, 4, 5));
        order.setProcessStatus(ProcessStatus.AUFTRAG);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order = orderRepository.saveAndFlush(order);

        TimetableRowData origin = new TimetableRowData();
        origin.setSequence(1);
        origin.setUopid("CH00001");
        origin.setName("Basel SBB");
        origin.setCountry("CHE");
        origin.setRoutePointRole(RoutePointRole.ORIGIN);
        origin.setJourneyLocationType("ORIGIN");
        origin.setToName("Olten");
        origin.setEstimatedDeparture("10:15");

        TimetableRowData destination = new TimetableRowData();
        destination.setSequence(2);
        destination.setUopid("CH00002");
        destination.setName("Olten");
        destination.setCountry("CHE");
        destination.setRoutePointRole(RoutePointRole.DESTINATION);
        destination.setJourneyLocationType("DESTINATION");
        destination.setFromName("Basel SBB");
        destination.setSegmentLengthMeters(54_000D);
        destination.setDistanceFromStartMeters(54_000D);
        destination.setArrivalMode(TimeConstraintMode.EXACT);
        destination.setArrivalExact("11:02");
        destination.setEstimatedArrival("11:02");

        var saved =
                timetableArchiveService.saveTimetablePosition(
                        order,
                        null,
                        "Basel - Olten",
                        "Trasse, Test",
                        "Archivtest",
                        List.of(LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3)),
                        List.of(origin, destination),
                        "95345");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getType()).isEqualTo(PositionType.FAHRPLAN);
        assertThat(saved.getFromLocation()).isEqualTo("Basel SBB");
        assertThat(saved.getToLocation()).isEqualTo("Olten");
        assertThat(saved.getStart()).isEqualTo(LocalDate.of(2026, 4, 2).atTime(10, 15));
        assertThat(saved.getEnd()).isEqualTo(LocalDate.of(2026, 4, 3).atTime(11, 2));
        assertThat(saved.getResourceNeeds())
                .hasSize(1)
                .allSatisfy(
                        need ->
                                assertThat(need.getResourceType())
                                        .isEqualTo(ResourceType.CAPACITY));

        var persisted = orderPositionRepository.findById(saved.getId()).orElseThrow();
        assertThat(persisted.getResourceNeeds()).hasSize(1);
        assertThat(persisted.getResourceNeeds().getFirst().getLinkedFahrplanId()).isNotNull();

        TimetableArchive archive =
                timetableArchiveRepository
                        .findById(persisted.getResourceNeeds().getFirst().getLinkedFahrplanId())
                        .orElseThrow();
        assertThat(archive.getRouteSummary()).isEqualTo("Basel SBB → Olten");
        assertThat(archive.getTableData()).contains("Basel SBB").contains("Olten");
    }

    @Test
    void savesOriginDepartureAfterAsSingleTttHalfWindow() {
        Order order = persistOrder("TEST-FAHRPLAN-AFTER");

        TimetableRowData origin = origin("Leipzig Hbf");
        origin.setDepartureMode(TimeConstraintMode.AFTER);
        origin.setDepartureEarliest("10:00");
        origin.setUserEnteredDepartureEarliest(true);

        TimetableRowData destination = destination("Karlsruhe Hbf");
        destination.setArrivalMode(TimeConstraintMode.EXACT);
        destination.setArrivalExact("15:20");
        destination.setEstimatedArrival("15:20");

        var saved =
                timetableArchiveService.saveTimetablePosition(
                        order,
                        null,
                        "Leipzig - Karlsruhe",
                        "Trasse, Test",
                        "AFTER half-window",
                        List.of(LocalDate.of(2026, 4, 2)),
                        List.of(origin, destination),
                        "95346");

        assertThat(saved.getStart()).isEqualTo(LocalDate.of(2026, 4, 2).atTime(10, 0));

        TimetableArchive archive =
                timetableArchiveRepository
                        .findById(saved.getResourceNeeds().getFirst().getLinkedFahrplanId())
                        .orElseThrow();
        assertThat(archive.getTableData())
                .contains("\"departureMode\":\"AFTER\"")
                .contains("\"departureEarliest\":\"10:00\"");
    }

    @Test
    void rejectsAfterModeWithoutEarliestTime() {
        Order order = persistOrder("TEST-FAHRPLAN-AFTER-MISSING");

        TimetableRowData origin = origin("Leipzig Hbf");
        origin.setDepartureMode(TimeConstraintMode.AFTER);

        TimetableRowData destination = destination("Karlsruhe Hbf");
        destination.setEstimatedArrival("15:20");

        assertThatThrownBy(
                        () ->
                                timetableArchiveService.saveTimetablePosition(
                                        order,
                                        null,
                                        "Leipzig - Karlsruhe",
                                        null,
                                        null,
                                        List.of(LocalDate.of(2026, 4, 2)),
                                        List.of(origin, destination),
                                        "95347"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Earliest timetable time is missing.");
    }

    private Order persistOrder(String orderNumber) {
        Instant now = Instant.parse("2026-03-31T16:15:00Z");

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setName("Persistenztest Fahrplan");
        order.setValidFrom(LocalDate.of(2026, 4, 1));
        order.setValidTo(LocalDate.of(2026, 4, 5));
        order.setProcessStatus(ProcessStatus.AUFTRAG);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return orderRepository.saveAndFlush(order);
    }

    private TimetableRowData origin(String name) {
        TimetableRowData origin = new TimetableRowData();
        origin.setSequence(1);
        origin.setUopid("DE00001");
        origin.setName(name);
        origin.setCountry("DEU");
        origin.setRoutePointRole(RoutePointRole.ORIGIN);
        origin.setJourneyLocationType("ORIGIN");
        origin.setToName("Karlsruhe Hbf");
        return origin;
    }

    private TimetableRowData destination(String name) {
        TimetableRowData destination = new TimetableRowData();
        destination.setSequence(2);
        destination.setUopid("DE00002");
        destination.setName(name);
        destination.setCountry("DEU");
        destination.setRoutePointRole(RoutePointRole.DESTINATION);
        destination.setJourneyLocationType("DESTINATION");
        destination.setFromName("Leipzig Hbf");
        destination.setSegmentLengthMeters(500_000D);
        destination.setDistanceFromStartMeters(500_000D);
        return destination;
    }
}

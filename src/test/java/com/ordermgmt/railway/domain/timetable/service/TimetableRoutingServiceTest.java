package com.ordermgmt.railway.domain.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.model.SectionOfLine;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.SectionOfLineRepository;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;

@ExtendWith(MockitoExtension.class)
class TimetableRoutingServiceTest {

    @Mock private OperationalPointRepository operationalPointRepository;
    @Mock private SectionOfLineRepository sectionOfLineRepository;

    @Test
    void choosesShortestPathAcrossSectionsOfLine() {
        OperationalPoint basel = point("CH00001", "Basel SBB", 47.5476, 7.5896);
        OperationalPoint olten = point("CH00002", "Olten", 47.3517, 7.9077);
        OperationalPoint bern = point("CH00003", "Bern", 46.9480, 7.4391);

        when(operationalPointRepository.findAll()).thenReturn(List.of(basel, olten, bern));
        when(sectionOfLineRepository.findAll())
                .thenReturn(List.of(
                        section("SOL-1", "CH00001", "CH00002", 54_000D),
                        section("SOL-2", "CH00002", "CH00003", 40_000D),
                        section("SOL-3", "CH00001", "CH00003", 140_000D)));

        TimetableRoutingService service =
                new TimetableRoutingService(operationalPointRepository, sectionOfLineRepository);

        var route = service.calculateRoute(List.of(basel, bern));

        assertThat(route.points()).extracting(point -> point.name()).containsExactly("Basel SBB", "Olten", "Bern");
        assertThat(route.points()).extracting(point -> point.routePointRole())
                .containsExactly(RoutePointRole.ORIGIN, RoutePointRole.AUTO, RoutePointRole.DESTINATION);
        assertThat(route.totalLengthMeters()).isEqualTo(94_000D);

        var rows = service.estimateRows(route, LocalTime.of(10, 0), null);
        assertThat(rows).hasSize(3);
        assertThat(rows.getFirst().getEstimatedDeparture()).isEqualTo("10:00");
        assertThat(rows.getLast().getEstimatedArrival()).isNotBlank();
    }

    private OperationalPoint point(String uopid, String name, double latitude, double longitude) {
        OperationalPoint point = new OperationalPoint();
        point.setUopid(uopid);
        point.setName(name);
        point.setCountry("CHE");
        point.setLatitude(latitude);
        point.setLongitude(longitude);
        return point;
    }

    private SectionOfLine section(String id, String start, String end, double length) {
        SectionOfLine section = new SectionOfLine();
        section.setSolId(id);
        section.setStartOpUopid(start);
        section.setEndOpUopid(end);
        section.setCountry("CHE");
        section.setLengthMeters(length);
        return section;
    }
}

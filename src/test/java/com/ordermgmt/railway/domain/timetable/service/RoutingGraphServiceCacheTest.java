package com.ordermgmt.railway.domain.timetable.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.ordermgmt.railway.domain.infrastructure.event.RinfDataImportedEvent;
import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.model.SectionOfLine;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.SectionOfLineRepository;

/**
 * Proves that the routing-graph cache is actually reached through the Spring proxy. Before the fix,
 * the {@code @Cacheable} methods lived on {@link TimetableRoutingService} and were only ever called
 * via self-invocation, so the cache was silently bypassed and the graph rebuilt on every route
 * calculation. With the cached methods in their own bean ({@link RoutingGraphService}), repeated
 * route calculations must reuse the cached graph + point lookup.
 */
@SpringJUnitConfig(RoutingGraphServiceCacheTest.CacheConfig.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class RoutingGraphServiceCacheTest {

    @Autowired private OperationalPointRepository operationalPointRepository;
    @Autowired private SectionOfLineRepository sectionOfLineRepository;
    @Autowired private TimetableRoutingService timetableRoutingService;
    @Autowired private RoutingGraphService routingGraphService;
    @Autowired private ApplicationEventPublisher events;

    @Test
    void repeatedRouteCalculationsReuseTheCachedGraph() {
        OperationalPoint basel = point("CH00001", "Basel SBB");
        OperationalPoint olten = point("CH00002", "Olten");
        OperationalPoint bern = point("CH00003", "Bern");
        when(operationalPointRepository.findAll()).thenReturn(List.of(basel, olten, bern));
        when(sectionOfLineRepository.findAll())
                .thenReturn(
                        List.of(
                                section("SOL-1", "CH00001", "CH00002", 54_000D),
                                section("SOL-2", "CH00002", "CH00003", 40_000D)));

        timetableRoutingService.calculateRoute(List.of(basel, bern));
        timetableRoutingService.calculateRoute(List.of(basel, bern));

        // routingTopology cache hit on the 2nd call: the section + OP lists are each read once for
        // the single topology build, not per route.
        verify(sectionOfLineRepository, times(1)).findAll();
        verify(operationalPointRepository, times(1)).findAll();
    }

    @Test
    void rinfImportEvictsTheRoutingCaches() {
        OperationalPoint basel = point("CH00001", "Basel SBB");
        when(operationalPointRepository.findAll()).thenReturn(List.of(basel));
        when(sectionOfLineRepository.findAll()).thenReturn(List.of());

        // Warm the topology cache, then confirm the next load is served from the cache.
        routingGraphService.loadTopology();
        routingGraphService.loadTopology();
        verify(operationalPointRepository, times(1)).findAll();

        // A successful RINF import must invalidate the cached topology...
        events.publishEvent(new RinfDataImportedEvent("RINF_OP", "CHE"));

        // ...so the next load rebuilds from the repository.
        routingGraphService.loadTopology();
        verify(operationalPointRepository, times(2)).findAll();
    }

    private OperationalPoint point(String uopid, String name) {
        OperationalPoint point = new OperationalPoint();
        point.setUopid(uopid);
        point.setName(name);
        point.setCountry("CHE");
        point.setLatitude(47.0);
        point.setLongitude(7.0);
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

    @Configuration
    @EnableCaching
    static class CacheConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("routingTopology");
        }

        @Bean
        OperationalPointRepository operationalPointRepository() {
            return Mockito.mock(OperationalPointRepository.class);
        }

        @Bean
        SectionOfLineRepository sectionOfLineRepository() {
            return Mockito.mock(SectionOfLineRepository.class);
        }

        @Bean
        RoutingGraphService routingGraphService(
                OperationalPointRepository opRepo, SectionOfLineRepository solRepo) {
            return new RoutingGraphService(opRepo, solRepo);
        }

        @Bean
        TimetableRoutingService timetableRoutingService(
                OperationalPointRepository opRepo, RoutingGraphService graph) {
            return new TimetableRoutingService(opRepo, graph);
        }
    }
}

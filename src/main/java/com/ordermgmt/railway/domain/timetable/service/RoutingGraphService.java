package com.ordermgmt.railway.domain.timetable.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.infrastructure.event.RinfDataImportedEvent;
import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.model.SectionOfLine;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.SectionOfLineRepository;

import lombok.RequiredArgsConstructor;

/**
 * Caches the RINF routing topology — the operational-point lookup together with the JGraphT graph
 * built from it.
 *
 * <p>Kept in a separate Spring bean from {@link TimetableRoutingService} so the cache is reached
 * across a bean boundary: Spring's proxy-based caching does not intercept self-invocation, so
 * calling a {@code @Cacheable} method from within the same bean would silently rebuild the (large,
 * ~19k operational point) graph on every route calculation.
 *
 * <p>The point lookup and the graph are cached together as a single {@link RoutingTopology}
 * snapshot — not as two independent caches — so they can never diverge across an import/eviction:
 * the graph is always consistent with the point map it was built from.
 *
 * <p>Known limitation (accepted): like any evict-on-write cache, a cold rebuild that happens to
 * overlap an import commit can re-store the pre-import snapshot in a narrow window, so freshly
 * imported data may stay stale until the time-based eviction ({@code expireAfterWrite=5m}). Left
 * as-is on purpose — RINF imports are infrequent admin actions, the cache is normally warm, and the
 * condition self-heals within the TTL; closing it fully would require replacing the idiomatic
 * Spring cache with a hand-rolled generation/lock-guarded cache, not worth the added concurrency
 * complexity here.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutingGraphService {

    private static final Logger log = LoggerFactory.getLogger(RoutingGraphService.class);

    private final OperationalPointRepository operationalPointRepository;
    private final SectionOfLineRepository sectionOfLineRepository;

    /** Operational-point lookup plus the routing graph built from exactly those points. */
    public record RoutingTopology(
            Map<String, OperationalPoint> pointsByUopid,
            Graph<String, DefaultWeightedEdge> graph) {}

    /**
     * Invalidates the cached topology after a RINF import so the next route calculation rebuilds
     * from the freshly imported data instead of waiting for the time-based eviction. Spring
     * dispatches the event through this proxy, so {@code @CacheEvict} is honoured.
     */
    @EventListener
    @CacheEvict(value = "routingTopology", allEntries = true)
    public void onRinfDataImported(RinfDataImportedEvent event) {
        log.info(
                "Evicting routing topology cache after RINF import: {} ({})",
                event.dataset(),
                event.country());
    }

    /** Loads the routing topology (point lookup + graph) as one cached, consistent snapshot. */
    @Cacheable(value = "routingTopology", key = "'topology'")
    public RoutingTopology loadTopology() {
        Map<String, OperationalPoint> pointsByUopid = loadPointsByUopid();
        return new RoutingTopology(pointsByUopid, buildGraph(pointsByUopid));
    }

    private Map<String, OperationalPoint> loadPointsByUopid() {
        Map<String, OperationalPoint> pointsByUopid = new LinkedHashMap<>();
        for (OperationalPoint point : operationalPointRepository.findAll()) {
            pointsByUopid.put(point.getUopid(), point);
        }
        return pointsByUopid;
    }

    private Graph<String, DefaultWeightedEdge> buildGraph(
            Map<String, OperationalPoint> pointsByUopid) {
        SimpleWeightedGraph<String, DefaultWeightedEdge> graph =
                new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (String uopid : pointsByUopid.keySet()) {
            graph.addVertex(uopid);
        }

        for (SectionOfLine section : sectionOfLineRepository.findAll()) {
            if (!pointsByUopid.containsKey(section.getStartOpUopid())
                    || !pointsByUopid.containsKey(section.getEndOpUopid())) {
                continue;
            }
            // Skip self-loops — SimpleWeightedGraph does not allow them
            if (section.getStartOpUopid().equals(section.getEndOpUopid())) {
                continue;
            }
            double length = section.getLengthMeters() != null ? section.getLengthMeters() : 0D;
            DefaultWeightedEdge edge =
                    graph.addEdge(section.getStartOpUopid(), section.getEndOpUopid());
            if (edge != null) {
                graph.setEdgeWeight(edge, length);
            }
        }
        return graph;
    }
}

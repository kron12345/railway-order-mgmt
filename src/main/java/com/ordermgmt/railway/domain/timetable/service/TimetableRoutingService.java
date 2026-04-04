package com.ordermgmt.railway.domain.timetable.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.model.SectionOfLine;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.SectionOfLineRepository;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimetableRoutePoint;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;

import lombok.RequiredArgsConstructor;

/** Searches operational points and calculates shortest routes over sections of line. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableRoutingService {

    private static final int MAX_SEARCH_RESULTS = 50;
    private static final double ASSUMED_SPEED_METERS_PER_SECOND = 70_000D / 3_600D;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OperationalPointRepository operationalPointRepository;
    private final SectionOfLineRepository sectionOfLineRepository;

    public List<OperationalPoint> searchOperationalPoints(String filter, int offset, int limit) {
        int pageSize = Math.max(1, Math.min(limit, MAX_SEARCH_RESULTS));
        int page = Math.max(0, offset / pageSize);
        PageRequest pageable =
                PageRequest.of(page, pageSize, Sort.by("country").ascending().and(Sort.by("name")));
        if (filter == null || filter.isBlank()) {
            return operationalPointRepository.findAll(pageable).getContent();
        }
        String normalized = filter.trim();
        return operationalPointRepository
                .findByNameContainingIgnoreCaseOrUopidContainingIgnoreCase(
                        normalized, normalized, pageable)
                .getContent();
    }

    public int countOperationalPoints(String filter) {
        if (filter == null || filter.isBlank()) {
            return Math.toIntExact(operationalPointRepository.count());
        }
        String normalized = filter.trim();
        return Math.toIntExact(
                operationalPointRepository
                        .countByNameContainingIgnoreCaseOrUopidContainingIgnoreCase(
                                normalized, normalized));
    }

    public Optional<OperationalPoint> resolveLegacyPoint(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        Optional<OperationalPoint> exact =
                operationalPointRepository.findFirstByNameIgnoreCase(name.trim());
        if (exact.isPresent()) {
            return exact;
        }

        for (String variant : legacyNameVariants(name)) {
            List<OperationalPoint> matches =
                    operationalPointRepository.findByNameContainingIgnoreCase(variant);
            if (!matches.isEmpty()) {
                return matches.stream()
                        .sorted(
                                Comparator.comparing(
                                        OperationalPoint::getName, String.CASE_INSENSITIVE_ORDER))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    public Optional<OperationalPoint> findByUopid(String uopid) {
        if (uopid == null || uopid.isBlank()) {
            return Optional.empty();
        }
        return operationalPointRepository.findByUopid(uopid);
    }

    public TimetableRouteResult calculateRoute(List<OperationalPoint> orderedWaypoints) {
        if (orderedWaypoints == null || orderedWaypoints.size() < 2) {
            throw new IllegalArgumentException("At least origin and destination are required.");
        }

        Map<String, OperationalPoint> pointsByUopid = loadOperationalPointsByUopid();
        Graph<String, DefaultWeightedEdge> graph = buildGraph();

        List<String> fullPath = new ArrayList<>();
        List<Double> segmentLengths = new ArrayList<>();

        DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra =
                new DijkstraShortestPath<>(graph);

        for (int i = 0; i < orderedWaypoints.size() - 1; i++) {
            OperationalPoint start = orderedWaypoints.get(i);
            OperationalPoint end = orderedWaypoints.get(i + 1);
            PathSegment segment = shortestPath(start.getUopid(), end.getUopid(), graph, dijkstra);

            if (fullPath.isEmpty()) {
                fullPath.addAll(segment.nodeUopids());
                segmentLengths.addAll(segment.segmentLengths());
            } else {
                for (int nodeIndex = 1; nodeIndex < segment.nodeUopids().size(); nodeIndex++) {
                    fullPath.add(segment.nodeUopids().get(nodeIndex));
                    segmentLengths.add(segment.segmentLengths().get(nodeIndex));
                }
            }
        }

        Map<Integer, RoutePointRole> roles = explicitRoles(fullPath, orderedWaypoints);
        List<TimetableRoutePoint> points = new ArrayList<>();
        double distanceFromStart = 0D;
        for (int index = 0; index < fullPath.size(); index++) {
            String uopid = fullPath.get(index);
            OperationalPoint point = pointsByUopid.get(uopid);
            if (point == null) {
                continue;
            }

            double segmentLength = segmentLengths.get(index);
            if (index > 0) {
                distanceFromStart += segmentLength;
            }
            RoutePointRole role = roles.getOrDefault(index, RoutePointRole.AUTO);
            points.add(
                    new TimetableRoutePoint(
                            point.getUopid(),
                            point.getName(),
                            point.getCountry(),
                            point.getLatitude(),
                            point.getLongitude(),
                            segmentLength,
                            distanceFromStart,
                            role,
                            journeyLocationType(role),
                            index > 0 ? pointsByUopid.get(fullPath.get(index - 1)).getName() : null,
                            index < fullPath.size() - 1
                                    ? pointsByUopid.get(fullPath.get(index + 1)).getName()
                                    : null));
        }

        return new TimetableRouteResult(points, distanceFromStart);
    }

    public TimetableRouteResult routeFromStoredRows(List<TimetableRowData> rows) {
        if (rows == null || rows.isEmpty()) {
            return new TimetableRouteResult(List.of(), 0D);
        }

        Map<String, OperationalPoint> pointsByUopid = loadOperationalPointsByUopid();
        List<TimetableRoutePoint> points = new ArrayList<>();
        double totalDistance = 0D;
        for (TimetableRowData row : rows) {
            OperationalPoint point = pointsByUopid.get(row.getUopid());
            if (point == null) {
                continue;
            }

            double segmentLength =
                    row.getSegmentLengthMeters() != null ? row.getSegmentLengthMeters() : 0D;
            totalDistance =
                    row.getDistanceFromStartMeters() != null
                            ? row.getDistanceFromStartMeters()
                            : totalDistance + segmentLength;
            points.add(
                    new TimetableRoutePoint(
                            point.getUopid(),
                            point.getName(),
                            point.getCountry(),
                            point.getLatitude(),
                            point.getLongitude(),
                            segmentLength,
                            totalDistance,
                            row.getRoutePointRole(),
                            row.getJourneyLocationType(),
                            row.getFromName(),
                            row.getToName()));
        }
        return new TimetableRouteResult(points, totalDistance);
    }

    public List<TimetableRowData> estimateRows(
            TimetableRouteResult routeResult,
            LocalTime originDeparture,
            LocalTime destinationArrival) {
        if (originDeparture != null && destinationArrival != null) {
            throw new IllegalArgumentException(
                    "Use either departure-oriented or arrival-oriented planning.");
        }

        List<TimetableRowData> rows = baseRows(routeResult);
        if (rows.isEmpty()) {
            return rows;
        }

        // No anchors: return rows with distances only, no time estimates
        if (originDeparture == null && destinationArrival == null) {
            return rows;
        }

        if (originDeparture != null) {
            rows.getFirst().setEstimatedDeparture(formatTime(originDeparture));
            LocalTime cursor = originDeparture;
            for (int index = 1; index < rows.size(); index++) {
                cursor =
                        cursor.plusSeconds(travelSeconds(rows.get(index).getSegmentLengthMeters()));
                rows.get(index).setEstimatedArrival(formatTime(cursor));
                if (index < rows.size() - 1) {
                    rows.get(index).setEstimatedDeparture(formatTime(cursor));
                }
            }
        } else {
            rows.getLast().setEstimatedArrival(formatTime(destinationArrival));
            LocalTime cursor = destinationArrival;
            for (int index = rows.size() - 2; index >= 0; index--) {
                cursor =
                        cursor.minusSeconds(
                                travelSeconds(rows.get(index + 1).getSegmentLengthMeters()));
                rows.get(index).setEstimatedDeparture(formatTime(cursor));
                if (index > 0) {
                    rows.get(index).setEstimatedArrival(formatTime(cursor));
                }
            }
        }

        return rows;
    }

    /** Builds the JGraphT routing graph from all sections of line (cached via Spring Cache). */
    @Cacheable(value = "routingGraph", key = "'graph'")
    public Graph<String, DefaultWeightedEdge> buildGraph() {
        Map<String, OperationalPoint> pointsByUopid = loadOperationalPointsByUopid();
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

    @Cacheable(value = "operationalPointsByUopid", key = "'all'")
    public Map<String, OperationalPoint> loadOperationalPointsByUopid() {
        Map<String, OperationalPoint> pointsByUopid = new LinkedHashMap<>();
        for (OperationalPoint point : operationalPointRepository.findAll()) {
            pointsByUopid.put(point.getUopid(), point);
        }
        return pointsByUopid;
    }

    private PathSegment shortestPath(
            String startUopid,
            String endUopid,
            Graph<String, DefaultWeightedEdge> graph,
            DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra) {
        if (startUopid.equals(endUopid)) {
            return new PathSegment(List.of(startUopid), List.of(0D));
        }

        if (!graph.containsVertex(startUopid) || !graph.containsVertex(endUopid)) {
            throw new IllegalStateException("No route found between selected operational points.");
        }

        var path = dijkstra.getPath(startUopid, endUopid);
        if (path == null) {
            throw new IllegalStateException("No route found between selected operational points.");
        }

        List<String> nodes = path.getVertexList();
        List<Double> lengths = new ArrayList<>();
        lengths.add(0D); // first node has 0 segment length
        List<DefaultWeightedEdge> edges = path.getEdgeList();
        for (DefaultWeightedEdge edge : edges) {
            lengths.add(graph.getEdgeWeight(edge));
        }
        return new PathSegment(nodes, lengths);
    }

    private Map<Integer, RoutePointRole> explicitRoles(
            List<String> fullPath, List<OperationalPoint> orderedWaypoints) {
        Map<Integer, RoutePointRole> roles = new HashMap<>();
        int searchStart = 0;
        for (int index = 0; index < orderedWaypoints.size(); index++) {
            OperationalPoint waypoint = orderedWaypoints.get(index);
            int foundIndex = findNextIndex(fullPath, waypoint.getUopid(), searchStart);
            if (foundIndex < 0) {
                continue;
            }
            roles.put(
                    foundIndex,
                    index == 0
                            ? RoutePointRole.ORIGIN
                            : index == orderedWaypoints.size() - 1
                                    ? RoutePointRole.DESTINATION
                                    : RoutePointRole.VIA);
            searchStart = foundIndex + 1;
        }
        return roles;
    }

    private int findNextIndex(List<String> fullPath, String uopid, int startIndex) {
        for (int index = startIndex; index < fullPath.size(); index++) {
            if (uopid.equals(fullPath.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private String journeyLocationType(RoutePointRole role) {
        return switch (role) {
            case ORIGIN -> "ORIGIN";
            case DESTINATION -> "DESTINATION";
            case VIA, AUTO -> "INTERMEDIATE";
        };
    }

    private List<String> legacyNameVariants(String name) {
        String normalized = name.trim();
        List<String> variants = new ArrayList<>();
        variants.add(normalized);
        variants.add(normalized.replace("ue", "ü").replace("oe", "ö").replace("ae", "ä"));
        variants.add(normalized.replace("ü", "ue").replace("ö", "oe").replace("ä", "ae"));
        variants.add(normalized.replace("ss", "ß"));
        variants.add(normalized.toLowerCase(Locale.GERMAN));
        return variants.stream().distinct().toList();
    }

    private List<TimetableRowData> baseRows(TimetableRouteResult routeResult) {
        List<TimetableRowData> rows = new ArrayList<>();
        for (int index = 0; index < routeResult.points().size(); index++) {
            TimetableRoutePoint point = routeResult.points().get(index);
            TimetableRowData row = new TimetableRowData();
            row.setSequence(index + 1);
            row.setUopid(point.uopid());
            row.setName(point.name());
            row.setCountry(point.country());
            row.setRoutePointRole(point.routePointRole());
            row.setJourneyLocationType(point.journeyLocationType());
            row.setFromName(point.fromName());
            row.setToName(point.toName());
            row.setSegmentLengthMeters(point.segmentLengthMeters());
            row.setDistanceFromStartMeters(point.distanceFromStartMeters());
            rows.add(row);
        }
        return rows;
    }

    private long travelSeconds(Double lengthMeters) {
        double length = lengthMeters != null ? lengthMeters : 0D;
        return Math.round(length / ASSUMED_SPEED_METERS_PER_SECOND);
    }

    private String formatTime(LocalTime time) {
        return time.truncatedTo(ChronoUnit.MINUTES).format(TIME_FORMAT);
    }

    private record PathSegment(List<String> nodeUopids, List<Double> segmentLengths) {}
}

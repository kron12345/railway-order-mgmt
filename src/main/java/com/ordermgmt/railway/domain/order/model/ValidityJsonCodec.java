package com.ordermgmt.railway.domain.order.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Codec for converting between {@code List<LocalDate>} and the JSON validity format used in order
 * positions and timetable archives.
 *
 * <p>The JSON format is an array of date-range segments:
 *
 * <pre>[{"startDate":"2026-01-01","endDate":"2026-01-05"}, ...]</pre>
 *
 * <p>Consecutive dates are collapsed into a single segment. This class replaces three independent
 * copies of the same logic that existed in TimetableArchiveService, ServicePositionDialog and
 * PurchaseCalendarGrid.
 */
public final class ValidityJsonCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ValidityJsonCodec() {}

    /**
     * Converts a list of dates to a JSON array of date-range segments. Dates are deduplicated and
     * sorted; consecutive dates are merged into ranges.
     *
     * @param dates the dates to encode (may be null or empty)
     * @return the JSON string, or {@code null} if the list is empty
     */
    public static String toJson(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            return null;
        }

        LinkedHashSet<LocalDate> uniqueDates = new LinkedHashSet<>(dates);
        List<LocalDate> sortedDates = uniqueDates.stream().sorted().toList();
        List<Map<String, String>> segments = new ArrayList<>();

        LocalDate segmentStart = sortedDates.getFirst();
        LocalDate previous = segmentStart;

        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate current = sortedDates.get(i);
            if (!current.equals(previous.plusDays(1))) {
                segments.add(segmentMap(segmentStart, previous));
                segmentStart = current;
            }
            previous = current;
        }
        segments.add(segmentMap(segmentStart, previous));

        try {
            return OBJECT_MAPPER.writeValueAsString(segments);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * Parses a JSON validity string back into an ordered list of individual dates.
     *
     * @param json the JSON array of date-range segments (may be null or blank)
     * @return a mutable list of dates, never null
     */
    public static List<LocalDate> fromJson(String json) {
        List<LocalDate> dates = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return dates;
        }
        try {
            var array = OBJECT_MAPPER.readTree(json);
            if (!array.isArray()) {
                return dates;
            }
            for (var segment : array) {
                var startNode = segment.get("startDate");
                var endNode = segment.get("endDate");
                if (startNode == null || endNode == null) {
                    continue;
                }
                LocalDate start = LocalDate.parse(startNode.asText());
                LocalDate end = LocalDate.parse(endNode.asText());
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    dates.add(d);
                }
            }
        } catch (Exception ignored) {
            // Malformed JSON returns empty list
        }
        return dates;
    }

    private static Map<String, String> segmentMap(LocalDate startDate, LocalDate endDate) {
        Map<String, String> segment = new LinkedHashMap<>();
        segment.put("startDate", startDate.toString());
        segment.put("endDate", endDate.toString());
        return segment;
    }
}

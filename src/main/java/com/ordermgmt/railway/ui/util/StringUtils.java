package com.ordermgmt.railway.ui.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared string utility methods used across UI components. Centralizes common tag splitting,
 * null-coalescing, and blank-to-null conversion to avoid duplication.
 */
public final class StringUtils {

    private StringUtils() {}

    /**
     * Splits a comma-separated tag string into a list of trimmed, non-blank tokens.
     *
     * @param storedTags the raw comma-separated string (may be null)
     * @return a list of individual tag names, never null
     */
    public static List<String> splitTags(String storedTags) {
        List<String> values = new ArrayList<>();
        if (storedTags == null || storedTags.isBlank()) {
            return values;
        }
        for (String token : storedTags.split(",")) {
            String normalized = token.trim();
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    /**
     * Returns the trimmed string if non-blank, otherwise {@code null}.
     *
     * @param value the input string (may be null)
     * @return the trimmed value or null
     */
    public static String blankToNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    /**
     * Null-safe coalesce: returns the string if non-null, otherwise an empty string.
     *
     * @param value the input string (may be null)
     * @return the value or ""
     */
    public static String nvl(String value) {
        return value != null ? value : "";
    }
}

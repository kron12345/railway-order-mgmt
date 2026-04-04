package com.ordermgmt.railway.ui.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared string utility methods used across UI components. Delegates to Apache Commons Lang where
 * possible and provides domain-specific helpers for tag handling.
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
        if (org.apache.commons.lang3.StringUtils.isBlank(storedTags)) {
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
     * Returns the trimmed string if non-blank, otherwise {@code null}. Delegates to Apache Commons
     * Lang {@code trimToNull}.
     *
     * @param value the input string (may be null)
     * @return the trimmed value or null
     */
    public static String blankToNull(String value) {
        return org.apache.commons.lang3.StringUtils.trimToNull(value);
    }

    /**
     * Null-safe coalesce: returns the string if non-null, otherwise an empty string. Delegates to
     * Apache Commons Lang {@code defaultString}.
     *
     * @param value the input string (may be null)
     * @return the value or ""
     */
    public static String nvl(String value) {
        return org.apache.commons.lang3.StringUtils.defaultString(value);
    }
}

package com.ordermgmt.railway.ui.util;

import java.util.ArrayList;
import java.util.List;

/** Shared string helpers used across UI components. */
public final class StringUtils {

    private StringUtils() {}

    public static List<String> splitTags(String storedTags) {
        List<String> tags = new ArrayList<>();
        if (org.apache.commons.lang3.StringUtils.isBlank(storedTags)) {
            return tags;
        }
        for (String token : storedTags.split(",")) {
            String tag = token.trim();
            if (!tag.isBlank()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    public static String blankToNull(String value) {
        return org.apache.commons.lang3.StringUtils.trimToNull(value);
    }

    public static String nvl(String value) {
        return org.apache.commons.lang3.StringUtils.defaultString(value);
    }
}

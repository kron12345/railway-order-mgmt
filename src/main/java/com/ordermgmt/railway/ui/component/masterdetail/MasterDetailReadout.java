package com.ordermgmt.railway.ui.component.masterdetail;

import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;

/**
 * Builds the lazy-list footer readout text ("loaded 1–N / more · filtered") and decides whether any
 * filter is active, for {@link MasterDetailLayout}. Pure presentation logic kept out of the layout.
 */
final class MasterDetailReadout {

    private MasterDetailReadout() {}

    /** Footer status line: loaded range, optional "more" marker, optional "filtered" marker. */
    static String statusText(
            MasterDetailSpec<?> spec, int loadedCount, boolean hasMore, boolean filterActive) {
        StringBuilder statusText = new StringBuilder(spec.readoutLoadedLabel);
        statusText.append(' ').append(loadedCount == 0 ? "0" : "1–" + loadedCount);
        if (hasMore) {
            statusText.append(" / ").append(spec.readoutMoreLabel);
        }
        if (filterActive) {
            statusText.append(" · ").append(spec.readoutFilteredLabel);
        }
        return statusText.toString();
    }

    /** Whether the free-text filter or any filter-panel chip is set. */
    static boolean isFilterActive(String filterText, MasterDetailSpec<?> spec) {
        if (!filterText.isBlank()) {
            return true;
        }
        for (FilterField<?> field : spec.filterFields) {
            if (!field.chips().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}

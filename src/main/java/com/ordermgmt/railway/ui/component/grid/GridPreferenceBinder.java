package com.ordermgmt.railway.ui.component.grid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;

import com.ordermgmt.railway.domain.userprefs.service.UserViewPreferenceService;

/**
 * Binds a {@link Grid}'s column layout (order, width, visibility) to a per-user persistence
 * backend, identified by a stable {@code viewKey}. Snapshots the default column setup at install
 * time so {@link #reset()} can restore it.
 *
 * <p>Save is debounced (1500ms) on UI thread to coalesce repeated resize/reorder events.
 */
public class GridPreferenceBinder<T> {

    private static final Logger log = LoggerFactory.getLogger(GridPreferenceBinder.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long DEBOUNCE_MS = 1500;

    private final Grid<T> grid;
    private final String viewKey;
    private final UserViewPreferenceService prefs;

    private final List<ColumnState> defaults = new ArrayList<>();

    private long pendingSaveScheduledAt = 0;
    private Runnable onChanged;

    public GridPreferenceBinder(Grid<T> grid, String viewKey, UserViewPreferenceService prefs) {
        this.grid = grid;
        this.viewKey = viewKey;
        this.prefs = prefs;
    }

    /** Set an optional callback invoked after any persisted change (e.g. to refresh badges). */
    public void setOnChanged(Runnable onChanged) {
        this.onChanged = onChanged;
    }

    /** Snapshot defaults, install listeners, and apply any persisted state. */
    public void install() {
        snapshotDefaults();
        applySaved();
        attachListeners();
        grid.setColumnReorderingAllowed(true);
        grid.getColumns().forEach(c -> c.setResizable(true));
    }

    /** Drop saved preference and restore the default snapshot. */
    public void reset() {
        prefs.delete(viewKey);
        applyState(defaults);
        notifyChanged();
    }

    public List<ColumnState> currentState() {
        List<ColumnState> states = new ArrayList<>();
        for (Grid.Column<T> column : grid.getColumns()) {
            String key = column.getKey();
            if (key == null) {
                continue;
            }
            states.add(
                    new ColumnState(
                            key, column.isVisible(), column.getWidth(), column.getFlexGrow()));
        }
        return states;
    }

    public List<ColumnState> defaultsSnapshot() {
        return List.copyOf(defaults);
    }

    /** Map columns by stable key for visibility-toggle UIs. */
    public Map<String, Grid.Column<T>> columnsByKey() {
        Map<String, Grid.Column<T>> columnsByKey = new LinkedHashMap<>();
        for (Grid.Column<T> column : grid.getColumns()) {
            if (column.getKey() != null) {
                columnsByKey.put(column.getKey(), column);
            }
        }
        return columnsByKey;
    }

    /** Public so the visibility popover can trigger an immediate save. */
    public void saveNow() {
        try {
            String json = MAPPER.writeValueAsString(currentState());
            prefs.saveJson(viewKey, json);
            notifyChanged();
        } catch (Exception ex) {
            log.warn("Failed to persist grid preferences for viewKey={}", viewKey, ex);
        }
    }

    private void snapshotDefaults() {
        defaults.clear();
        defaults.addAll(currentState());
    }

    private void applySaved() {
        Optional<String> json = prefs.loadJson(viewKey);
        if (json.isEmpty()) {
            return;
        }
        try {
            List<ColumnState> saved =
                    MAPPER.readValue(json.get(), new TypeReference<List<ColumnState>>() {});
            applyState(saved);
        } catch (Exception ex) {
            log.warn(
                    "Failed to load grid preferences for viewKey={}, payload ignored", viewKey, ex);
        }
    }

    private void applyState(List<ColumnState> states) {
        Map<String, Grid.Column<T>> columnsByKey = columnsByKey();
        Map<String, ColumnState> stateByKey = new HashMap<>();
        for (ColumnState state : states) {
            stateByKey.put(state.key(), state);
        }

        for (ColumnState state : states) {
            Grid.Column<T> column = columnsByKey.get(state.key());
            if (column == null) {
                continue;
            }
            column.setVisible(state.visible());
            if (state.width() != null) {
                column.setWidth(state.width());
            }
            column.setFlexGrow(state.flexGrow());
        }

        // Reorder: known keys in saved order first, unknown keys (newly added columns) appended.
        List<Grid.Column<T>> ordered = new ArrayList<>();
        for (ColumnState state : states) {
            Grid.Column<T> column = columnsByKey.get(state.key());
            if (column != null) {
                ordered.add(column);
            }
        }
        for (Grid.Column<T> column : grid.getColumns()) {
            if (column.getKey() != null
                    && !stateByKey.containsKey(column.getKey())
                    && !ordered.contains(column)) {
                ordered.add(column);
            }
        }
        if (!ordered.isEmpty()) {
            grid.setColumnOrder(ordered);
        }
    }

    private void attachListeners() {
        grid.addColumnReorderListener(e -> scheduleSave());
        grid.addColumnResizeListener(e -> scheduleSave());
    }

    private void scheduleSave() {
        long now = System.currentTimeMillis();
        pendingSaveScheduledAt = now;
        UI ui = UI.getCurrent();
        if (ui == null) return;
        ui.getPage()
                .executeJs("return new Promise(r => setTimeout(r, $0))", DEBOUNCE_MS)
                .then(
                        ignored -> {
                            if (System.currentTimeMillis() - pendingSaveScheduledAt
                                    >= DEBOUNCE_MS - 50) {
                                saveNow();
                            }
                        });
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    /** Serializable per-column state. Width is a CSS string ("120px") or null for flex-only. */
    public record ColumnState(String key, boolean visible, String width, int flexGrow) {}
}

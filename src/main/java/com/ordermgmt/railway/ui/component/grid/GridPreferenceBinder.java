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
 * Binds a {@link Grid}'s column layout (order, width, visibility) to a per-user
 * persistence backend, identified by a stable {@code viewKey}. Snapshots the default
 * column setup at install time so {@link #reset()} can restore it.
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

    /** Default snapshot taken at install time, used by reset(). */
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
        if (onChanged != null) onChanged.run();
    }

    public List<ColumnState> currentState() {
        List<ColumnState> states = new ArrayList<>();
        for (Grid.Column<T> col : grid.getColumns()) {
            String key = col.getKey();
            if (key == null) continue;
            states.add(new ColumnState(key, col.isVisible(), col.getWidth(), col.getFlexGrow()));
        }
        return states;
    }

    public List<ColumnState> defaultsSnapshot() {
        return List.copyOf(defaults);
    }

    /** Map columns by stable key for visibility-toggle UIs. */
    public Map<String, Grid.Column<T>> columnsByKey() {
        Map<String, Grid.Column<T>> byKey = new LinkedHashMap<>();
        for (Grid.Column<T> col : grid.getColumns()) {
            if (col.getKey() != null) byKey.put(col.getKey(), col);
        }
        return byKey;
    }

    /** Public so the visibility popover can trigger an immediate save. */
    public void saveNow() {
        try {
            String json = MAPPER.writeValueAsString(currentState());
            prefs.saveJson(viewKey, json);
            if (onChanged != null) onChanged.run();
        } catch (Exception ex) {
            log.warn("Failed to persist grid preferences for viewKey={}", viewKey, ex);
        }
    }

    // ─── internals ─────────

    private void snapshotDefaults() {
        defaults.clear();
        defaults.addAll(currentState());
    }

    private void applySaved() {
        Optional<String> json = prefs.loadJson(viewKey);
        if (json.isEmpty()) return;
        try {
            List<ColumnState> saved = MAPPER.readValue(json.get(),
                    new TypeReference<List<ColumnState>>() {});
            applyState(saved);
        } catch (Exception ex) {
            log.warn("Failed to load grid preferences for viewKey={}, payload ignored", viewKey, ex);
        }
    }

    private void applyState(List<ColumnState> states) {
        Map<String, Grid.Column<T>> byKey = columnsByKey();
        Map<String, ColumnState> byKeyState = new HashMap<>();
        for (ColumnState s : states) byKeyState.put(s.key(), s);

        // Apply per-column properties for keys that exist in both saved state and grid.
        for (ColumnState s : states) {
            Grid.Column<T> col = byKey.get(s.key());
            if (col == null) continue;
            col.setVisible(s.visible());
            if (s.width() != null) col.setWidth(s.width());
            col.setFlexGrow(s.flexGrow());
        }

        // Reorder: known keys in saved order first, unknown keys (newly added columns) appended.
        List<Grid.Column<T>> ordered = new ArrayList<>();
        for (ColumnState s : states) {
            Grid.Column<T> col = byKey.get(s.key());
            if (col != null) ordered.add(col);
        }
        for (Grid.Column<T> col : grid.getColumns()) {
            if (col.getKey() != null && !byKeyState.containsKey(col.getKey()) && !ordered.contains(col)) {
                ordered.add(col);
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
        ui.getPage().executeJs(
                "return new Promise(r => setTimeout(r, $0))", DEBOUNCE_MS)
                .then(ignored -> {
                    if (System.currentTimeMillis() - pendingSaveScheduledAt >= DEBOUNCE_MS - 50) {
                        saveNow();
                    }
                });
    }

    /** Serializable per-column state. Width is a CSS string ("120px") or null for flex-only. */
    public record ColumnState(String key, boolean visible, String width, int flexGrow) {}
}

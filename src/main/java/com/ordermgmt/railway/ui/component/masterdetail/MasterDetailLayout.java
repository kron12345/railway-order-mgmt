package com.ordermgmt.railway.ui.component.masterdetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

import com.ordermgmt.railway.ui.component.DataReadout;
import com.ordermgmt.railway.ui.component.a11y.AriaLive;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterPanel;

/**
 * Generic Bloomberg-style master-detail shell: scrollable card list on the left, detail panel on
 * the right. Selection is URL-driven (caller navigates), but the layout owns keyboard navigation
 * (↑/↓/Home/End), filter wiring, ARIA semantics, and the live-region announcements.
 *
 * <p>Cards are rendered through a {@link Function} the caller provides; selection state is updated
 * by calling {@link #setSelectedId(UUID)} after navigation.
 */
public class MasterDetailLayout<T> extends Div {

    private static final int FILTER_VALUE_CHANGE_TIMEOUT_MS = 200;
    private static final int MASTER_SPLIT_POSITION = 28;
    private static final String FILTER_WIDTH = "280px";
    private static final String EMPTY_FILTER_TEXT = "";

    /** Auto-load when keyboard navigation reaches this close to the last loaded row. */
    private static final int AUTO_LOAD_THRESHOLD = 3;

    private final MasterDetailSpec<T> spec;
    private final TextField filter = new TextField();
    private final Div listScroll = new Div();
    private final Div detailPane = new Div();
    private final AriaLive ariaLive = new AriaLive();
    private final Span emptyState = new Span();

    private final List<T> allItems = new ArrayList<>();
    private final List<T> visibleItems = new ArrayList<>();
    private final List<MasterCardWrapper> cardWrappers = new ArrayList<>();
    private UUID selectedId;
    private String filterText = EMPTY_FILTER_TEXT;

    private FilterPanel<T> filterPanel;
    private Predicate<T> panelPredicate = item -> true;

    // Lazy mode (opt-in via setLazyLoader): server-paged accumulation alongside the legacy
    // in-memory setItems() path. When lazyMode is false, none of this is touched.
    private final DataReadout readout = new DataReadout();
    private LazyListController<T> lazyController;
    private boolean lazyMode = false;

    public MasterDetailLayout(MasterDetailSpec<T> spec) {
        this.spec = spec;
        addClassName("md-layout");
        getStyle().set("display", "flex").set("flex-direction", "column").set("height", "100%");

        filterPanel = buildFilterPanel();

        add(buildToolbar());
        if (filterPanel != null) {
            add(filterPanel);
        }
        add(buildSplit());
        add(ariaLive);

        registerShortcuts();
    }

    private FilterPanel<T> buildFilterPanel() {
        if (spec.filterFields.isEmpty()) {
            return null;
        }
        return new FilterPanel<>(
                spec.filterFields,
                predicate -> {
                    panelPredicate = predicate;
                    applyFilter();
                },
                new FilterPanel.Labels(
                        spec.filterToggleLabel,
                        spec.filterClearAllLabel,
                        spec.filterChipClearAria,
                        spec.filterPanelAria));
    }

    /**
     * View-local shortcuts via the shared hotkeys-js wrapper in {@code frontend/rom-shortcuts.ts}.
     * Single entry point keeps the editable-input detection consistent across master-detail views
     * and lets us add new shortcuts by editing one TS file instead of duplicated inline JS.
     */
    private void registerShortcuts() {
        getElement()
                .executeJs(
                        "window.romShortcuts && window.romShortcuts.registerView(this, $0, $1);",
                        spec.filterId,
                        spec.shortcutNew != null);

        // The Esc-clears-filter behaviour is now scoped to the actual filter input via
        // a Vaadin keydown listener, so it only fires when the filter has focus and
        // doesn't depend on the global JS shortcut handler.
        filter.getElement()
                .addEventListener(
                        "keydown",
                        e -> {
                            String key = e.getEventData().getString("event.key");
                            if ("Escape".equals(key)) {
                                filter.setValue("");
                            }
                        })
                .addEventData("event.key")
                .setFilter("event.key === 'Escape'");

        if (spec.shortcutNew != null) {
            getElement().addEventListener("md-new", e -> spec.shortcutNew.run());
        }
    }

    private Component buildToolbar() {
        var bar = new HorizontalLayout();
        bar.addClassName("md-toolbar");
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.getElement().setAttribute("role", "toolbar");
        bar.getElement().setAttribute("aria-label", spec.toolbarAriaLabel);

        configureFilterField();
        bar.add(filter);

        if (filterPanel != null) {
            bar.add(filterPanel.getToggle());
        }

        for (Component extraComponent : spec.extraToolbar) {
            bar.add(extraComponent);
        }
        return bar;
    }

    private void configureFilterField() {
        filter.setId(spec.filterId);
        filter.setPlaceholder(spec.filterPlaceholder);
        filter.setPrefixComponent(VaadinIcon.SEARCH.create());
        filter.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        filter.setClearButtonVisible(true);
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.setValueChangeTimeout(FILTER_VALUE_CHANGE_TIMEOUT_MS);
        filter.setWidth(FILTER_WIDTH);
        filter.getElement().setAttribute("aria-label", spec.filterAriaLabel);
        filter.addValueChangeListener(e -> updateFilterText(e.getValue()));
    }

    private void updateFilterText(String rawValue) {
        filterText =
                rawValue == null ? EMPTY_FILTER_TEXT : rawValue.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    private Component buildSplit() {
        var split = new SplitLayout();
        split.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        split.setSplitterPosition(MASTER_SPLIT_POSITION);
        split.setSizeFull();
        split.addClassName("md-split");

        var listPane = new Div();
        listPane.addClassName("md-list-pane");
        listPane.setId(spec.listId);
        listPane.getElement().setAttribute("role", "region");
        listPane.getElement().setAttribute("aria-label", spec.listAriaLabel);
        listPane.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "100%")
                .set("min-height", "0");

        listScroll.addClassName("md-list");
        listScroll.getElement().setAttribute("role", "listbox");
        listScroll.getElement().setAttribute("aria-label", spec.listAriaLabel);
        listScroll.getElement().setAttribute("tabindex", "0");
        listScroll
                .getStyle()
                .set("flex", "1 1 auto")
                .set("overflow-y", "auto")
                .set("min-height", "0")
                .set("outline", "none");

        // Keyboard navigation on the list itself.
        listScroll
                .getElement()
                .addEventListener(
                        "keydown", e -> onListKey(e.getEventData().getString("event.key")))
                .addEventData("event.key")
                .addEventData("event.preventDefault()")
                .setFilter("['ArrowDown','ArrowUp','Home','End','Enter'].includes(event.key)");

        emptyState.addClassName("md-empty");
        emptyState.setText(spec.emptyText);
        listScroll.add(emptyState);

        listPane.add(listScroll);

        // Terminal-style readout footer, shown only in lazy mode (setLazyLoader makes it visible).
        readout.setVisible(false);
        listPane.add(readout);

        detailPane.addClassName("md-detail");
        detailPane.setId(spec.detailId);
        detailPane.getElement().setAttribute("role", "region");
        detailPane.getElement().setAttribute("aria-label", spec.detailAriaLabel);
        detailPane.setSizeFull();
        detailPane.getStyle().set("overflow", "auto");

        split.addToPrimary(listPane);
        split.addToSecondary(detailPane);
        return split;
    }

    public void setItems(List<T> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter();
    }

    /**
     * Switch this list into lazy (server-paged) mode: {@code loader} receives the current filter
     * text + an offset (always a multiple of the loader's page size) and returns one page. Call
     * once after {@link MasterDetailSpec#build()}; the legacy {@link #setItems(List)} path stays
     * untouched.
     */
    public void setLazyLoader(BiFunction<String, Integer, SliceResult<T>> loader) {
        this.lazyMode = true;
        this.lazyController = new LazyListController<>(loader, this::onLazyChange);
        this.readout.setVisible(true);
        // No initial load here — beforeEnter drives the first page (reloadLazy for the bare list,
        // ensureLoaded for a deep link), so navigation never fires a duplicate page-1 query.
    }

    /**
     * Re-run the current lazy query from offset 0 — explicit refresh (filter, card edit, list
     * view).
     */
    public void reloadLazy() {
        if (lazyMode && lazyController != null) {
            lazyController.reset(filterText);
        }
    }

    /**
     * Load the first page only if it has never been loaded — used on a deep-link/selection
     * navigation so an already-accumulated list keeps its pages, scroll position and selection
     * highlight instead of collapsing back to page 1.
     */
    public void ensureLoaded() {
        if (lazyMode && lazyController != null && !lazyController.isStarted()) {
            lazyController.reset(filterText);
        }
    }

    public boolean isLazyMode() {
        return lazyMode;
    }

    public DataReadout getReadout() {
        return readout;
    }

    private void onLazyChange() {
        renderCards();
        updateReadout();
    }

    /** The currently displayed list — accumulated lazy items, or the in-memory filtered list. */
    private List<T> displayed() {
        return lazyMode ? lazyController.items() : visibleItems;
    }

    public void setSelectedId(UUID id) {
        this.selectedId = id;
        for (MasterCardWrapper cardWrapper : cardWrappers) {
            cardWrapper.applySelection(Objects.equals(cardWrapper.id, id));
        }
        T item = findById(id);
        if (item != null) {
            // Lazy mode: announce without a real total (we only know the loaded count). Falls back
            // to the regular "x of N" template when no lazy template was supplied.
            if (lazyMode && spec.lazyAnnounceTemplate != null) {
                ariaLive.announce(spec.lazyAnnounceTemplate.apply(item, indexOf(id) + 1));
            } else {
                ariaLive.announce(
                        spec.announceTemplate.apply(item, indexOf(id) + 1, displayed().size()));
            }
        }
    }

    /** Replace the right-side detail. Pass {@code null} to clear and show empty state. */
    public void setDetail(Component detail) {
        detailPane.removeAll();
        if (detail == null) {
            var empty = new Span(spec.detailEmptyText);
            empty.addClassName("md-detail-empty");
            detailPane.add(empty);
        } else {
            detailPane.add(detail);
        }
    }

    public TextField getFilter() {
        return filter;
    }

    private void applyFilter() {
        if (lazyMode) {
            // Server-side: rebuild the query from filterText + field getters (the view's loader
            // closure reads them) and reload from offset 0; renderCards runs via onLazyChange.
            lazyController.reset(filterText);
            return;
        }
        visibleItems.clear();
        Predicate<T> text =
                filterText.isBlank() ? item -> true : item -> spec.matcher.test(item, filterText);
        Predicate<T> matches = text.and(panelPredicate);
        for (T item : allItems) {
            if (matches.test(item)) {
                visibleItems.add(item);
            }
        }
        renderCards();
    }

    private void renderCards() {
        listScroll.removeAll();
        cardWrappers.clear();
        List<T> items = displayed();
        if (items.isEmpty()) {
            listScroll.add(emptyState);
            return;
        }
        int index = 0;
        for (T item : items) {
            listScroll.add(renderOneCard(item, index));
            index++;
        }
        if (lazyMode && lazyController.hasMore()) {
            LazyLoadSentinel sentinel =
                    new LazyLoadSentinel(
                            spec.sentinelLabel,
                            () -> lazyController.loadNext(filterText),
                            this::loadNextAndFocusFirstNew);
            listScroll.add(sentinel);
            sentinel.observe(listScroll.getElement());
        } else if (lazyMode) {
            detachObserver();
        }
    }

    private Div renderOneCard(T item, int index) {
        UUID id = spec.idExtractor.apply(item);
        Component card = spec.cardRenderer.apply(item);
        Div wrapper = new Div(card);
        wrapper.addClassName("md-card-wrapper");
        wrapper.getElement().setAttribute("role", "option");
        wrapper.getElement().setAttribute("tabindex", index == 0 ? "0" : "-1");
        wrapper.getElement().setAttribute("data-id", id.toString());
        wrapper.getElement().addEventListener("click", e -> spec.onSelect.accept(id));
        MasterCardWrapper cardWrapper = new MasterCardWrapper(id, wrapper);
        cardWrapper.applySelection(Objects.equals(id, selectedId));
        cardWrappers.add(cardWrapper);
        return wrapper;
    }

    /**
     * Interactive "load more" (click/keyboard on the sentinel): append the next page, then move
     * focus to the first newly-loaded card so keyboard users land on the new content. The
     * scroll-triggered auto-load path deliberately does not move focus.
     */
    private void loadNextAndFocusFirstNew() {
        int firstNewIndex = lazyController.loadedCount();
        lazyController.loadNext(filterText);
        listScroll
                .getElement()
                .executeJs(
                        "const opts=this.querySelectorAll('[role=option]');"
                                + "const el=opts[$0]; if(el){el.focus();}",
                        firstNewIndex);
    }

    /** Disconnect the auto-load observer when no further pages remain (no sentinel to watch). */
    private void detachObserver() {
        listScroll
                .getElement()
                .executeJs("if(this.__romObs){this.__romObs.disconnect(); this.__romObs=null;}");
    }

    private void updateReadout() {
        if (!lazyMode) {
            return;
        }
        int loadedCount = lazyController.loadedCount();
        StringBuilder statusText = new StringBuilder(spec.readoutLoadedLabel);
        statusText.append(' ').append(loadedCount == 0 ? "0" : "1–" + loadedCount);
        if (lazyController.hasMore()) {
            statusText.append(" / ").append(spec.readoutMoreLabel);
        }
        if (isFilterActive()) {
            statusText.append(" · ").append(spec.readoutFilteredLabel);
        }
        readout.setStatus(statusText.toString());
    }

    private boolean isFilterActive() {
        if (!filterText.isBlank()) {
            return true;
        }
        for (FilterField<T> field : spec.filterFields) {
            if (!field.chips().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void onListKey(String key) {
        List<T> items = displayed();
        if (items.isEmpty()) {
            return;
        }
        int currentIndex = selectedId != null ? indexOf(selectedId) : -1;
        int lastIndex = items.size() - 1;
        int next = currentIndex;
        switch (key) {
            case "ArrowDown" -> next = currentIndex < 0 ? 0 : Math.min(lastIndex, currentIndex + 1);
            case "ArrowUp" -> next = currentIndex <= 0 ? 0 : currentIndex - 1;
            case "Home" -> next = 0;
            case "End" -> next = lastIndex;
            case "Enter" -> {
                if (currentIndex >= 0) {
                    spec.onSelect.accept(spec.idExtractor.apply(items.get(currentIndex)));
                }
                return;
            }
            default -> {
                return;
            }
        }
        if (next == currentIndex || next < 0) {
            return;
        }
        // Lazy: pull the next page when navigating into the last few loaded rows.
        if (lazyMode && lazyController.hasMore() && next >= items.size() - AUTO_LOAD_THRESHOLD) {
            lazyController.loadNext(filterText);
        }
        spec.onSelect.accept(spec.idExtractor.apply(items.get(next)));
    }

    private int indexOf(UUID id) {
        List<T> items = displayed();
        for (int index = 0; index < items.size(); index++) {
            if (Objects.equals(spec.idExtractor.apply(items.get(index)), id)) {
                return index;
            }
        }
        return -1;
    }

    private T findById(UUID id) {
        // Non-lazy: full set (announce works for a filtered-out deep link). Lazy: only loaded rows
        // (a deep-linked unloaded row still renders its detail via beforeEnter, just no highlight).
        List<T> source = lazyMode ? lazyController.items() : allItems;
        for (T item : source) {
            if (Objects.equals(spec.idExtractor.apply(item), id)) {
                return item;
            }
        }
        return null;
    }

    private static class MasterCardWrapper {
        final UUID id;
        final Div wrapper;

        MasterCardWrapper(UUID id, Div wrapper) {
            this.id = id;
            this.wrapper = wrapper;
        }

        void applySelection(boolean selected) {
            if (selected) {
                wrapper.addClassName("md-card-wrapper--selected");
                wrapper.getElement().setAttribute("aria-selected", "true");
                wrapper.getElement()
                        .executeJs("$0.scrollIntoView({block:'nearest', behavior:'auto'})");
            } else {
                wrapper.removeClassName("md-card-wrapper--selected");
                wrapper.getElement().setAttribute("aria-selected", "false");
            }
        }
    }

    public static <T> MasterDetailSpec<T> spec() {
        return new MasterDetailSpec<>();
    }
}

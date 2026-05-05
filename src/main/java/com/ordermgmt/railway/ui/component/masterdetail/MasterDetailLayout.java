package com.ordermgmt.railway.ui.component.masterdetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
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

import com.ordermgmt.railway.ui.component.a11y.AriaLive;

/**
 * Generic Bloomberg-style master-detail shell: scrollable card list on the left,
 * detail panel on the right. Selection is URL-driven (caller navigates), but the
 * layout owns keyboard navigation (↑/↓/Home/End), filter wiring, ARIA semantics,
 * and the live-region announcements.
 *
 * <p>Cards are rendered through a {@link Function} the caller provides; selection
 * state is updated by calling {@link #setSelectedId(UUID)} after navigation.
 */
public class MasterDetailLayout<T> extends Div {

    private final Spec<T> spec;
    private final TextField filter = new TextField();
    private final Div listScroll = new Div();
    private final Div detailPane = new Div();
    private final AriaLive ariaLive = new AriaLive();
    private final Span emptyState = new Span();

    private final List<T> allItems = new ArrayList<>();
    private final List<T> visibleItems = new ArrayList<>();
    private final List<MasterCardWrapper> cardWrappers = new ArrayList<>();
    private UUID selectedId;
    private String filterText = "";

    public MasterDetailLayout(Spec<T> spec) {
        this.spec = spec;
        addClassName("md-layout");
        getStyle().set("display", "flex").set("flex-direction", "column").set("height", "100%");

        add(buildToolbar());
        add(buildSplit());
        add(ariaLive);

        registerShortcuts();
    }

    /**
     * View-local keyboard shortcuts via a body-level keydown listener that explicitly
     * skips when the user is typing into a text input — Vaadin's {@link Shortcuts}
     * matches by physical key code, so on layouts where the SLASH key sits where US
     * keyboards have it (e.g. German "-"), the shortcut would fire on every dash typed
     * inside a form. The JS-level filter checks both the actual character ({@code key})
     * and the focused element so neither "n" nor "/" hijacks regular text entry.
     */
    private void registerShortcuts() {
        // Browser-side keymap for "/" (focus filter), "n" (new), and Esc (clear filter).
        // Listens at document level but only acts when no editable element has focus.
        getElement().executeJs(
                "const root = $0;"
                + "const filter = root.querySelector('#' + $1);"
                + "const newBtnEvent = $2;"
                + "if (!root.__mdShortcutsBound) {"
                + "  root.__mdShortcutsBound = true;"
                + "  document.addEventListener('keydown', (e) => {"
                + "    if (!root.isConnected) return;"
                + "    const t = e.target;"
                + "    const tag = (t && t.tagName) || '';"
                + "    const inEditable = tag === 'INPUT' || tag === 'TEXTAREA'"
                + "        || (t && t.isContentEditable)"
                + "        || (t && t.matches && t.matches('vaadin-text-field, vaadin-text-area,"
                + "             vaadin-combo-box, vaadin-date-picker, vaadin-checkbox,"
                + "             vaadin-select, vaadin-number-field'));"
                + "    if (e.ctrlKey || e.metaKey || e.altKey) return;"
                + "    if (e.key === 'Escape' && filter && document.activeElement === filter) {"
                + "      filter.value = '';"
                + "      filter.dispatchEvent(new Event('change', {bubbles: true}));"
                + "      e.preventDefault();"
                + "      return;"
                + "    }"
                + "    if (inEditable) return;"
                + "    if (e.key === '/' && filter) {"
                + "      e.preventDefault();"
                + "      filter.focus();"
                + "    } else if (e.key === 'n' && newBtnEvent) {"
                + "      e.preventDefault();"
                + "      root.dispatchEvent(new CustomEvent('md-new', {bubbles: true, composed: true}));"
                + "    }"
                + "  });"
                + "}", getElement(), spec.filterId, spec.shortcutNew != null);

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

        filter.setId(spec.filterId);
        filter.setPlaceholder(spec.filterPlaceholder);
        filter.setPrefixComponent(VaadinIcon.SEARCH.create());
        filter.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        filter.setClearButtonVisible(true);
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.setValueChangeTimeout(200);
        filter.setWidth("280px");
        filter.getElement().setAttribute("aria-label", spec.filterAriaLabel);
        filter.addValueChangeListener(e -> {
            filterText = e.getValue() == null ? "" : e.getValue().trim().toLowerCase(Locale.ROOT);
            applyFilter();
        });
        bar.add(filter);

        for (Component extra : spec.extraToolbar) {
            bar.add(extra);
        }
        return bar;
    }

    private Component buildSplit() {
        var split = new SplitLayout();
        split.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        split.setSplitterPosition(28);
        split.setSizeFull();
        split.addClassName("md-split");

        var listPane = new Div();
        listPane.addClassName("md-list-pane");
        listPane.setId(spec.listId);
        listPane.getElement().setAttribute("role", "region");
        listPane.getElement().setAttribute("aria-label", spec.listAriaLabel);
        listPane.getStyle().set("display", "flex").set("flex-direction", "column")
                .set("height", "100%").set("min-height", "0");

        listScroll.addClassName("md-list");
        listScroll.getElement().setAttribute("role", "listbox");
        listScroll.getElement().setAttribute("aria-label", spec.listAriaLabel);
        listScroll.getElement().setAttribute("tabindex", "0");
        listScroll.getStyle().set("flex", "1 1 auto").set("overflow-y", "auto")
                .set("min-height", "0").set("outline", "none");

        // Keyboard navigation on the list itself.
        listScroll.getElement().addEventListener("keydown", e -> {
                    String key = e.getEventData().getString("event.key");
                    onListKey(key);
                }).addEventData("event.key").addEventData("event.preventDefault()")
                .setFilter("['ArrowDown','ArrowUp','Home','End','Enter'].includes(event.key)");

        emptyState.addClassName("md-empty");
        emptyState.setText(spec.emptyText);
        listScroll.add(emptyState);

        listPane.add(listScroll);

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

    public void setSelectedId(UUID id) {
        this.selectedId = id;
        for (MasterCardWrapper w : cardWrappers) {
            w.applySelection(Objects.equals(w.id, id));
        }
        T item = findById(id);
        if (item != null) {
            ariaLive.announce(spec.announceTemplate.apply(item, indexOf(id) + 1, visibleItems.size()));
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
        visibleItems.clear();
        Predicate<T> matches = filterText.isBlank()
                ? t -> true
                : t -> spec.matcher.test(t, filterText);
        for (T t : allItems) {
            if (matches.test(t)) visibleItems.add(t);
        }
        renderCards();
    }

    private void renderCards() {
        listScroll.removeAll();
        cardWrappers.clear();
        if (visibleItems.isEmpty()) {
            listScroll.add(emptyState);
            return;
        }
        int idx = 0;
        for (T item : visibleItems) {
            UUID id = spec.idExtractor.apply(item);
            Component card = spec.cardRenderer.apply(item);
            Div wrapper = new Div(card);
            wrapper.addClassName("md-card-wrapper");
            wrapper.getElement().setAttribute("role", "option");
            wrapper.getElement().setAttribute("tabindex", idx == 0 ? "0" : "-1");
            wrapper.getElement().setAttribute("data-id", id.toString());
            wrapper.getElement().addEventListener("click",
                    e -> spec.onSelect.accept(id));
            MasterCardWrapper mcw = new MasterCardWrapper(id, wrapper);
            mcw.applySelection(Objects.equals(id, selectedId));
            cardWrappers.add(mcw);
            listScroll.add(wrapper);
            idx++;
        }
    }

    private void onListKey(String key) {
        if (visibleItems.isEmpty()) return;
        int currentIndex = selectedId != null ? indexOf(selectedId) : -1;
        int next = currentIndex;
        switch (key) {
            case "ArrowDown" -> next = currentIndex < 0 ? 0 : Math.min(visibleItems.size() - 1, currentIndex + 1);
            case "ArrowUp" -> next = currentIndex <= 0 ? 0 : currentIndex - 1;
            case "Home" -> next = 0;
            case "End" -> next = visibleItems.size() - 1;
            case "Enter" -> { if (currentIndex >= 0) spec.onSelect.accept(spec.idExtractor.apply(visibleItems.get(currentIndex))); return; }
            default -> { return; }
        }
        if (next == currentIndex || next < 0) return;
        T target = visibleItems.get(next);
        UUID id = spec.idExtractor.apply(target);
        spec.onSelect.accept(id);
    }

    private int indexOf(UUID id) {
        for (int i = 0; i < visibleItems.size(); i++) {
            if (Objects.equals(spec.idExtractor.apply(visibleItems.get(i)), id)) return i;
        }
        return -1;
    }

    private T findById(UUID id) {
        for (T t : allItems) {
            if (Objects.equals(spec.idExtractor.apply(t), id)) return t;
        }
        return null;
    }

    private static class MasterCardWrapper {
        final UUID id;
        final Div wrapper;
        MasterCardWrapper(UUID id, Div wrapper) { this.id = id; this.wrapper = wrapper; }
        void applySelection(boolean sel) {
            if (sel) {
                wrapper.addClassName("md-card-wrapper--selected");
                wrapper.getElement().setAttribute("aria-selected", "true");
                wrapper.getElement().executeJs("$0.scrollIntoView({block:'nearest', behavior:'auto'})");
            } else {
                wrapper.removeClassName("md-card-wrapper--selected");
                wrapper.getElement().setAttribute("aria-selected", "false");
            }
        }
    }

    /** Builder-style configuration. All fields except {@code idExtractor},
     *  {@code cardRenderer}, {@code matcher}, {@code onSelect} have safe defaults. */
    public static class Spec<T> {
        Function<T, UUID> idExtractor;
        Function<T, Component> cardRenderer;
        java.util.function.BiPredicate<T, String> matcher;
        java.util.function.Consumer<UUID> onSelect = id -> {};
        Runnable shortcutNew;
        boolean shortcutFocusFilter = true;
        String filterPlaceholder = "Filter…";
        String filterAriaLabel = "Filter";
        String filterId = "md-filter";
        String listId = "md-list";
        String detailId = "md-detail";
        String listAriaLabel = "Liste";
        String detailAriaLabel = "Detail";
        String toolbarAriaLabel = "Toolbar";
        String emptyText = "Keine Einträge";
        String detailEmptyText = "Wähle einen Eintrag aus der Liste.";
        TriFunction<T, Integer, Integer, String> announceTemplate =
                (item, index, total) -> "Eintrag " + index + " von " + total;
        List<Component> extraToolbar = new ArrayList<>();

        public Spec<T> idExtractor(Function<T, UUID> v) { this.idExtractor = v; return this; }
        public Spec<T> cardRenderer(Function<T, Component> v) { this.cardRenderer = v; return this; }
        public Spec<T> matcher(java.util.function.BiPredicate<T, String> v) { this.matcher = v; return this; }
        public Spec<T> onSelect(java.util.function.Consumer<UUID> v) { this.onSelect = v; return this; }
        public Spec<T> shortcutNew(Runnable v) { this.shortcutNew = v; return this; }
        public Spec<T> filterPlaceholder(String v) { this.filterPlaceholder = v; return this; }
        public Spec<T> filterAriaLabel(String v) { this.filterAriaLabel = v; return this; }
        public Spec<T> filterId(String v) { this.filterId = v; return this; }
        public Spec<T> listId(String v) { this.listId = v; return this; }
        public Spec<T> detailId(String v) { this.detailId = v; return this; }
        public Spec<T> listAriaLabel(String v) { this.listAriaLabel = v; return this; }
        public Spec<T> detailAriaLabel(String v) { this.detailAriaLabel = v; return this; }
        public Spec<T> toolbarAriaLabel(String v) { this.toolbarAriaLabel = v; return this; }
        public Spec<T> emptyText(String v) { this.emptyText = v; return this; }
        public Spec<T> detailEmptyText(String v) { this.detailEmptyText = v; return this; }
        public Spec<T> announceTemplate(TriFunction<T, Integer, Integer, String> v) { this.announceTemplate = v; return this; }
        public Spec<T> extraToolbar(List<Component> v) { this.extraToolbar = v; return this; }

        public MasterDetailLayout<T> build() { return new MasterDetailLayout<>(this); }
    }

    public static <T> Spec<T> spec() { return new Spec<>(); }

    @FunctionalInterface public interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }
}

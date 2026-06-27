package com.ordermgmt.railway.ui.component.masterdetail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.flow.component.Component;

import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;

/**
 * Builder-style configuration for {@link MasterDetailLayout}. All fields except {@code
 * idExtractor}, {@code cardRenderer}, {@code matcher} and {@code onSelect} have safe defaults.
 * Fields are package-private so the layout can read them directly.
 */
public class MasterDetailSpec<T> {

    Function<T, UUID> idExtractor;
    Function<T, Component> cardRenderer;
    BiPredicate<T, String> matcher;
    Consumer<UUID> onSelect = id -> {};
    Runnable shortcutNew;
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
    // Lazy mode only: announce without a total (the real total is unknown — only "loaded" is). When
    // null, lazy selection falls back to announceTemplate with the loaded count as the "total".
    BiFunction<T, Integer, String> lazyAnnounceTemplate;
    List<Component> extraToolbar = new ArrayList<>();
    List<FilterField<T>> filterFields = new ArrayList<>();
    String filterToggleLabel = "Filter";
    String filterClearAllLabel = "Alle löschen";
    String filterChipClearAria = "Filter entfernen";
    String filterPanelAria = "Filter";

    // Lazy-mode (setLazyLoader) readout + sentinel labels; only used when a loader is set.
    String readoutLoadedLabel = "geladen";
    String readoutMoreLabel = "mehr";
    String readoutFilteredLabel = "gefiltert";
    String sentinelLabel = "weitere {0} laden";

    public MasterDetailSpec<T> idExtractor(Function<T, UUID> idExtractor) {
        this.idExtractor = idExtractor;
        return this;
    }

    public MasterDetailSpec<T> cardRenderer(Function<T, Component> cardRenderer) {
        this.cardRenderer = cardRenderer;
        return this;
    }

    public MasterDetailSpec<T> matcher(BiPredicate<T, String> matcher) {
        this.matcher = matcher;
        return this;
    }

    public MasterDetailSpec<T> onSelect(Consumer<UUID> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    public MasterDetailSpec<T> shortcutNew(Runnable shortcutNew) {
        this.shortcutNew = shortcutNew;
        return this;
    }

    public MasterDetailSpec<T> filterPlaceholder(String filterPlaceholder) {
        this.filterPlaceholder = filterPlaceholder;
        return this;
    }

    public MasterDetailSpec<T> filterAriaLabel(String filterAriaLabel) {
        this.filterAriaLabel = filterAriaLabel;
        return this;
    }

    public MasterDetailSpec<T> filterId(String filterId) {
        this.filterId = filterId;
        return this;
    }

    public MasterDetailSpec<T> listId(String listId) {
        this.listId = listId;
        return this;
    }

    public MasterDetailSpec<T> detailId(String detailId) {
        this.detailId = detailId;
        return this;
    }

    public MasterDetailSpec<T> listAriaLabel(String listAriaLabel) {
        this.listAriaLabel = listAriaLabel;
        return this;
    }

    public MasterDetailSpec<T> detailAriaLabel(String detailAriaLabel) {
        this.detailAriaLabel = detailAriaLabel;
        return this;
    }

    public MasterDetailSpec<T> toolbarAriaLabel(String toolbarAriaLabel) {
        this.toolbarAriaLabel = toolbarAriaLabel;
        return this;
    }

    public MasterDetailSpec<T> emptyText(String emptyText) {
        this.emptyText = emptyText;
        return this;
    }

    public MasterDetailSpec<T> detailEmptyText(String detailEmptyText) {
        this.detailEmptyText = detailEmptyText;
        return this;
    }

    public MasterDetailSpec<T> announceTemplate(
            TriFunction<T, Integer, Integer, String> announceTemplate) {
        this.announceTemplate = announceTemplate;
        return this;
    }

    public MasterDetailSpec<T> lazyAnnounceTemplate(BiFunction<T, Integer, String> template) {
        this.lazyAnnounceTemplate = template;
        return this;
    }

    public MasterDetailSpec<T> extraToolbar(List<Component> extraToolbar) {
        this.extraToolbar = extraToolbar;
        return this;
    }

    public MasterDetailSpec<T> filterFields(List<FilterField<T>> filterFields) {
        this.filterFields = filterFields;
        return this;
    }

    public MasterDetailSpec<T> filterToggleLabel(String filterToggleLabel) {
        this.filterToggleLabel = filterToggleLabel;
        return this;
    }

    public MasterDetailSpec<T> filterClearAllLabel(String filterClearAllLabel) {
        this.filterClearAllLabel = filterClearAllLabel;
        return this;
    }

    public MasterDetailSpec<T> filterChipClearAria(String filterChipClearAria) {
        this.filterChipClearAria = filterChipClearAria;
        return this;
    }

    public MasterDetailSpec<T> filterPanelAria(String filterPanelAria) {
        this.filterPanelAria = filterPanelAria;
        return this;
    }

    public MasterDetailSpec<T> readoutLoadedLabel(String readoutLoadedLabel) {
        this.readoutLoadedLabel = readoutLoadedLabel;
        return this;
    }

    public MasterDetailSpec<T> readoutMoreLabel(String readoutMoreLabel) {
        this.readoutMoreLabel = readoutMoreLabel;
        return this;
    }

    public MasterDetailSpec<T> readoutFilteredLabel(String readoutFilteredLabel) {
        this.readoutFilteredLabel = readoutFilteredLabel;
        return this;
    }

    public MasterDetailSpec<T> sentinelLabel(String sentinelLabel) {
        this.sentinelLabel = sentinelLabel;
        return this;
    }

    public MasterDetailLayout<T> build() {
        return new MasterDetailLayout<>(this);
    }
}

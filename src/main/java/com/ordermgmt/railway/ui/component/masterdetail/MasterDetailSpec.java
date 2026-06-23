package com.ordermgmt.railway.ui.component.masterdetail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    List<FilterField<T>> filterFields = new ArrayList<>();
    String filterToggleLabel = "Filter";
    String filterClearAllLabel = "Alle löschen";
    String filterChipClearAria = "Filter entfernen";
    String filterPanelAria = "Filter";

    public MasterDetailSpec<T> idExtractor(Function<T, UUID> v) {
        this.idExtractor = v;
        return this;
    }

    public MasterDetailSpec<T> cardRenderer(Function<T, Component> v) {
        this.cardRenderer = v;
        return this;
    }

    public MasterDetailSpec<T> matcher(BiPredicate<T, String> v) {
        this.matcher = v;
        return this;
    }

    public MasterDetailSpec<T> onSelect(Consumer<UUID> v) {
        this.onSelect = v;
        return this;
    }

    public MasterDetailSpec<T> shortcutNew(Runnable v) {
        this.shortcutNew = v;
        return this;
    }

    public MasterDetailSpec<T> filterPlaceholder(String v) {
        this.filterPlaceholder = v;
        return this;
    }

    public MasterDetailSpec<T> filterAriaLabel(String v) {
        this.filterAriaLabel = v;
        return this;
    }

    public MasterDetailSpec<T> filterId(String v) {
        this.filterId = v;
        return this;
    }

    public MasterDetailSpec<T> listId(String v) {
        this.listId = v;
        return this;
    }

    public MasterDetailSpec<T> detailId(String v) {
        this.detailId = v;
        return this;
    }

    public MasterDetailSpec<T> listAriaLabel(String v) {
        this.listAriaLabel = v;
        return this;
    }

    public MasterDetailSpec<T> detailAriaLabel(String v) {
        this.detailAriaLabel = v;
        return this;
    }

    public MasterDetailSpec<T> toolbarAriaLabel(String v) {
        this.toolbarAriaLabel = v;
        return this;
    }

    public MasterDetailSpec<T> emptyText(String v) {
        this.emptyText = v;
        return this;
    }

    public MasterDetailSpec<T> detailEmptyText(String v) {
        this.detailEmptyText = v;
        return this;
    }

    public MasterDetailSpec<T> announceTemplate(TriFunction<T, Integer, Integer, String> v) {
        this.announceTemplate = v;
        return this;
    }

    public MasterDetailSpec<T> extraToolbar(List<Component> v) {
        this.extraToolbar = v;
        return this;
    }

    public MasterDetailSpec<T> filterFields(List<FilterField<T>> v) {
        this.filterFields = v;
        return this;
    }

    public MasterDetailSpec<T> filterToggleLabel(String v) {
        this.filterToggleLabel = v;
        return this;
    }

    public MasterDetailSpec<T> filterClearAllLabel(String v) {
        this.filterClearAllLabel = v;
        return this;
    }

    public MasterDetailSpec<T> filterChipClearAria(String v) {
        this.filterChipClearAria = v;
        return this;
    }

    public MasterDetailSpec<T> filterPanelAria(String v) {
        this.filterPanelAria = v;
        return this;
    }

    public MasterDetailLayout<T> build() {
        return new MasterDetailLayout<>(this);
    }
}

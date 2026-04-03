package com.ordermgmt.railway.ui.component.vehicleplanning;

import java.util.List;
import java.util.UUID;

import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;

/** Left sidebar palette listing available PM reference trains as draggable items. */
public class TrainPalette extends Div {

    private final TextField searchField;
    private final Div trainList;
    private List<PmReferenceTrain> allTrains = List.of();

    /** Callback for when a train is dragged from the palette. */
    @FunctionalInterface
    public interface TrainDragHandler {
        void onTrainDrag(UUID trainId);
    }

    public TrainPalette() {
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "100%")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-right", "1px solid var(--lumo-contrast-10pct)");

        searchField = new TextField();
        searchField.setPlaceholder("Filter trains...");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> filterTrains(e.getValue()));
        searchField.getStyle().set("padding", "var(--lumo-space-xs)").set("flex-shrink", "0");

        trainList = new Div();
        trainList
                .getStyle()
                .set("overflow-y", "auto")
                .set("flex-grow", "1")
                .set("padding", "var(--lumo-space-xs)");

        add(searchField, trainList);
    }

    /** Sets the list of available trains and renders them. */
    public void setTrains(List<PmReferenceTrain> trains) {
        this.allTrains = trains;
        renderTrains(trains);
    }

    private void filterTrains(String query) {
        if (query == null || query.isBlank()) {
            renderTrains(allTrains);
            return;
        }
        String lower = query.toLowerCase();
        List<PmReferenceTrain> filtered =
                allTrains.stream()
                        .filter(
                                t -> {
                                    String otn = t.getOperationalTrainNumber();
                                    String core = t.getTridCore();
                                    return (otn != null && otn.toLowerCase().contains(lower))
                                            || (core != null && core.toLowerCase().contains(lower));
                                })
                        .toList();
        renderTrains(filtered);
    }

    private void renderTrains(List<PmReferenceTrain> trains) {
        trainList.removeAll();
        for (PmReferenceTrain train : trains) {
            trainList.add(buildTrainItem(train));
        }
    }

    private Div buildTrainItem(PmReferenceTrain train) {
        String otn = train.getOperationalTrainNumber();
        String label = otn != null ? otn : train.getTridCore();

        Div item = new Div();
        item.getStyle()
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("margin-bottom", "2px")
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("cursor", "grab")
                .set("font-size", "var(--lumo-font-size-s)");

        Span nameSpan = new Span(label);
        nameSpan.getStyle().set("font-weight", "500");

        String type = train.getTrainType();
        if (type != null && !type.isBlank()) {
            Span typeSpan = new Span(" [" + type + "]");
            typeSpan.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-xs)");
            item.add(nameSpan, typeSpan);
        } else {
            item.add(nameSpan);
        }

        // Make draggable
        DragSource<Div> dragSource = DragSource.create(item);
        dragSource.setDragData(train.getId());
        dragSource.setEffectAllowed(EffectAllowed.COPY);

        return item;
    }
}

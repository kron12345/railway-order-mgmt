package com.ordermgmt.railway.ui.component;

import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;

/**
 * Compact colored cells showing the status of each position in an order. Each cell represents one
 * position — color and icon indicate its status.
 */
public class StatusHeatmap extends Div {

    public StatusHeatmap(List<OrderPosition> positions) {
        getStyle()
                .set("display", "flex")
                .set("gap", "4px")
                .set("flex-wrap", "wrap")
                .set("align-items", "center");

        if (positions.isEmpty()) {
            Span empty = new Span("—");
            empty.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "12px");
            add(empty);
            return;
        }

        for (OrderPosition pos : positions) {
            add(createCell(pos));
        }
    }

    private Div createCell(OrderPosition pos) {
        PositionStatus status = pos.getInternalStatus();
        CellStyle cs = cellStyle(status);

        Div cell = new Div();
        cell.setText(cs.icon);
        cell.getStyle()
                .set("width", "26px")
                .set("height", "26px")
                .set("border-radius", "4px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "11px")
                .set("font-weight", "600")
                .set("cursor", "default")
                .set("background", cs.bg)
                .set("color", cs.color)
                .set("transition", "transform 0.15s");

        cell.getElement().setAttribute("title", pos.getName() + ": " + statusLabel(status));
        return cell;
    }

    private CellStyle cellStyle(PositionStatus status) {
        if (status == null) {
            return new CellStyle("rgba(148,163,184,0.08)", "var(--rom-text-muted)", "–");
        }
        return switch (status) {
            case FREIGEGEBEN, ABGESCHLOSSEN ->
                    new CellStyle("rgba(52,211,153,0.15)", "var(--rom-status-active)", "✓");
            case IN_BEARBEITUNG ->
                    new CellStyle("rgba(96,165,250,0.15)", "var(--rom-status-info)", "●");
            case UEBERARBEITEN, BEANTRAGT ->
                    new CellStyle("rgba(251,191,36,0.15)", "var(--rom-status-warning)", "!");
            case UEBERMITTELT ->
                    new CellStyle("rgba(96,165,250,0.15)", "var(--rom-status-info)", "→");
            case ANNULLIERT ->
                    new CellStyle("rgba(248,113,113,0.15)", "var(--rom-status-danger)", "✗");
        };
    }

    private String statusLabel(PositionStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case IN_BEARBEITUNG -> "In Bearbeitung";
            case FREIGEGEBEN -> "Freigegeben";
            case UEBERARBEITEN -> "Überarbeiten";
            case UEBERMITTELT -> "Übermittelt";
            case BEANTRAGT -> "Beantragt";
            case ABGESCHLOSSEN -> "Abgeschlossen";
            case ANNULLIERT -> "Annulliert";
        };
    }

    private record CellStyle(String bg, String color, String icon) {}
}

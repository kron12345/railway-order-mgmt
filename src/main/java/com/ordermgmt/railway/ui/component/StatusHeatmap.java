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
        CellStyle style = cellStyle(status);

        Div cell = new Div();
        cell.setText(style.icon);
        cell.getStyle()
                .set("width", "28px")
                .set("height", "28px")
                .set("border-radius", "4px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "13px")
                .set("font-weight", "700")
                .set("cursor", "default")
                .set("background", style.bg)
                .set("color", style.color)
                .set("border", "1px solid " + style.color)
                .set("transition", "transform 0.15s");

        // Rich tooltip via Vaadin Tooltip
        com.vaadin.flow.component.shared.Tooltip.forComponent(cell)
                .withText(pos.getName() + " — " + statusLabel(status))
                .withPosition(com.vaadin.flow.component.shared.Tooltip.TooltipPosition.TOP);
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
        return status == null ? "—" : getTranslation("position.status." + status.name());
    }

    private record CellStyle(String bg, String color, String icon) {}
}

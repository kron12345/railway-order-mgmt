package com.ordermgmt.railway.ui.component;

import java.time.format.DateTimeFormatter;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;

/**
 * Card-style tile for displaying an order position inside the accordion. Shows name, type badge,
 * route, time, resources, and status.
 */
public class PositionTile extends Div {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");

    public PositionTile(OrderPosition pos) {
        addClassName("position-tile");
        getElement().setAttribute("tabindex", "0");
        getElement().setAttribute("role", "button");
        getElement().setAttribute("aria-label", pos.getName());
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid var(--rom-border-subtle, var(--rom-border))")
                .set("border-radius", "6px")
                .set("padding", "14px 16px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "8px")
                .set("cursor", "pointer")
                .set("transition", "border-color 0.15s, transform 0.15s");

        getElement()
                .executeJs(
                        "this.addEventListener('mouseover', function(){this.style.borderColor='var(--rom-accent)'});"
                                + "this.addEventListener('mouseout', function(){this.style.borderColor=''});");

        add(createHeader(pos));
        add(createRoute(pos));
        add(createFooter(pos));
    }

    private Div createHeader(OrderPosition pos) {
        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        Span name = new Span(pos.getName());
        name.getStyle()
                .set("font-weight", "600")
                .set("font-size", "13px")
                .set("color", "var(--rom-text-primary)");

        Span typeBadge = createTypeBadge(pos);
        header.add(name, typeBadge);
        return header;
    }

    private Div createRoute(OrderPosition pos) {
        Div route = new Div();
        route.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-secondary)");

        String from = pos.getFromLocation();
        String to = pos.getToLocation();
        if (from != null && to != null) {
            route.setText(from + " → " + to);
        } else if (from != null) {
            route.setText(from);
        } else if (to != null) {
            route.setText(to);
        } else {
            route.setText("—");
        }

        if (pos.getStart() != null) {
            Span time = new Span(" · " + pos.getStart().format(DT_FMT));
            time.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "11px");
            route.add(time);
        }

        return route;
    }

    private Div createFooter(OrderPosition pos) {
        Div footer = new Div();
        footer.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-top", "4px")
                .set("padding-top", "8px")
                .set("border-top", "1px solid var(--rom-border-subtle, var(--rom-border))");

        Span statusBadge = createStatusBadge(pos.getInternalStatus());
        footer.add(createResources(), statusBadge);
        return footer;
    }

    private Div createResources() {
        Div resources = new Div();
        resources.getStyle().set("display", "flex").set("gap", "4px");
        resources.add(createResourceIcon("V", "rgba(96,165,250,0.12)", "var(--rom-status-info)", "Vehicle"));
        resources.add(createResourceIcon("P", "rgba(251,191,36,0.12)", "var(--rom-status-warning)", "Personnel"));
        resources.add(createResourceIcon("C", "rgba(52,211,153,0.12)", "var(--rom-status-active)", "Capacity"));
        return resources;
    }

    private Div createResourceIcon(String label, String bg, String color, String ariaLabel) {
        Div icon = new Div();
        icon.setText(label);
        icon.getElement().setAttribute("aria-label", ariaLabel);
        com.vaadin.flow.component.shared.Tooltip.forComponent(icon)
                .withText(ariaLabel);
        icon.getStyle()
                .set("width", "22px")
                .set("height", "22px")
                .set("border-radius", "4px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("font-size", "10px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "600")
                .set("background", bg)
                .set("color", color);
        return icon;
    }

    private Span createTypeBadge(OrderPosition pos) {
        PositionType type = pos.getType();
        boolean isTimetable = type == PositionType.FAHRPLAN;
        String color = isTimetable ? "var(--rom-status-info)" : "var(--rom-status-warning)";
        String bgColor = isTimetable ? "rgba(96,165,250,0.12)" : "rgba(251,191,36,0.12)";
        String label = type == null ? "—" : getTranslation("position.type." + type.name());

        Span badge = new Span(label);
        badge.getStyle()
                .set("font-size", "9px")
                .set("font-weight", "600")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.04em")
                .set("padding", "2px 6px")
                .set("border-radius", "3px")
                .set("color", color)
                .set("background", bgColor)
                .set("border", "1px solid " + color);
        return badge;
    }

    private Span createStatusBadge(PositionStatus status) {
        String label = statusLabel(status);
        String color = statusColor(status);

        Span badge = new Span(label);
        badge.getStyle()
                .set("font-size", "10px")
                .set("font-weight", "600")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.03em")
                .set("padding", "2px 8px")
                .set("border-radius", "4px")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 12%, transparent)")
                .set("border", "1px solid " + color);
        return badge;
    }

    private String statusLabel(PositionStatus status) {
        return status == null ? "—" : getTranslation("position.status." + status.name());
    }

    private String statusColor(PositionStatus status) {
        if (status == null) {
            return "var(--rom-text-muted)";
        }
        return switch (status) {
            case FREIGEGEBEN, ABGESCHLOSSEN -> "var(--rom-status-active)";
            case IN_BEARBEITUNG, UEBERMITTELT -> "var(--rom-status-info)";
            case UEBERARBEITEN, BEANTRAGT -> "var(--rom-status-warning)";
            case ANNULLIERT -> "var(--rom-status-danger)";
        };
    }
}

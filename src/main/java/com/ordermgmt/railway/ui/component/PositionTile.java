package com.ordermgmt.railway.ui.component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.ui.util.StringUtils;

public class PositionTile extends Div {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final int MAX_TAGS = 2;

    private final BiFunction<String, Object[], String> translator;

    public PositionTile(OrderPosition position, BiFunction<String, Object[], String> translator) {
        this.translator = translator;
        addClassName("position-tile");
        getElement().setAttribute("tabindex", "0");
        getElement().setAttribute("role", "button");
        getElement().setAttribute("aria-label", position.getName());
        applyTileStyle();

        getElement()
                .executeJs(
                        "this.addEventListener('mouseover', function(){this.style.borderColor='var(--rom-accent)'});"
                                + "this.addEventListener('mouseout', function(){this.style.borderColor=''});");

        add(createHeader(position));
        add(createRoute(position));
        Div comment = createComment(position);
        if (comment != null) {
            add(comment);
        }
        Div meta = createMeta(position);
        if (meta.getComponentCount() > 0) {
            add(meta);
        }
        add(createFooter(position));
    }

    private void applyTileStyle() {
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
    }

    private Div createHeader(OrderPosition position) {
        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center");

        Span name = new Span(position.getName());
        name.getStyle()
                .set("font-weight", "600")
                .set("font-size", "13px")
                .set("color", "var(--rom-text-primary)");

        header.add(name, createTypeBadge(position));
        return header;
    }

    private Div createRoute(OrderPosition position) {
        Div route = new Div();
        route.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-secondary)");

        route.setText(formatRoute(position));
        return route;
    }

    private String formatRoute(OrderPosition position) {
        String trainNumber = position.getOperationalTrainNumber();
        String fromLocation = position.getFromLocation();
        String toLocation = position.getToLocation();
        String trainPrefix =
                trainNumber != null && !trainNumber.isBlank() ? "OTN " + trainNumber + " · " : "";

        if (fromLocation != null && toLocation != null) {
            return trainPrefix + fromLocation + " → " + toLocation;
        }
        if (fromLocation != null) {
            return fromLocation;
        }
        if (toLocation != null) {
            return toLocation;
        }
        return "—";
    }

    private Div createComment(OrderPosition position) {
        if (position.getComment() == null || position.getComment().isBlank()) {
            return null;
        }

        Div comment = new Div();
        comment.setText(position.getComment());
        comment.getStyle()
                .set("font-size", "11px")
                .set("line-height", "1.4")
                .set("color", "var(--rom-text-muted)")
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "2")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden");
        return comment;
    }

    private Div createMeta(OrderPosition position) {
        Div meta = new Div();
        meta.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "6px");

        if (position.getStart() != null || position.getEnd() != null) {
            meta.add(createMetaBadge(formatTimeWindow(position), "var(--rom-status-info)"));
        }
        if (position.getServiceType() != null && !position.getServiceType().isBlank()) {
            meta.add(createMetaBadge(position.getServiceType(), "var(--rom-status-warning)"));
        }

        List<String> tags = StringUtils.splitTags(position.getTags());
        for (int i = 0; i < Math.min(tags.size(), MAX_TAGS); i++) {
            meta.add(createMetaBadge("#" + tags.get(i), "var(--rom-text-muted)"));
        }
        if (tags.size() > MAX_TAGS) {
            meta.add(createMetaBadge("+" + (tags.size() - MAX_TAGS), "var(--rom-text-muted)"));
        }

        return meta;
    }

    private Div createFooter(OrderPosition position) {
        Div footer = new Div();
        footer.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("margin-top", "4px")
                .set("padding-top", "8px")
                .set("border-top", "1px solid var(--rom-border-subtle, var(--rom-border))");

        footer.add(createPurchaseBadge(position), createStatusBadge(position.getInternalStatus()));
        return footer;
    }

    private Span createPurchaseBadge(OrderPosition position) {
        int purchaseCount =
                position.getPurchasePositions() != null
                        ? position.getPurchasePositions().size()
                        : 0;
        Span badge = new Span(purchaseCount + " " + t("purchase.calendar.btn"));
        badge.getStyle()
                .set("font-size", "10px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "600")
                .set("padding", "2px 8px")
                .set("border-radius", "4px")
                .set("color", purchaseCount > 0 ? "var(--rom-accent)" : "var(--rom-text-muted)")
                .set(
                        "background",
                        purchaseCount > 0 ? "rgba(45,212,191,0.08)" : "rgba(148,163,184,0.08)")
                .set(
                        "border",
                        purchaseCount > 0
                                ? "1px solid rgba(45,212,191,0.25)"
                                : "1px solid var(--rom-border)");
        return badge;
    }

    private Span createTypeBadge(OrderPosition position) {
        PositionType type = position.getType();
        boolean isTimetable = type == PositionType.FAHRPLAN;
        String color = isTimetable ? "var(--rom-status-info)" : "var(--rom-status-warning)";
        String bgColor = isTimetable ? "rgba(96,165,250,0.12)" : "rgba(251,191,36,0.12)";
        String label = type == null ? "—" : t("position.type." + type.name());

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
        return status == null ? "—" : t("position.status." + status.name());
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

    private Span createMetaBadge(String text, String color) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("font-size", "10px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-weight", "500")
                .set("padding", "2px 6px")
                .set("border-radius", "4px")
                .set("color", color)
                .set("background", "color-mix(in srgb, " + color + " 10%, transparent)")
                .set("border", "1px solid color-mix(in srgb, " + color + " 20%, transparent)");
        return badge;
    }

    private String formatTimeWindow(OrderPosition position) {
        String start =
                position.getStart() != null
                        ? position.getStart().format(DATE_TIME_FORMATTER)
                        : "—";
        String end =
                position.getEnd() != null ? position.getEnd().format(DATE_TIME_FORMATTER) : "—";
        if (position.getStart() != null && position.getEnd() != null) {
            return start + " → " + end;
        }
        return position.getStart() != null ? start : end;
    }

    private String t(String key) {
        return translator.apply(key, new Object[0]);
    }
}

package com.ordermgmt.railway.ui.component.order;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.order.model.OrderPositionVersion;
import com.ordermgmt.railway.domain.order.model.PositionChangeSource;
import com.ordermgmt.railway.ui.component.StatusBadge;

/**
 * Compact change feed: lists an expression's versions (base + overrides) — or, on a train identity,
 * the aggregated changes of all its expressions — each with its source and validity window, so the
 * planner can trace what changed when and from where (self vs. infrastructure).
 */
class VersionFeed extends Div {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy");

    VersionFeed(List<OrderPositionVersion> versions, BiFunction<String, Object[], String> t) {
        getStyle()
                .set("margin", "0 12px 8px 12px")
                .set("padding", "8px 10px")
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px");

        Span title = new Span(t.apply("version.feed.title", new Object[0]));
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "12px")
                .set("color", "var(--rom-text-secondary)");
        add(title);

        versions.stream()
                .sorted(
                        Comparator.comparing(
                                OrderPositionVersion::getVersionNumber,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .forEach(version -> add(line(version, t)));
    }

    private HorizontalLayout line(
            OrderPositionVersion version, BiFunction<String, Object[], String> t) {
        PositionChangeSource source =
                version.getSource() != null ? version.getSource() : PositionChangeSource.INITIAL;
        StatusBadge badge =
                new StatusBadge(
                        t.apply("version.source." + source.name(), new Object[0]),
                        badgeType(source));

        String range = "";
        if (version.getValidFrom() != null) {
            range =
                    DATE_FORMAT.format(version.getValidFrom())
                            + " – "
                            + (version.getValidTo() != null
                                    ? DATE_FORMAT.format(version.getValidTo())
                                    : "…")
                            + " · ";
        }
        Span text =
                new Span(
                        range
                                + (version.getChangeSummary() != null
                                        ? version.getChangeSummary()
                                        : ""));
        text.getStyle().set("font-size", "12px").set("color", "var(--rom-text-primary)");

        HorizontalLayout row = new HorizontalLayout(badge, text);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.getStyle().set("margin-top", "4px");
        return row;
    }

    private static StatusBadge.StatusType badgeType(PositionChangeSource src) {
        return switch (src) {
            case MODIFICATION -> StatusBadge.StatusType.INFO;
            case ALTERATION -> StatusBadge.StatusType.WARNING;
            case CANCELLATION -> StatusBadge.StatusType.DANGER;
            case INITIAL -> StatusBadge.StatusType.NEUTRAL;
        };
    }
}

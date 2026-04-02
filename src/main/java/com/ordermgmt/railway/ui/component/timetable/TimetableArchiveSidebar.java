package com.ordermgmt.railway.ui.component.timetable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;

/** Right-side sidebar for the timetable archive view: map, validity, metadata. */
public class TimetableArchiveSidebar extends VerticalLayout {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public TimetableArchiveSidebar(
            TimetableArchive archive,
            OrderPosition position,
            TimetableRouteResult routeResult,
            TimetableArchiveService archiveService) {
        setPadding(false);
        setSpacing(false);
        setSizeFull();
        getStyle().set("gap", "var(--lumo-space-s)").set("overflow-y", "auto");

        add(
                createMapCard(routeResult),
                createValidityCard(position, archiveService),
                createMetadataCard(archive));
    }

    private Component createMapCard(TimetableRouteResult routeResult) {
        Div card = sidebarCard();
        TimetableMap map = new TimetableMap();
        map.getStyle().set("min-height", "250px").set("border-radius", "4px");
        if (routeResult != null && !routeResult.points().isEmpty()) {
            map.setRoute(routeResult.points());
        }
        card.add(map);
        return card;
    }

    private Component createValidityCard(
            OrderPosition position, TimetableArchiveService archiveService) {
        Div card = sidebarCard();
        card.add(sidebarLabel(t("timetable.archive.validity")));

        List<LocalDate> dates = archiveService.parseValidityDates(position.getValidity());
        if (dates.isEmpty()) {
            card.add(sidebarValue("\u2014"));
        } else {
            List<LocalDate> sorted = dates.stream().sorted().toList();
            LocalDate first = sorted.getFirst();
            LocalDate last = sorted.getLast();
            String range = first.format(DATE_FMT) + " \u2013 " + last.format(DATE_FMT);
            card.add(sidebarValue(range));
            card.add(sidebarMuted(t("timetable.archive.days", dates.size())));
        }
        return card;
    }

    private Component createMetadataCard(TimetableArchive archive) {
        Div card = sidebarCard();
        card.add(sidebarLabel(t("timetable.archive.metadata")));

        if (hasText(archive.getOperationalTrainNumber())) {
            card.add(metaRow(t("timetable.otn"), archive.getOperationalTrainNumber()));
        }
        if (hasText(archive.getTimetableType())) {
            card.add(metaRow(t("position.type"), archive.getTimetableType()));
        }
        if (hasText(archive.getRouteSummary())) {
            card.add(metaRow(t("timetable.route.map"), archive.getRouteSummary()));
        }
        card.add(metaRow(t("timetable.archive.created"), formatInstant(archive.getCreatedAt())));
        card.add(metaRow(t("timetable.archive.updated"), formatInstant(archive.getUpdatedAt())));
        return card;
    }

    // ── Reusable sidebar primitives ───────────────────────────────────

    private Div sidebarCard() {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "12px 16px")
                .set("box-sizing", "border-box");
        return card;
    }

    private Span sidebarLabel(String text) {
        Span label = new Span(text);
        label.getStyle()
                .set("display", "block")
                .set("font-size", "11px")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.06em")
                .set("color", "var(--rom-accent)")
                .set("margin-bottom", "6px");
        return label;
    }

    private Span sidebarValue(String text) {
        Span val = new Span(text);
        val.getStyle()
                .set("display", "block")
                .set("font-size", "13px")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-primary)");
        return val;
    }

    private Span sidebarMuted(String text) {
        Span val = new Span(text);
        val.getStyle()
                .set("display", "block")
                .set("font-size", "11px")
                .set("color", "var(--rom-text-muted)")
                .set("margin-top", "2px");
        return val;
    }

    private Div metaRow(String label, String value) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("padding", "3px 0")
                .set("font-size", "11px")
                .set("border-bottom", "1px solid var(--rom-border-subtle)");
        Span lbl = new Span(label);
        lbl.getStyle().set("color", "var(--rom-text-muted)");
        Span val = new Span(value != null ? value : "\u2014");
        val.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("color", "var(--rom-text-primary)");
        row.add(lbl, val);
        return row;
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "\u2014";
        }
        return instant.atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String t(String key, Object... params) {
        return getTranslation(key, params);
    }
}

package com.ordermgmt.railway.ui.view.order;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.timetable.model.RoutePointRole;
import com.ordermgmt.railway.domain.timetable.model.TimetableArchive;
import com.ordermgmt.railway.domain.timetable.model.TimetableRouteResult;
import com.ordermgmt.railway.domain.timetable.model.TimetableRowData;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;
import com.ordermgmt.railway.domain.timetable.service.TimetableRoutingService;
import com.ordermgmt.railway.ui.component.ValidityCalendar;
import com.ordermgmt.railway.ui.component.timetable.TimetableRouteStep;
import com.ordermgmt.railway.ui.component.timetable.TimetableTableStep;

/**
 * Loads existing position data into the timetable builder. Extracted from {@link
 * TimetableBuilderView} to keep file sizes manageable.
 */
class TimetableDataLoader {

    private final TimetableArchiveService archiveService;
    private final TimetableRoutingService routingService;
    private final Map<String, OperationalPoint> operationalPointsByUopid;
    private final Component translationSource;

    TimetableDataLoader(
            TimetableArchiveService archiveService,
            TimetableRoutingService routingService,
            Map<String, OperationalPoint> operationalPointsByUopid,
            Component translationSource) {
        this.archiveService = archiveService;
        this.routingService = routingService;
        this.operationalPointsByUopid = operationalPointsByUopid;
        this.translationSource = translationSource;
    }

    /** Result of loading existing data. */
    record LoadResult(
            ValidityCalendar calendar,
            TimetableRouteResult route,
            List<TimetableRowData> rows,
            boolean switchToTable) {}

    /**
     * Loads existing position data and returns the result.
     *
     * @return the loaded data, or a result with empty rows and no switch if nothing to load
     */
    LoadResult load(
            Order order,
            OrderPosition existingPosition,
            TextField positionName,
            TextField otnField,
            TextArea commentField,
            Runnable readTagsFn,
            TimetableRouteStep routeStep,
            TimetableTableStep tableStep,
            String routeSummaryText) {

        positionName.setValue(
                existingPosition != null ? textOrBlank(existingPosition.getName()) : "");
        commentField.setValue(
                existingPosition != null ? textOrBlank(existingPosition.getComment()) : "");
        readTagsFn.run();

        if (existingPosition != null) {
            archiveService
                    .findArchive(existingPosition)
                    .ifPresent(a -> otnField.setValue(textOrBlank(a.getOperationalTrainNumber())));
        }

        LocalDate min = order.getValidFrom() != null ? order.getValidFrom() : LocalDate.now();
        LocalDate max = order.getValidTo() != null ? order.getValidTo() : min.plusMonths(3);
        ValidityCalendar cal = new ValidityCalendar(min, max);
        if (existingPosition != null) {
            cal.setSelectedDates(archiveService.parseValidityDates(existingPosition.getValidity()));
        }

        if (existingPosition == null) {
            routeStep.getRouteSummary().setText(t("timetable.route.empty"));
            routeStep.getRouteError().setText("");
            return new LoadResult(cal, new TimetableRouteResult(List.of(), 0D), List.of(), false);
        }

        Optional<TimetableArchive> archive = archiveService.findArchive(existingPosition);
        if (archive.isPresent()) {
            List<TimetableRowData> rows = archiveService.readRows(archive.get());
            TimetableRouteResult route = routingService.routeFromStoredRows(rows);
            routeStep.setRoute(route);
            prefillRouteInputsFromRows(rows, routeStep);
            routeStep.getRouteSummary().setText(routeSummaryText);
            routeStep.getRouteError().setText("");
            tableStep.setRows(new ArrayList<>(rows));
            return new LoadResult(cal, route, rows, true);
        }

        return prefillLegacyRoute(existingPosition, cal, routeStep, tableStep);
    }

    private void prefillRouteInputsFromRows(
            List<TimetableRowData> rows, TimetableRouteStep routeStep) {
        if (rows.isEmpty()) {
            return;
        }
        List<TimetableRouteStep.ViaData> vias = new ArrayList<>();
        for (TimetableRowData row : rows) {
            if (row.getRoutePointRole() == RoutePointRole.VIA) {
                vias.add(
                        new TimetableRouteStep.ViaData(
                                operationalPointsByUopid.get(row.getUopid()),
                                Boolean.TRUE.equals(row.getHalt()),
                                row.getActivityCode()));
            }
        }
        routeStep.prefillFrom(
                operationalPointsByUopid.get(rows.getFirst().getUopid()),
                operationalPointsByUopid.get(rows.getLast().getUopid()),
                vias);
    }

    private LoadResult prefillLegacyRoute(
            OrderPosition existingPosition,
            ValidityCalendar cal,
            TimetableRouteStep routeStep,
            TimetableTableStep tableStep) {
        Optional<OperationalPoint> fromPt =
                routingService.resolveLegacyPoint(existingPosition.getFromLocation());
        Optional<OperationalPoint> toPt =
                routingService.resolveLegacyPoint(existingPosition.getToLocation());
        routeStep.prefillFrom(fromPt.orElse(null), toPt.orElse(null), null);
        if (existingPosition.getStart() != null) {
            routeStep.setDepartureAnchor(existingPosition.getStart().toLocalTime());
        }
        if (existingPosition.getEnd() != null && routeStep.getDepartureAnchor() == null) {
            routeStep.setArrivalAnchor(existingPosition.getEnd().toLocalTime());
        }
        if (fromPt.isPresent() && toPt.isPresent()) {
            List<TimetableRowData> rows =
                    routeStep.calculateRoute(
                            routeStep.getDepartureAnchor(), routeStep.getArrivalAnchor());
            if (rows != null) {
                tableStep.setRows(new ArrayList<>(rows));
                return new LoadResult(cal, routeStep.getCurrentRoute(), rows, false);
            }
        }
        routeStep.getRouteSummary().setText(t("timetable.route.empty"));
        routeStep.getRouteError().setText(t("timetable.route.legacyUnresolved"));
        return new LoadResult(cal, new TimetableRouteResult(List.of(), 0D), List.of(), false);
    }

    private String textOrBlank(String v) {
        return v != null ? v : "";
    }

    private String t(String key, Object... params) {
        return translationSource.getTranslation(key, params);
    }
}

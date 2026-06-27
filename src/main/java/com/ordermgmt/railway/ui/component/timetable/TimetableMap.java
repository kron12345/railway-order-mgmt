package com.ordermgmt.railway.ui.component.timetable;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.timetable.model.TimetableRoutePoint;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/** Leaflet-based route map for the timetable builder. */
@Tag("rom-timetable-map")
@JsModule("./components/rom-timetable-map.ts")
@NpmPackage(value = "leaflet", version = "1.9.4")
public class TimetableMap extends Div {

    public TimetableMap() {
        setWidthFull();
        getElement().getStyle().set("display", "block").set("min-height", "100%");
    }

    public void setRoute(List<TimetableRoutePoint> routePoints) {
        JsonArray jsonPoints = Json.createArray();
        for (int index = 0; index < routePoints.size(); index++) {
            TimetableRoutePoint point = routePoints.get(index);
            JsonObject item = Json.createObject();
            item.put("uopid", point.uopid());
            item.put("name", point.name());
            item.put("country", point.country());
            item.put("latitude", point.latitude() != null ? point.latitude() : Double.NaN);
            item.put("longitude", point.longitude() != null ? point.longitude() : Double.NaN);
            item.put("role", point.routePointRole().name());
            item.put(
                    "distanceFromStartMeters",
                    point.distanceFromStartMeters() != null ? point.distanceFromStartMeters() : 0D);
            item.put("journeyLocationType", point.journeyLocationType());
            jsonPoints.set(index, item);
        }
        getElement().callJsFunction("setRoute", jsonPoints);
    }

    public void clearRoute() {
        getElement().callJsFunction("clearRoute");
    }

    /** Renders the given operational points (the current viewport's set) as background markers. */
    public void setBackgroundOperationalPoints(List<OperationalPoint> operationalPoints) {
        JsonArray jsonOperationalPoints = Json.createArray();
        int jsonIndex = 0;
        for (OperationalPoint operationalPoint : operationalPoints) {
            if (operationalPoint.getLatitude() == null
                    || operationalPoint.getLongitude() == null) {
                continue;
            }
            JsonObject item = Json.createObject();
            item.put("uopid", operationalPoint.getUopid());
            item.put("name", operationalPoint.getName());
            item.put("latitude", operationalPoint.getLatitude());
            item.put("longitude", operationalPoint.getLongitude());
            jsonOperationalPoints.set(jsonIndex++, item);
        }
        getElement().callJsFunction("setOperationalPoints", jsonOperationalPoints);
    }

    /** Registers a callback that fires when the user clicks an OP marker on the map. */
    public void addOpSelectedListener(Consumer<String> callback) {
        getElement()
                .addEventListener(
                        "op-selected",
                        event -> {
                            String uopid = event.getEventData().getString("event.detail.uopid");
                            if (uopid != null && !uopid.isBlank()) {
                                callback.accept(uopid);
                            }
                        })
                .addEventData("event.detail.uopid");
    }

    /**
     * Registers a callback that fires (debounced) when the map's viewport changes — its bounds
     * drive a server-side fetch of just the operational points in view, so the background never
     * loads all ~19,300 points up front.
     */
    public void addBoundsChangedListener(BoundsListener listener) {
        getElement()
                .addEventListener(
                        "bounds-changed",
                        event -> {
                            var eventData = event.getEventData();
                            listener.onBoundsChanged(
                                    eventData.getNumber("event.detail.south"),
                                    eventData.getNumber("event.detail.west"),
                                    eventData.getNumber("event.detail.north"),
                                    eventData.getNumber("event.detail.east"),
                                    (int) eventData.getNumber("event.detail.zoom"));
                        })
                .addEventData("event.detail.south")
                .addEventData("event.detail.west")
                .addEventData("event.detail.north")
                .addEventData("event.detail.east")
                .addEventData("event.detail.zoom");
    }

    /** Viewport bounds callback: south/west/north/east in degrees, plus the current zoom level. */
    public interface BoundsListener {
        void onBoundsChanged(double south, double west, double north, double east, int zoom);
    }
}

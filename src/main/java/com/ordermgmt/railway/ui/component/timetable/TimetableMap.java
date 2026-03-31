package com.ordermgmt.railway.ui.component.timetable;

import java.util.List;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

import com.ordermgmt.railway.domain.timetable.model.TimetableRoutePoint;

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
}

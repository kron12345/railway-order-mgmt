package com.ordermgmt.railway.ui.view.settings;

import java.util.function.BiFunction;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;

/** Browse and search imported operational points. */
public class TopologyTab extends Div {

    private final OperationalPointRepository opRepo;
    private final Grid<OperationalPoint> grid = new Grid<>(OperationalPoint.class, false);
    private final BiFunction<String, Object[], String> t;

    public TopologyTab(OperationalPointRepository opRepo,
                       BiFunction<String, Object[], String> translator) {
        this.opRepo = opRepo;
        this.t = translator;
        setWidthFull();

        H3 title = new H3(tr("settings.topology"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("margin", "0 0 var(--lumo-space-s) 0")
                .set("font-size", "var(--lumo-font-size-m)");
        add(title);

        TextField search = new TextField();
        search.setPlaceholder(tr("common.search") + "...");
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidthFull();
        search.getStyle().set("margin-bottom", "var(--lumo-space-s)");
        search.addValueChangeListener(e -> filter(e.getValue()));
        add(search);

        grid.addColumn(OperationalPoint::getUopid)
                .setHeader("UOPID").setWidth("100px").setSortable(true);
        grid.addColumn(OperationalPoint::getName)
                .setHeader(tr("settings.topology.name")).setSortable(true).setFlexGrow(2);
        grid.addColumn(OperationalPoint::getTafTapCode)
                .setHeader("PLC").setWidth("100px");
        grid.addColumn(op -> opTypeName(op.getOpType()))
                .setHeader(tr("settings.topology.type")).setWidth("140px");
        grid.addColumn(OperationalPoint::getCountry)
                .setHeader(tr("settings.import.country")).setWidth("60px");
        grid.addColumn(op -> op.getLatitude() != null
                        ? String.format("%.4f, %.4f", op.getLatitude(), op.getLongitude()) : "—")
                .setHeader("Koordinaten").setWidth("160px");

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        grid.setPageSize(50);

        Div wrap = new Div(grid);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("overflow", "hidden");
        add(wrap);

        grid.setItems(opRepo.findAll());
    }

    private void filter(String query) {
        if (query == null || query.isBlank()) {
            grid.setItems(opRepo.findAll());
        } else {
            grid.setItems(opRepo.findByNameContainingIgnoreCase(query));
        }
    }

    private String opTypeName(Integer type) {
        if (type == null) return "—";
        return switch (type) {
            case 10 -> "Station";
            case 20 -> "Abzweigung";
            case 30 -> "Gueter-Terminal";
            case 70 -> "Kleinstation";
            case 80 -> "Personenhalt";
            case 90 -> "Grenzpunkt (inl.)";
            case 110 -> "Rangierbahnhof";
            case 130 -> "Grenzpunkt (int.)";
            default -> String.valueOf(type);
        };
    }

    private String tr(String key) {
        return t.apply(key, new Object[0]);
    }
}

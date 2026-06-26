package com.ordermgmt.railway.ui.view.settings;

import java.util.List;
import java.util.function.BiFunction;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.ui.component.DataReadout;

/** Browse and search imported operational points — lazy, server-side filtered/sorted grid. */
public class TopologyTab extends Div {

    private final OperationalPointRepository opRepo;
    private final Grid<OperationalPoint> grid = new Grid<>(OperationalPoint.class, false);
    private final BiFunction<String, Object[], String> t;
    private final DataReadout readout = new DataReadout();

    private String currentFilter = "";
    private long totalCount = 0;
    private int rangeFrom = 0;
    private int rangeTo = 0;

    public TopologyTab(
            OperationalPointRepository opRepo, BiFunction<String, Object[], String> translator) {
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
        search.addValueChangeListener(
                e -> {
                    currentFilter = e.getValue() == null ? "" : e.getValue().trim();
                    grid.getDataProvider().refreshAll();
                });
        add(search);

        grid.addColumn(OperationalPoint::getUopid)
                .setHeader("UOPID")
                .setWidth("100px")
                .setSortProperty("uopid");
        grid.addColumn(OperationalPoint::getName)
                .setHeader(tr("settings.topology.name"))
                .setFlexGrow(2)
                .setSortProperty("name");
        grid.addColumn(OperationalPoint::getTafTapCode)
                .setHeader("PLC")
                .setWidth("100px")
                .setSortProperty("tafTapCode");
        grid.addColumn(op -> opTypeName(op.getOpType()))
                .setHeader(tr("settings.topology.type"))
                .setWidth("140px")
                .setSortProperty("opType");
        grid.addColumn(OperationalPoint::getCountry)
                .setHeader(tr("settings.import.country"))
                .setWidth("60px")
                .setSortProperty("country");
        grid.addColumn(
                        op ->
                                op.getLatitude() != null
                                        ? String.format(
                                                "%.4f, %.4f", op.getLatitude(), op.getLongitude())
                                        : "—")
                .setHeader("Koordinaten")
                .setWidth("160px");

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        grid.setPageSize(50);

        Div wrap = new Div(grid, readout);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("overflow", "hidden");
        add(wrap);

        // Lazy, server-side: fetch one page at a time (sorted server-side, stable id tie-breaker);
        // the count callback drives an exact total (cheap, indexed) for the readout.
        grid.setItems(
                query -> {
                    List<OperationalPoint> content =
                            opRepo.searchByNameOrUopid(currentFilter, stablePageable(query));
                    rangeFrom = content.isEmpty() ? 0 : (int) query.getOffset() + 1;
                    rangeTo = (int) query.getOffset() + content.size();
                    updateReadout();
                    return content.stream();
                },
                query -> {
                    totalCount =
                            opRepo.countByNameContainingIgnoreCaseOrUopidContainingIgnoreCase(
                                    currentFilter, currentFilter);
                    updateReadout();
                    return (int) totalCount;
                });
    }

    /**
     * Vaadin sort → Spring Pageable, always with an id tie-breaker so paging never skips/repeats.
     */
    private static Pageable stablePageable(com.vaadin.flow.data.provider.Query<?, ?> query) {
        Pageable base = VaadinSpringDataHelpers.toSpringPageRequest(query);
        Sort sort =
                base.getSort().isSorted()
                        ? base.getSort().and(Sort.by(Sort.Direction.ASC, "id"))
                        : Sort.by(Sort.Direction.ASC, "name")
                                .and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(base.getPageNumber(), base.getPageSize(), sort);
    }

    private void updateReadout() {
        if (totalCount == 0) {
            readout.setStatus(tr("data.readout.empty"));
            return;
        }
        String range = rangeFrom + "–" + rangeTo + " / " + String.format("%,d", totalCount);
        boolean filtered = currentFilter != null && !currentFilter.isBlank();
        readout.setStatus(filtered ? range + " · " + tr("data.readout.filtered") : range);
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

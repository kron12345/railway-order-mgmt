package com.ordermgmt.railway.ui.view.dashboard;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.domain.order.service.OrderService.DashboardStats;
import com.ordermgmt.railway.ui.component.KpiCard;
import com.ordermgmt.railway.ui.component.KpiRow;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Landing page showing live order KPIs and a per-phase breakdown. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final transient OrderService orderService;

    public DashboardView(OrderService orderService) {
        this.orderService = orderService;
        setPadding(true);
        setSpacing(false);
        setSizeFull();
        getStyle().set("background", "var(--rom-bg-primary)");

        DashboardStats stats = orderService.dashboardStats();

        add(createHeader());
        add(createKpiSection(stats));
        add(createPhaseBreakdown(stats));
    }

    private Div createHeader() {
        Div header = new Div();
        H2 title = new H2(getTranslation("nav.dashboard"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-weight", "600")
                .set("margin", "0 0 var(--lumo-space-xs) 0");

        Span subtitle = new Span(getTranslation("dashboard.subtitle"));
        subtitle.getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("font-size", "var(--lumo-font-size-s)");

        header.add(title, subtitle);
        header.getStyle().set("margin-bottom", "var(--lumo-space-l)");
        return header;
    }

    private KpiRow createKpiSection(DashboardStats stats) {
        KpiCard orders =
                new KpiCard(
                        getTranslation("dashboard.kpi.active_orders"),
                        Long.toString(stats.active()),
                        "var(--rom-accent)");
        KpiCard planning =
                new KpiCard(
                        getTranslation("dashboard.kpi.in_planning"),
                        Long.toString(stats.inPlanning()),
                        "var(--rom-status-info)");
        KpiCard production =
                new KpiCard(
                        getTranslation("dashboard.kpi.in_production"),
                        Long.toString(stats.inProduction()),
                        "var(--rom-status-warning)");
        KpiCard critical =
                new KpiCard(
                        getTranslation("dashboard.kpi.critical_deadlines"),
                        Long.toString(stats.criticalDeadlines()),
                        "var(--rom-status-danger)");

        return new KpiRow(orders, planning, production, critical);
    }

    private Div createPhaseBreakdown(DashboardStats stats) {
        Div panel = new Div();
        panel.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-l)")
                .set("flex", "1")
                .set("box-sizing", "border-box");

        H3 title = new H3(getTranslation("dashboard.breakdown.title"));
        title.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("margin", "0 0 var(--lumo-space-m) 0");
        panel.add(title);

        if (stats.total() == 0) {
            Span empty = new Span(getTranslation("dashboard.overview.placeholder"));
            empty.getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-family", "'JetBrains Mono', monospace")
                    .set("font-size", "var(--lumo-font-size-s)");
            panel.add(empty);
            return panel;
        }

        long total = stats.total();
        for (ProcessStatus phase : ProcessStatus.values()) {
            panel.add(buildPhaseRow(phase, stats.byStatus().getOrDefault(phase, 0L), total));
        }
        return panel;
    }

    private Div buildPhaseRow(ProcessStatus phase, long count, long total) {
        Span label = new Span(getTranslation("process." + phase.name()));
        label.getStyle().set("color", "var(--rom-text-secondary)").set("min-width", "160px");

        Div bar = new Div();
        int pct = total == 0 ? 0 : (int) Math.round(100.0 * count / total);
        bar.getStyle()
                .set("height", "8px")
                .set("width", pct + "%")
                .set("min-width", count > 0 ? "4px" : "0")
                .set("background", "var(--rom-accent)")
                .set("border-radius", "4px");
        Div track = new Div(bar);
        track.getStyle()
                .set("flex", "1")
                .set("height", "8px")
                .set("background", "var(--rom-bg-secondary)")
                .set("border-radius", "4px")
                .set("overflow", "hidden");

        Span value = new Span(Long.toString(count));
        value.getStyle()
                .set("color", "var(--rom-text-primary)")
                .set("font-weight", "600")
                .set("min-width", "32px")
                .set("text-align", "right");

        Div row = new Div(label, track, value);
        row.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xs) 0");
        return row;
    }
}

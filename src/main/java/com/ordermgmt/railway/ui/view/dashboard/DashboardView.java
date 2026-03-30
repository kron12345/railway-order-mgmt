package com.ordermgmt.railway.ui.view.dashboard;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.ui.component.KpiCard;
import com.ordermgmt.railway.ui.component.KpiRow;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Landing page showing the dashboard shell and KPI placeholders. */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView() {
        setPadding(true);
        setSpacing(false);
        setSizeFull();
        getStyle().set("background", "var(--rom-bg-primary)");

        add(createHeader());
        add(createKpiSection());
        add(createQuickOverview());
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

    private KpiRow createKpiSection() {
        KpiCard orders =
                new KpiCard(
                        getTranslation("dashboard.kpi.active_orders"), "—", "var(--rom-accent)");
        KpiCard planning =
                new KpiCard(
                        getTranslation("dashboard.kpi.in_planning"), "—", "var(--rom-status-info)");
        KpiCard production =
                new KpiCard(
                        getTranslation("dashboard.kpi.in_production"),
                        "—",
                        "var(--rom-status-warning)");
        KpiCard critical =
                new KpiCard(
                        getTranslation("dashboard.kpi.critical_deadlines"),
                        "—",
                        "var(--rom-status-danger)");

        return new KpiRow(orders, planning, production, critical);
    }

    private Div createQuickOverview() {
        Div overview = new Div();
        overview.getStyle()
                .set("background", "var(--rom-bg-card)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "var(--lumo-space-l)")
                .set("flex", "1");

        Span placeholder = new Span(getTranslation("dashboard.overview.placeholder"));
        placeholder
                .getStyle()
                .set("color", "var(--rom-text-muted)")
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "var(--lumo-font-size-s)");

        overview.add(placeholder);
        return overview;
    }
}

package com.ordermgmt.railway.ui.component;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/** Horizontal row of KPI cards with consistent spacing. */
public class KpiRow extends HorizontalLayout {

    public KpiRow(KpiCard... cards) {
        setWidthFull();
        setPadding(false);
        setSpacing(true);
        getStyle().set("gap", "var(--lumo-space-m)").set("margin-bottom", "var(--lumo-space-m)");

        for (KpiCard card : cards) {
            add(card);
        }
    }
}

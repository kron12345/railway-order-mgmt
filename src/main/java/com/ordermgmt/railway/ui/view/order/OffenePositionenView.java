package com.ordermgmt.railway.ui.view.order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;
import com.ordermgmt.railway.domain.pathmanager.model.PmReferenceTrain;
import com.ordermgmt.railway.domain.pathmanager.service.PathManagerService;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Overview of "open" work: positions with unbooked purchases + unassigned RailOpt timetables. */
@Route(value = "offene-positionen", layout = MainLayout.class)
@PageTitle("Offene Positionen")
@PermitAll
public class OffenePositionenView extends VerticalLayout {

    private final PurchasePositionRepository purchasePositionRepository;
    private final PathManagerService pathManagerService;

    public OffenePositionenView(
            PurchasePositionRepository purchasePositionRepository,
            PathManagerService pathManagerService) {
        this.purchasePositionRepository = purchasePositionRepository;
        this.pathManagerService = pathManagerService;

        setWidthFull();
        setPadding(true);

        H2 title = new H2(getTranslation("offen.title"));
        title.getStyle().set("margin", "0 0 var(--lumo-space-m) 0");
        add(title, buildOpenPurchasesSection(), buildUnassignedSection());
    }

    private Div buildOpenPurchasesSection() {
        Div section = new Div();
        section.setWidthFull();
        section.add(sectionHeader(getTranslation("offen.section.purchases")));

        List<PurchasePosition> open =
                purchasePositionRepository.findOpenWithOrder(PurchaseStatus.OFFEN, "BOOKED");
        Map<UUID, List<PurchasePosition>> byPosition =
                open.stream()
                        .filter(pp -> pp.getOrderPosition() != null)
                        .collect(
                                Collectors.groupingBy(
                                        pp -> pp.getOrderPosition().getId(),
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        if (byPosition.isEmpty()) {
            section.add(emptyHint(getTranslation("offen.purchases.empty")));
            return section;
        }
        byPosition.forEach(
                (positionId, purchases) ->
                        section.add(
                                openPurchaseRow(
                                        purchases.get(0).getOrderPosition(), purchases.size())));
        return section;
    }

    private Component openPurchaseRow(OrderPosition pos, int openCount) {
        String orderNo = pos.getOrder() != null ? pos.getOrder().getOrderNumber() : "—";
        Span label =
                new Span(
                        orderNo
                                + " · "
                                + pos.getName()
                                + "  ·  "
                                + getTranslation("offen.openCount", openCount));
        label.getStyle().set("font-size", "13px");

        Button toOrder =
                new Button(getTranslation("offen.toOrder"), VaadinIcon.ARROW_RIGHT.create());
        toOrder.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        if (pos.getOrder() != null) {
            toOrder.addClickListener(
                    e -> UI.getCurrent().navigate("orders/" + pos.getOrder().getId()));
        }
        return row(label, toOrder);
    }

    private Div buildUnassignedSection() {
        Div section = new Div();
        section.setWidthFull();
        section.getStyle().set("margin-top", "var(--lumo-space-l)");
        section.add(sectionHeader(getTranslation("offen.section.unassigned")));

        List<PmReferenceTrain> trains = pathManagerService.findUnassignedTrains();
        if (trains.isEmpty()) {
            section.add(emptyHint(getTranslation("offen.unassigned.empty")));
            return section;
        }
        for (PmReferenceTrain t : trains) {
            String otn =
                    t.getOperationalTrainNumber() != null
                            ? "OTN " + t.getOperationalTrainNumber()
                            : t.getTridCore();
            String validity =
                    (t.getCalendarStart() != null && t.getCalendarEnd() != null)
                            ? "  ·  " + t.getCalendarStart() + " – " + t.getCalendarEnd()
                            : "";
            Span label = new Span(otn + validity);
            label.getStyle().set("font-size", "13px");
            Span hint = new Span(getTranslation("offen.unassignedHint"));
            hint.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "12px");
            section.add(row(label, hint));
        }
        return section;
    }

    private H4 sectionHeader(String text) {
        H4 h = new H4(text);
        h.getStyle().set("margin", "var(--lumo-space-m) 0 var(--lumo-space-s) 0");
        return h;
    }

    private Span emptyHint(String text) {
        Span s = new Span(text);
        s.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "13px");
        return s;
    }

    private Div row(Component left, Component right) {
        HorizontalLayout hl = new HorizontalLayout(left, right);
        hl.setWidthFull();
        hl.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        hl.setAlignItems(FlexComponent.Alignment.CENTER);
        Div card = new Div(hl);
        card.setWidthFull();
        card.getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "8px 14px")
                .set("margin-bottom", "6px")
                .set("background", "var(--rom-bg-card)");
        return card;
    }
}

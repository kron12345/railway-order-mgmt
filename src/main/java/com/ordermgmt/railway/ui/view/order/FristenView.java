package com.ordermgmt.railway.ui.view.order;

import java.util.List;

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

import com.ordermgmt.railway.domain.order.model.FristRegel;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.service.FristService;
import com.ordermgmt.railway.domain.order.service.FristService.AutoBusiness;
import com.ordermgmt.railway.domain.order.service.FristService.FristEntry;
import com.ordermgmt.railway.domain.order.service.FristService.Status;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Deadline overview: rule-derived deadlines per position, grouped by urgency. */
@Route(value = "fristen", layout = MainLayout.class)
@PageTitle("Fristen")
@PermitAll
public class FristenView extends VerticalLayout {

    public FristenView(FristService fristService) {
        setWidthFull();
        setPadding(true);

        H2 title = new H2(getTranslation("fristen.title"));
        title.getStyle().set("margin", "0 0 var(--lumo-space-m) 0");
        add(title);

        FristService.Overview overview = fristService.overview();
        add(buildAutoBusinessSection(overview.businesses()));

        List<FristEntry> entries = overview.entries();
        if (entries.isEmpty()) {
            add(emptyHint(getTranslation("fristen.empty")));
            return;
        }
        for (Status status : Status.values()) {
            List<FristEntry> group = entries.stream().filter(e -> e.status() == status).toList();
            if (group.isEmpty()) {
                continue;
            }
            add(
                    sectionHeader(
                            getTranslation("fristen.status." + status.name())
                                    + " ("
                                    + group.size()
                                    + ")"));
            group.forEach(e -> add(entryRow(e)));
        }
    }

    private Component buildAutoBusinessSection(List<AutoBusiness> autos) {
        Div section = new Div();
        section.setWidthFull();
        section.add(sectionHeader(getTranslation("fristen.auto.title")));
        if (autos.isEmpty()) {
            section.add(emptyHint(getTranslation("fristen.empty")));
            return section;
        }
        for (AutoBusiness ab : autos) {
            FristRegel r = ab.regel();
            String summary =
                    "⚙ "
                            + r.getName()
                            + "  ·  "
                            + getTranslation("fristen.auto.members", ab.total())
                            + (ab.overdue() > 0
                                    ? "  ·  " + getTranslation("fristen.auto.overdue", ab.overdue())
                                    : "")
                            + (ab.dueSoon() > 0
                                    ? "  ·  " + getTranslation("fristen.auto.dueSoon", ab.dueSoon())
                                    : "");
            Span label = new Span(summary);
            label.getStyle().set("font-size", "13px");
            StatusBadge action =
                    new StatusBadge(
                            getTranslation("fristen.action." + r.getAction().name()),
                            r.getAction() == FristRegel.Action.AUTO_BESTELLEN
                                    ? StatusBadge.StatusType.INFO
                                    : StatusBadge.StatusType.NEUTRAL);
            section.add(row(label, action));
        }
        return section;
    }

    private Component entryRow(FristEntry e) {
        OrderPosition pos = e.position();
        String orderNo = pos.getOrder() != null ? pos.getOrder().getOrderNumber() : "—";
        Span label = new Span(orderNo + " · " + pos.getName() + "  ·  " + e.regel().getName());
        label.getStyle().set("font-size", "13px");

        StatusBadge badge =
                new StatusBadge(
                        getTranslation("fristen.dueOn", e.deadline()), badgeType(e.status()));

        Button toOrder =
                new Button(getTranslation("offen.toOrder"), VaadinIcon.ARROW_RIGHT.create());
        toOrder.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        if (pos.getOrder() != null) {
            toOrder.addClickListener(
                    ev -> UI.getCurrent().navigate("orders/" + pos.getOrder().getId()));
        }

        HorizontalLayout right = new HorizontalLayout(badge, toOrder);
        right.setAlignItems(FlexComponent.Alignment.CENTER);
        right.setSpacing(true);
        return row(label, right);
    }

    private static StatusBadge.StatusType badgeType(Status status) {
        return switch (status) {
            case UEBERFAELLIG -> StatusBadge.StatusType.DANGER;
            case FAELLIG_BALD -> StatusBadge.StatusType.WARNING;
            case OK -> StatusBadge.StatusType.NEUTRAL;
        };
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

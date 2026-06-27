package com.ordermgmt.railway.ui.view.order;

import java.util.List;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
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
import com.ordermgmt.railway.domain.order.service.AutoBusinessService;
import com.ordermgmt.railway.domain.order.service.FristAutoOrderService;
import com.ordermgmt.railway.domain.order.service.FristService;
import com.ordermgmt.railway.domain.order.service.FristService.AutoBusiness;
import com.ordermgmt.railway.domain.order.service.FristService.DayDeadline;
import com.ordermgmt.railway.domain.order.service.FristService.FristEntry;
import com.ordermgmt.railway.domain.order.service.FristService.Status;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.ui.component.StatusBadge;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Deadline overview: rule-derived deadlines per position, grouped by urgency. */
@Route(value = "fristen", layout = MainLayout.class)
@PageTitle("Fristen")
@PermitAll
public class FristenView extends VerticalLayout {

    private static final int PAGE_SIZE = 25;

    private final FristService fristService;
    private final FristAutoOrderService autoOrderService;
    private final AutoBusinessService autoBusinessService;
    private Integer lastFired;

    public FristenView(
            FristService fristService,
            FristAutoOrderService autoOrderService,
            AutoBusinessService autoBusinessService) {
        this.fristService = fristService;
        this.autoOrderService = autoOrderService;
        this.autoBusinessService = autoBusinessService;
        setWidthFull();
        setPadding(true);
        build();
    }

    /**
     * Builds (or rebuilds in place) the whole view; called again after a manual rule evaluation.
     */
    private void build() {
        removeAll();

        add(createHeader());
        if (lastFired != null) {
            add(createEvaluationResult());
        }

        FristService.Overview overview = fristService.overview();
        add(buildAutoBusinessSection(overview.businesses()));

        List<FristEntry> entries = overview.entries();
        if (entries.isEmpty()) {
            add(emptyHint(getTranslation("fristen.empty")));
            return;
        }
        for (Status status : Status.values()) {
            List<FristEntry> group = entries.stream().filter(e -> e.status() == status).toList();
            if (!group.isEmpty()) {
                renderGroup(status, group);
            }
        }
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2(getTranslation("fristen.title"));
        title.getStyle().set("margin", "0");

        Button evaluate =
                new Button(getTranslation("fristen.evaluate.button"), VaadinIcon.PLAY.create());
        evaluate.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        // Triggering auto-orders is a mutating action → mutators only (the @Scheduled path is
        // system).
        evaluate.setVisible(CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER"));
        evaluate.addClickListener(
                e -> {
                    autoBusinessService.syncAll(); // refresh the automatic businesses first
                    lastFired = autoOrderService.runOnce();
                    build(); // refresh counts in place with an inline result banner
                });

        HorizontalLayout header = new HorizontalLayout(title, evaluate);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        return header;
    }

    private Span createEvaluationResult() {
        Span result = new Span(getTranslation("fristen.evaluate.result", lastFired));
        result.getStyle()
                .set("display", "inline-block")
                .set("background", "color-mix(in srgb, var(--rom-status-active) 12%, transparent)")
                .set("color", "var(--rom-status-active)")
                .set("border", "1px solid var(--rom-status-active)")
                .set("border-radius", "4px")
                .set("padding", "4px 10px")
                .set("font-size", "13px")
                .set("margin-bottom", "var(--lumo-space-s)");
        return result;
    }

    /**
     * Renders one urgency group: a counted header, its first {@value #PAGE_SIZE} rows, and a
     * "weitere laden" button that reveals the rest in pages — no entry is hidden (the old static
     * "…+N" cap dropped them). The service computes the full grouped result; this is the UI reveal.
     */
    private void renderGroup(Status status, List<FristEntry> group) {
        add(
                sectionHeader(
                        getTranslation("fristen.status." + status.name())
                                + " ("
                                + group.size()
                                + ")"));
        Div rowsBox = new Div();
        rowsBox.setWidthFull();
        add(rowsBox);
        if (group.size() <= PAGE_SIZE) {
            group.forEach(e -> rowsBox.add(entryRow(e)));
            return;
        }
        Button more = new Button();
        more.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        more.getStyle().set("margin", "4px 0 8px 0");
        int[] shown = {0};
        Runnable reveal =
                () -> {
                    int end = Math.min(group.size(), shown[0] + PAGE_SIZE);
                    for (int i = shown[0]; i < end; i++) {
                        rowsBox.add(entryRow(group.get(i)));
                    }
                    shown[0] = end;
                    int remaining = group.size() - shown[0];
                    more.setText(getTranslation("fristen.loadMore", remaining));
                    more.setVisible(remaining > 0);
                };
        more.addClickListener(e -> reveal.run());
        add(more);
        reveal.run();
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
        Div card = row(label, right);

        // Rolling FAHRT rules carry a per-Verkehrstag breakdown (3b): show it as an expandable
        // detail.
        if (e.perDay().isEmpty()) {
            return card;
        }
        Details perDay =
                new Details(
                        getTranslation("fristen.perDay.summary", e.perDay().size()),
                        buildPerDayList(e));
        perDay.getStyle().set("margin", "-2px 0 8px 0").set("font-size", "12px");
        Div wrapper = new Div(card, perDay);
        wrapper.setWidthFull();
        return wrapper;
    }

    private Component buildPerDayList(FristEntry e) {
        Div box = new Div();
        box.getStyle().set("padding", "2px 14px 6px 14px");
        for (DayDeadline day : e.perDay()) {
            Span line =
                    new Span(
                            getTranslation(
                                    "fristen.perDay.row", day.operatingDay(), day.deadline()));
            line.getStyle()
                    .set("display", "block")
                    .set("font-size", "12px")
                    .set("color", statusColor(day.status()));
            box.add(line);
        }
        return box;
    }

    private static String statusColor(Status status) {
        return switch (status) {
            case UEBERFAELLIG -> "var(--rom-status-danger, #c0392b)";
            case FAELLIG_BALD -> "var(--rom-status-warning, #b9770e)";
            case OK -> "var(--rom-text-muted)";
        };
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

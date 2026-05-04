package com.ordermgmt.railway.ui.view.order;

import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Function;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.ui.component.business.LinkedBusinessesPanel;

/**
 * Read-only detail panel for a single {@link OrderPosition}, embedded by
 * {@link OrderOverviewView} when the URL is {@code /orders/{orderId}/positions/{posId}}.
 *
 * <p>Top: pinned action bar with the primary "← Zum Auftrag" button (Alt+U) so users
 * always have a one-key route back to the order aggregate. Below: read-only fields.
 */
public class OrderPositionDetailView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    public OrderPositionDetailView(OrderPositionRepository positionRepo,
                                   BusinessService businessService,
                                   UUID orderId, UUID positionId,
                                   Function<String, String> tr) {
        addClassName("order-pos-detail");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--rom-bg-primary)");

        OrderPosition pos = positionRepo.findById(positionId).orElse(null);
        // Reject cross-order deep links: the position must actually belong to the URL's order.
        if (pos == null || pos.getOrder() == null
                || !orderId.equals(pos.getOrder().getId())) {
            Notification.show(tr.apply("order.position.notFound"), 2500,
                    Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("orders/" + orderId);
            return;
        }

        add(buildActionBar(orderId, tr));
        add(buildBody(pos, tr));
        add(new LinkedBusinessesPanel(
                businessService.findByLinkedOrderPosition(positionId), tr));
    }

    private HorizontalLayout buildActionBar(UUID orderId, Function<String, String> tr) {
        var bar = new HorizontalLayout();
        bar.addClassName("order-pos-detail__actions");
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getElement().setAttribute("role", "toolbar");

        var back = new Button(tr.apply("order.position.toOrder"), VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        back.getElement().setAttribute("aria-keyshortcuts", "Alt+U");
        back.addClickListener(e -> UI.getCurrent().navigate("orders/" + orderId));
        // Alt+U → back to order
        com.vaadin.flow.component.Shortcuts.addShortcutListener(this,
                () -> UI.getCurrent().navigate("orders/" + orderId),
                com.vaadin.flow.component.Key.KEY_U,
                com.vaadin.flow.component.KeyModifier.ALT)
                .listenOn(this);

        bar.add(back);
        return bar;
    }

    private Div buildBody(OrderPosition pos, Function<String, String> tr) {
        Div card = new Div();
        card.addClassName("biz-card");
        card.addClassName("biz-card--flex");

        Span title = new Span(safe(pos.getName()).isEmpty() ? "—" : pos.getName());
        title.addClassName("order-pos-detail__title");
        card.add(title);

        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("520px", 2));
        form.addClassName("biz-form");

        form.addFormItem(label(pos.getType() == null ? "—" : pos.getType().name()),
                tr.apply("position.type"));
        form.addFormItem(label(safe(pos.getServiceType())),
                tr.apply("position.serviceType"));
        form.addFormItem(label(safe(pos.getOperationalTrainNumber())),
                tr.apply("position.operationalTrainNumber"));
        form.addFormItem(label(safe(pos.getFromLocation())),
                tr.apply("position.from"));
        form.addFormItem(label(safe(pos.getToLocation())),
                tr.apply("position.to"));
        form.addFormItem(label(pos.getStart() == null ? "—" : pos.getStart().format(TIME_FMT)),
                tr.apply("position.start"));
        form.addFormItem(label(pos.getEnd() == null ? "—" : pos.getEnd().format(TIME_FMT)),
                tr.apply("position.end"));
        form.addFormItem(label(safe(pos.getTags())),
                tr.apply("position.tags"));
        form.addFormItem(label(safe(pos.getComment())),
                tr.apply("position.comment"));

        card.add(form);
        return card;
    }

    private Span label(String text) {
        Span s = new Span(text == null || text.isBlank() ? "—" : text);
        s.addClassName("order-pos-detail__value");
        return s;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

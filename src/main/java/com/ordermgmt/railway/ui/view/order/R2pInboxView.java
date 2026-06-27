package com.ordermgmt.railway.ui.view.order;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.R2pInboxEntry;
import com.ordermgmt.railway.domain.order.model.R2pResourceRequest;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;
import com.ordermgmt.railway.domain.order.service.R2pIntakeService;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.ui.layout.MainLayout;

/** Inbox for inbound R2P orders: simulate an incoming order and accept it into an order. */
@Route(value = "r2p-inbox", layout = MainLayout.class)
@PageTitle("R2P-Eingang")
@PermitAll
public class R2pInboxView extends VerticalLayout {

    private final R2pIntakeService r2pIntakeService;
    private final OrderRepository orderRepository;
    private final boolean editable = CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER");

    private final VerticalLayout list = new VerticalLayout();

    public R2pInboxView(R2pIntakeService r2pIntakeService, OrderRepository orderRepository) {
        this.r2pIntakeService = r2pIntakeService;
        this.orderRepository = orderRepository;

        setWidthFull();
        setPadding(true);

        H2 title = new H2(getTranslation("r2p.title"));
        title.getStyle().set("margin", "0");

        Button simulate = new Button(getTranslation("r2p.simulate"), VaadinIcon.INBOX.create());
        simulate.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        simulate.setVisible(editable);
        simulate.addClickListener(
                e -> {
                    r2pIntakeService.simulateIncoming();
                    refresh();
                });

        HorizontalLayout header = new HorizontalLayout(title, simulate);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        list.setPadding(false);
        list.setSpacing(true);
        list.setWidthFull();

        add(header, list);
        refresh();
    }

    private void refresh() {
        list.removeAll();
        var pending = r2pIntakeService.findPending();
        if (pending.isEmpty()) {
            Span empty = new Span(getTranslation("r2p.empty"));
            empty.getStyle().set("color", "var(--rom-text-muted)");
            list.add(empty);
            return;
        }
        for (R2pInboxEntry entry : pending) {
            list.add(buildEntryCard(entry));
        }
    }

    private Div buildEntryCard(R2pInboxEntry entry) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "10px 14px")
                .set("background", "var(--rom-bg-card)");

        Span requester = new Span(entry.getRequester());
        requester.getStyle().set("font-weight", "600");

        Span metaSpan = new Span(entryMetaText(entry));
        metaSpan.getStyle().set("color", "var(--rom-text-secondary)").set("font-size", "12px");

        Span resSpan = new Span(resourceSummaryText(entry));
        resSpan.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "12px");

        Div info = new Div(requester, metaSpan, resSpan);
        info.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "3px");

        Button accept = new Button(getTranslation("r2p.accept"), VaadinIcon.CHECK.create());
        accept.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        accept.setVisible(editable);
        accept.addClickListener(e -> openAcceptDialog(entry));

        HorizontalLayout row = new HorizontalLayout(info, accept);
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        card.add(row);
        return card;
    }

    private String entryMetaText(R2pInboxEntry entry) {
        StringBuilder meta = new StringBuilder();
        if (entry.getOperationalTrainNumber() != null) {
            meta.append("OTN ").append(entry.getOperationalTrainNumber());
        }
        if (entry.getFromLocation() != null && entry.getToLocation() != null) {
            meta.append("  ·  ")
                    .append(entry.getFromLocation())
                    .append(" → ")
                    .append(entry.getToLocation());
        }
        return meta.toString();
    }

    private String resourceSummaryText(R2pInboxEntry entry) {
        StringBuilder summary = new StringBuilder();
        for (R2pResourceRequest request : r2pIntakeService.resourcesOf(entry)) {
            if (!summary.isEmpty()) {
                summary.append("  ·  ");
            }
            summary.append(request.quantity())
                    .append("× ")
                    .append(getTranslation("resource.type." + request.resourceType().name()))
                    .append(" — ")
                    .append(request.description());
        }
        return summary.toString();
    }

    private void openAcceptDialog(R2pInboxEntry entry) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("r2p.accept.title"));
        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setWidth("420px");

        boolean hasMatch = r2pIntakeService.findMatchingPosition(entry).isPresent();
        Select<Order> orderCombo = new Select<>();
        orderCombo.setLabel(getTranslation("r2p.accept.targetOrder"));

        Button confirm = new Button(getTranslation("r2p.accept"));
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        if (hasMatch) {
            Span note =
                    new Span(
                            getTranslation(
                                    "r2p.accept.matched", entry.getOperationalTrainNumber()));
            body.add(note);
            confirm.addClickListener(e -> doAccept(entry, null, dialog));
        } else {
            orderCombo.setItems(orderRepository.findAll());
            orderCombo.setItemLabelGenerator(o -> o.getOrderNumber() + " · " + o.getName());
            orderCombo.setWidthFull();
            body.add(new Span(getTranslation("r2p.accept.noMatch")), orderCombo);
            confirm.setEnabled(false);
            orderCombo.addValueChangeListener(e -> confirm.setEnabled(e.getValue() != null));
            confirm.addClickListener(
                    e -> {
                        if (orderCombo.getValue() != null) {
                            doAccept(entry, orderCombo.getValue().getId(), dialog);
                        }
                    });
        }

        dialog.add(body);
        dialog.getFooter().add(new Button(getTranslation("common.cancel"), e -> dialog.close()));
        dialog.getFooter().add(confirm);
        dialog.open();
    }

    private void doAccept(R2pInboxEntry entry, java.util.UUID fallbackOrderId, Dialog dialog) {
        try {
            r2pIntakeService.accept(entry.getId(), fallbackOrderId);
            Notification.show(
                            getTranslation("r2p.accepted"), 2500, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            dialog.close();
            refresh();
        } catch (RuntimeException ex) {
            Notification.show(
                            getTranslation("common.errorGeneric"),
                            3000,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

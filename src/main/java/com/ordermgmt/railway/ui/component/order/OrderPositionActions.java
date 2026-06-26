package com.ordermgmt.railway.ui.component.order;

import java.util.function.BiFunction;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.domain.order.service.PurchaseOrderService;
import com.ordermgmt.railway.domain.timetable.service.TimetableArchiveService;

/**
 * Position-level actions for the position panel: create/edit a service or timetable position,
 * delete with confirmation, add an expression (Ausprägung), and respond to an infrastructure
 * alteration. Keeps the dialog/handler wiring out of {@link OrderPositionPanel}.
 */
class OrderPositionActions {

    private final Order order;
    private final OrderService orderService;
    private final TimetableArchiveService timetableArchiveService;
    private final OperationalPointRepository opRepo;
    private final PredefinedTagRepository tagRepo;
    private final BusinessService businessService;
    private final PurchaseOrderService purchaseOrderService;
    private final BiFunction<String, Object[], String> t;
    private final Runnable onRefresh;

    OrderPositionActions(
            Order order,
            OrderService orderService,
            TimetableArchiveService timetableArchiveService,
            OperationalPointRepository opRepo,
            PredefinedTagRepository tagRepo,
            BusinessService businessService,
            PurchaseOrderService purchaseOrderService,
            BiFunction<String, Object[], String> t,
            Runnable onRefresh) {
        this.order = order;
        this.orderService = orderService;
        this.timetableArchiveService = timetableArchiveService;
        this.opRepo = opRepo;
        this.tagRepo = tagRepo;
        this.businessService = businessService;
        this.purchaseOrderService = purchaseOrderService;
        this.t = t;
        this.onRefresh = onRefresh;
    }

    private String tr(String key) {
        return t.apply(key, new Object[0]);
    }

    void editPosition(OrderPosition pos) {
        if (pos.getType() == PositionType.LEISTUNG) {
            openServiceDialog(pos);
        } else {
            openTimetableBuilder(pos);
        }
    }

    void openServiceDialog(OrderPosition existing) {
        ServicePositionDialog dialog =
                new ServicePositionDialog(
                        order, existing, orderService, opRepo, tagRepo, businessService, t);
        dialog.addSaveListener(e -> onRefresh.run());
        dialog.open();
    }

    void confirmDelete(OrderPosition pos) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(tr("common.delete") + ": " + pos.getName() + "?");
        dialog.setCancelable(true);
        dialog.setCancelText(tr("common.cancel"));
        dialog.setConfirmText(tr("common.delete"));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(
                e -> {
                    orderService.deletePosition(pos.getId());
                    onRefresh.run();
                });
        dialog.open();
    }

    void openTimetableBuilder(OrderPosition existing) {
        String target = "orders/" + order.getId() + "/timetable-builder";
        if (existing != null) {
            target += "?positionId=" + existing.getId();
        }
        UI.getCurrent().navigate(target);
    }

    /** Opens the Verkehrstage editor for an expression: pick days, reassign from siblings. */
    void openVerkehrstageDialog(OrderPosition expr) {
        OrderService.VerkehrstageContext ctx = orderService.verkehrstageContext(expr.getId());
        new ExpressionVerkehrstageDialog(
                        tr("verkehrstage.dialog.title") + " · " + expr.getName(),
                        ctx.min(),
                        ctx.max(),
                        ctx.current(),
                        ctx.occupied(),
                        t,
                        days -> {
                            orderService.setExpressionVerkehrstage(expr.getId(), days);
                            Notification.show(
                                            tr("verkehrstage.saved"),
                                            2500,
                                            Notification.Position.BOTTOM_END)
                                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            onRefresh.run();
                        })
                .open();
    }

    /**
     * Creates a new expression as a clone of the parent train identity and opens the
     * type-appropriate editor pre-filled: the Fahrplan-Builder for FAHRPLAN (with the parent's
     * timetable archive cloned), the service dialog for LEISTUNG. Replaces the former generic
     * expression dialog.
     */
    void openAddExpression(OrderPosition parent) {
        OrderPosition child;
        try {
            child = orderService.addExpressionFromParent(parent.getId());
        } catch (OrderService.PositionHasBookingsException ex) {
            Notification.show(tr("expression.hasBookings"), 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (parent.getType() == PositionType.FAHRPLAN) {
            timetableArchiveService.cloneArchiveTo(parent.getId(), child.getId());
            openTimetableBuilder(child); // edit-mode on the freshly cloned child
        } else {
            openServiceDialog(child); // edit-mode on the freshly cloned child
        }
    }

    /**
     * Whether a position can be split into expressions. Mirrors {@link
     * OrderService#addExpressionFromParent}: a flat position that already has bookings cannot be
     * promoted, so the UI disables the action up front. A train identity (typed) is always
     * splittable.
     */
    static boolean canSplit(OrderPosition pos) {
        if (pos.getVariantType() != null) {
            return true; // a train identity is already split-ready
        }
        // Only real purchase orders block a split (a CAPACITY resource need does not).
        return pos.getPurchasePositions() == null || pos.getPurchasePositions().isEmpty();
    }

    void respondToAlteration(OrderPosition pos, boolean accept) {
        try {
            purchaseOrderService.respondToAlteration(pos.getId(), accept);
            Notification.show(
                            tr(accept ? "order.alteration.accepted" : "order.alteration.rejected"),
                            2500,
                            Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (RuntimeException ex) {
            Notification.show(tr("common.errorGeneric"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            onRefresh.run();
        }
    }
}

package com.ordermgmt.railway.ui.component.business;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.value.ValueChangeMode;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.userprefs.service.UserViewPreferenceService;
import com.ordermgmt.railway.ui.component.grid.GridPreferenceBinder;

/**
 * Unified tree view for everything linked to a business: orders → order positions → purchase
 * positions. Provides a live filter, two link buttons (one per kind), and delegates persistence +
 * actions to suppliers/callbacks.
 */
public class BusinessLinksTree extends Div {

    private static final UUID UNKNOWN_ORDER_ID = new UUID(0, 0);
    private static final String EMPTY_VALUE = "—";
    private static final int LINK_NOTIFICATION_DURATION_MS = 1200;

    private final Spec spec;
    private final TreeGrid<BusinessLinkNode> tree = new TreeGrid<>();
    private final TextField filter = new TextField();
    private List<BusinessLinkNode> roots = List.of();
    private String filterText = "";

    public BusinessLinksTree(Spec spec) {
        this.spec = spec;
        addClassName("biz-links-tree");
        configureLayout();

        add(buildToolbar());
        add(tree);

        configureTree();
        configureColumns();
        installPreferenceBinder();

        refresh();
    }

    private void configureLayout() {
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("min-height", "0")
                .set("height", "100%");
    }

    private void configureTree() {
        tree.addClassName("biz-links-tree__grid");
        tree.addThemeVariants(
                GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        tree.setSizeFull();
    }

    private void installPreferenceBinder() {
        if (spec.viewKey != null && spec.preferenceService != null) {
            var binder = new GridPreferenceBinder<>(tree, spec.viewKey, spec.preferenceService);
            binder.install();
        }
    }

    public void refresh() {
        roots =
                buildTree(
                        spec.allOrderPositions.get(),
                        spec.allPurchasePositions.get(),
                        spec.linkedOrderPositions.get(),
                        spec.linkedPurchasePositions.get());
        applyFilter();
    }

    // ─── toolbar ─────────

    private Component buildToolbar() {
        var bar = new HorizontalLayout();
        bar.addClassName("biz-links-tree__toolbar");
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(false);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);

        filter.setPlaceholder(spec.translator.apply("business.filterPlaceholder"));
        filter.setPrefixComponent(VaadinIcon.SEARCH.create());
        filter.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        filter.setClearButtonVisible(true);
        filter.setValueChangeMode(ValueChangeMode.LAZY);
        filter.setValueChangeTimeout(220);
        filter.addClassName("biz-links-tree__filter");
        filter.addValueChangeListener(
                e -> {
                    filterText =
                            e.getValue() == null
                                    ? ""
                                    : e.getValue().trim().toLowerCase(Locale.ROOT);
                    applyFilter();
                });

        bar.add(filter);
        bar.expand(filter);
        return bar;
    }

    // ─── columns ─────────

    private void configureColumns() {
        tree.addComponentColumn(this::buildLinkCheckbox)
                .setKey("__linked")
                .setHeader("")
                .setWidth("44px")
                .setFlexGrow(0)
                .setFrozen(true);
        tree.addComponentHierarchyColumn(this::buildNameCell)
                .setKey("name")
                .setHeader(spec.translator.apply("business.tree.name"))
                .setFlexGrow(2);
        tree.addColumn(BusinessLinkNode::number)
                .setKey("number")
                .setHeader(spec.translator.apply("business.tree.number"))
                .setWidth("160px")
                .setFlexGrow(0);
    }

    private Component buildLinkCheckbox(BusinessLinkNode node) {
        if (node.kind() == BusinessLinkNode.Kind.ORDER) {
            return new Span();
        }
        var checkbox = new Checkbox(node.linked());
        checkbox.addClassName("biz-tree-checkbox");
        checkbox.getElement().setAttribute("aria-label", spec.translator.apply("business.linked"));
        checkbox.addValueChangeListener(e -> toggleLink(node, Boolean.TRUE.equals(e.getValue())));
        return checkbox;
    }

    private void toggleLink(BusinessLinkNode node, boolean wantLinked) {
        UUID id = node.entityId();
        if (id == null || wantLinked == node.linked()) {
            return;
        }
        switch (node.kind()) {
            case ORDER_POSITION -> {
                if (wantLinked) spec.onLinkOrderPosition.accept(id);
                else spec.onUnlinkOrderPosition.accept(id);
            }
            case PURCHASE_POSITION -> {
                if (wantLinked) spec.onLinkPurchasePosition.accept(id);
                else spec.onUnlinkPurchasePosition.accept(id);
            }
            default -> {
                return;
            }
        }
        showLinkNotification(wantLinked);
        refresh();
    }

    private void showLinkNotification(boolean linked) {
        Notification.show(
                        spec.translator.apply(linked ? "business.linked" : "business.unlinked"),
                        LINK_NOTIFICATION_DURATION_MS,
                        Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private Component buildNameCell(BusinessLinkNode node) {
        var wrapper = new HorizontalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.addClassName("biz-tree-cell");
        wrapper.addClassName("biz-tree-cell--" + cssName(node.kind().name()));

        var tag = new Span(node.kind().tag());
        tag.addClassName("biz-tree-tag");
        tag.addClassName("biz-tree-tag--" + cssName(node.kind().name()));

        var name = new Span(node.name());
        name.addClassName("biz-tree-name");
        if (node.kind() == BusinessLinkNode.Kind.ORDER_POSITION && !node.linked()) {
            name.addClassName("biz-tree-name--ghost");
        }

        wrapper.add(tag, name);
        return wrapper;
    }

    // ─── tree assembly + filtering ─────────

    private List<BusinessLinkNode> buildTree(
            List<OrderPosition> allOrderPositions,
            List<PurchasePosition> allPurchasePositions,
            List<OrderPosition> linkedOrderPositions,
            List<PurchasePosition> linkedPurchasePositions) {
        Set<UUID> linkedOpIds = new HashSet<>();
        for (OrderPosition orderPosition : linkedOrderPositions) {
            linkedOpIds.add(orderPosition.getId());
        }
        Set<UUID> linkedPpIds = new HashSet<>();
        for (PurchasePosition purchasePosition : linkedPurchasePositions) {
            linkedPpIds.add(purchasePosition.getId());
        }

        Map<UUID, BusinessLinkNode> ordersById = new LinkedHashMap<>();
        Map<UUID, BusinessLinkNode> orderPositionsById = new LinkedHashMap<>();

        for (OrderPosition orderPosition : allOrderPositions) {
            BusinessLinkNode orderNode = ensureOrderNode(ordersById, orderPosition.getOrder());
            BusinessLinkNode orderPositionNode =
                    BusinessLinkNode.orderPosition(
                            orderPosition.getId(),
                            displayName(orderPosition.getName()),
                            linkedOpIds.contains(orderPosition.getId()));
            orderPositionsById.put(orderPosition.getId(), orderPositionNode);
            orderNode.children().add(orderPositionNode);
        }

        for (PurchasePosition purchasePosition : allPurchasePositions) {
            OrderPosition orderPosition = purchasePosition.getOrderPosition();
            if (orderPosition == null) {
                continue;
            }
            BusinessLinkNode orderPositionNode = orderPositionsById.get(orderPosition.getId());
            if (orderPositionNode == null) {
                // Edge case: PP exists but its OP wasn't returned by allOrderPositions.
                BusinessLinkNode orderNode = ensureOrderNode(ordersById, orderPosition.getOrder());
                orderPositionNode =
                        BusinessLinkNode.orderPosition(
                                orderPosition.getId(),
                                displayName(orderPosition.getName()),
                                linkedOpIds.contains(orderPosition.getId()));
                orderPositionsById.put(orderPosition.getId(), orderPositionNode);
                orderNode.children().add(orderPositionNode);
            }
            orderPositionNode.children()
                    .add(
                            BusinessLinkNode.purchasePosition(
                                    purchasePosition.getId(),
                                    displayName(orderPosition.getName()),
                                    displayName(purchasePosition.getPositionNumber()),
                                    linkedPpIds.contains(purchasePosition.getId())));
        }

        return new ArrayList<>(ordersById.values());
    }

    private BusinessLinkNode ensureOrderNode(Map<UUID, BusinessLinkNode> ordersById, Order order) {
        UUID key = order != null && order.getId() != null ? order.getId() : UNKNOWN_ORDER_ID;
        return ordersById.computeIfAbsent(
                key,
                orderId ->
                        BusinessLinkNode.order(
                                orderId,
                                order != null && order.getName() != null
                                        ? order.getName()
                                        : spec.translator.apply("business.tree.unknownOrder"),
                                order != null && order.getOrderNumber() != null
                                        ? order.getOrderNumber()
                                        : EMPTY_VALUE));
    }

    private static String displayName(String value) {
        return value != null ? value : EMPTY_VALUE;
    }

    private static String cssName(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private void applyFilter() {
        List<BusinessLinkNode> visible =
                filterText.isBlank() ? roots : filterTree(roots, filterText);
        tree.setItems(visible, BusinessLinkNode::children);
        if (!filterText.isBlank()) {
            expandAll(visible);
        } else {
            expandLinked(visible);
        }
    }

    /** Expand any branch that contains at least one linked descendant. */
    private boolean expandLinked(List<BusinessLinkNode> nodes) {
        boolean anyLinked = false;
        for (BusinessLinkNode node : nodes) {
            boolean childLinked = expandLinked(node.children());
            boolean nodeLinked = node.linked();
            if (childLinked || (node.kind() != BusinessLinkNode.Kind.ORDER && nodeLinked)) {
                tree.expand(node);
                anyLinked = true;
            } else if (nodeLinked) {
                anyLinked = true;
            }
        }
        return anyLinked;
    }

    private List<BusinessLinkNode> filterTree(List<BusinessLinkNode> nodes, String lowerCaseQuery) {
        List<BusinessLinkNode> kept = new ArrayList<>();
        for (BusinessLinkNode node : nodes) {
            boolean nodeMatches = node.matches(lowerCaseQuery);
            List<BusinessLinkNode> filteredChildren =
                    filterTree(node.children(), lowerCaseQuery);
            if (nodeMatches || !filteredChildren.isEmpty()) {
                BusinessLinkNode copy = cloneShallow(node);
                copy.children().addAll(nodeMatches ? node.children() : filteredChildren);
                kept.add(copy);
            }
        }
        return kept;
    }

    private BusinessLinkNode cloneShallow(BusinessLinkNode node) {
        return switch (node.kind()) {
            case ORDER -> BusinessLinkNode.order(node.entityId(), node.name(), node.number());
            case ORDER_POSITION ->
                    BusinessLinkNode.orderPosition(node.entityId(), node.name(), node.linked());
            case PURCHASE_POSITION ->
                    BusinessLinkNode.purchasePosition(
                            node.entityId(), node.name(), node.number(), node.linked());
        };
    }

    private void expandAll(List<BusinessLinkNode> nodes) {
        for (BusinessLinkNode node : nodes) {
            tree.expand(node);
            expandAll(node.children());
        }
    }

    // ─── builder ─────────

    public static Spec spec() {
        return new Spec();
    }

    public static class Spec {
        Function<String, String> translator = translationKey -> translationKey;
        Supplier<List<OrderPosition>> linkedOrderPositions = List::of;
        Supplier<List<PurchasePosition>> linkedPurchasePositions = List::of;
        Supplier<List<OrderPosition>> allOrderPositions = List::of;
        Supplier<List<PurchasePosition>> allPurchasePositions = List::of;
        Consumer<UUID> onLinkOrderPosition = id -> {};
        Consumer<UUID> onUnlinkOrderPosition = id -> {};
        Consumer<UUID> onLinkPurchasePosition = id -> {};
        Consumer<UUID> onUnlinkPurchasePosition = id -> {};
        String viewKey;
        UserViewPreferenceService preferenceService;

        public Spec translator(Function<String, String> translator) {
            this.translator = translator;
            return this;
        }

        public Spec linkedOrderPositions(Supplier<List<OrderPosition>> linkedOrderPositions) {
            this.linkedOrderPositions = linkedOrderPositions;
            return this;
        }

        public Spec linkedPurchasePositions(
                Supplier<List<PurchasePosition>> linkedPurchasePositions) {
            this.linkedPurchasePositions = linkedPurchasePositions;
            return this;
        }

        public Spec allOrderPositions(Supplier<List<OrderPosition>> allOrderPositions) {
            this.allOrderPositions = allOrderPositions;
            return this;
        }

        public Spec allPurchasePositions(Supplier<List<PurchasePosition>> allPurchasePositions) {
            this.allPurchasePositions = allPurchasePositions;
            return this;
        }

        public Spec onLinkOrderPosition(Consumer<UUID> onLinkOrderPosition) {
            this.onLinkOrderPosition = onLinkOrderPosition;
            return this;
        }

        public Spec onUnlinkOrderPosition(Consumer<UUID> onUnlinkOrderPosition) {
            this.onUnlinkOrderPosition = onUnlinkOrderPosition;
            return this;
        }

        public Spec onLinkPurchasePosition(Consumer<UUID> onLinkPurchasePosition) {
            this.onLinkPurchasePosition = onLinkPurchasePosition;
            return this;
        }

        public Spec onUnlinkPurchasePosition(Consumer<UUID> onUnlinkPurchasePosition) {
            this.onUnlinkPurchasePosition = onUnlinkPurchasePosition;
            return this;
        }

        public Spec viewKey(String viewKey) {
            this.viewKey = viewKey;
            return this;
        }

        public Spec preferenceService(UserViewPreferenceService preferenceService) {
            this.preferenceService = preferenceService;
            return this;
        }

        public BusinessLinksTree build() {
            return new BusinessLinksTree(this);
        }
    }
}

package com.ordermgmt.railway.ui.component.business;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Unified tree view for everything linked to a business: orders → order positions →
 * purchase positions. Provides a live filter, two link buttons (one per kind), and
 * delegates persistence + actions to suppliers/callbacks.
 */
public class BusinessLinksTree extends Div {

    private final Spec spec;
    private final TreeGrid<BusinessLinkNode> tree = new TreeGrid<>();
    private final TextField filter = new TextField();
    private List<BusinessLinkNode> roots = List.of();
    private String filterText = "";

    public BusinessLinksTree(Spec spec) {
        this.spec = spec;
        addClassName("biz-links-tree");
        getStyle().set("display", "flex").set("flex-direction", "column")
                .set("min-height", "0").set("height", "100%");

        add(buildToolbar());
        add(tree);

        tree.addClassName("biz-links-tree__grid");
        tree.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER,
                GridVariant.LUMO_ROW_STRIPES);
        tree.setSizeFull();
        configureColumns();

        // Persist column width / order / visibility for the unified tree.
        if (spec.viewKey != null && spec.preferenceService != null) {
            var binder = new GridPreferenceBinder<>(tree, spec.viewKey, spec.preferenceService);
            binder.install();
        }

        refresh();
    }

    public void refresh() {
        roots = buildTree(
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
        filter.addValueChangeListener(e -> {
            filterText = e.getValue() == null ? "" : e.getValue().trim().toLowerCase();
            applyFilter();
        });

        bar.add(filter);
        bar.expand(filter);
        return bar;
    }

    // ─── columns ─────────

    private void configureColumns() {
        tree.addComponentColumn(this::buildLinkCheckbox)
                .setKey("__linked").setHeader("").setWidth("44px").setFlexGrow(0)
                .setFrozen(true);
        tree.addComponentHierarchyColumn(this::buildNameCell)
                .setKey("name")
                .setHeader(spec.translator.apply("business.tree.name"))
                .setFlexGrow(2);
        tree.addColumn(BusinessLinkNode::number)
                .setKey("number")
                .setHeader(spec.translator.apply("business.tree.number"))
                .setWidth("160px").setFlexGrow(0);
    }

    private Component buildLinkCheckbox(BusinessLinkNode node) {
        if (node.kind() == BusinessLinkNode.Kind.ORDER) return new Span();
        var cb = new Checkbox(node.linked());
        cb.addClassName("biz-tree-checkbox");
        cb.getElement().setAttribute("aria-label",
                spec.translator.apply("business.linked"));
        cb.addValueChangeListener(e -> toggleLink(node, Boolean.TRUE.equals(e.getValue())));
        return cb;
    }

    private void toggleLink(BusinessLinkNode node, boolean wantLinked) {
        UUID id = node.entityId();
        if (id == null) return;
        if (wantLinked == node.linked()) return;
        switch (node.kind()) {
            case ORDER_POSITION -> {
                if (wantLinked) spec.onLinkOrderPosition.accept(id);
                else spec.onUnlinkOrderPosition.accept(id);
            }
            case PURCHASE_POSITION -> {
                if (wantLinked) spec.onLinkPurchasePosition.accept(id);
                else spec.onUnlinkPurchasePosition.accept(id);
            }
            default -> { return; }
        }
        Notification.show(
                spec.translator.apply(wantLinked ? "business.linked" : "business.unlinked"),
                1200, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refresh();
    }

    private Component buildNameCell(BusinessLinkNode node) {
        var wrapper = new HorizontalLayout();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.addClassName("biz-tree-cell");
        wrapper.addClassName("biz-tree-cell--" + node.kind().name().toLowerCase());

        var tag = new Span(node.kind().tag());
        tag.addClassName("biz-tree-tag");
        tag.addClassName("biz-tree-tag--" + node.kind().name().toLowerCase());

        var name = new Span(node.name());
        name.addClassName("biz-tree-name");
        if (node.kind() == BusinessLinkNode.Kind.ORDER_POSITION && !node.linked()) {
            name.addClassName("biz-tree-name--ghost");
        }

        wrapper.add(tag, name);
        return wrapper;
    }

    // ─── tree assembly + filtering ─────────

    private List<BusinessLinkNode> buildTree(List<OrderPosition> allOps,
                                             List<PurchasePosition> allPps,
                                             List<OrderPosition> linkedOps,
                                             List<PurchasePosition> linkedPps) {
        Set<UUID> linkedOpIds = new HashSet<>();
        for (OrderPosition op : linkedOps) linkedOpIds.add(op.getId());
        Set<UUID> linkedPpIds = new HashSet<>();
        for (PurchasePosition pp : linkedPps) linkedPpIds.add(pp.getId());

        Map<UUID, BusinessLinkNode> ordersByID = new LinkedHashMap<>();
        Map<UUID, BusinessLinkNode> opsByID = new LinkedHashMap<>();

        for (OrderPosition op : allOps) {
            BusinessLinkNode orderNode = ensureOrderNode(ordersByID, op.getOrder());
            BusinessLinkNode opNode = BusinessLinkNode.orderPosition(
                    op.getId(),
                    op.getName() != null ? op.getName() : "—",
                    linkedOpIds.contains(op.getId()));
            opsByID.put(op.getId(), opNode);
            orderNode.children().add(opNode);
        }

        for (PurchasePosition pp : allPps) {
            OrderPosition op = pp.getOrderPosition();
            if (op == null) continue;
            BusinessLinkNode opNode = opsByID.get(op.getId());
            if (opNode == null) {
                // Edge case: PP exists but its OP wasn't returned by allOps — synthesize.
                BusinessLinkNode orderNode = ensureOrderNode(ordersByID, op.getOrder());
                opNode = BusinessLinkNode.orderPosition(
                        op.getId(),
                        op.getName() != null ? op.getName() : "—",
                        linkedOpIds.contains(op.getId()));
                opsByID.put(op.getId(), opNode);
                orderNode.children().add(opNode);
            }
            opNode.children().add(BusinessLinkNode.purchasePosition(
                    pp.getId(),
                    op.getName() != null ? op.getName() : "—",
                    pp.getPositionNumber() != null ? pp.getPositionNumber() : "—",
                    linkedPpIds.contains(pp.getId())));
        }

        return new ArrayList<>(ordersByID.values());
    }

    private BusinessLinkNode ensureOrderNode(Map<UUID, BusinessLinkNode> map, Order order) {
        UUID key = order != null && order.getId() != null
                ? order.getId() : new UUID(0, 0);
        return map.computeIfAbsent(key, k -> BusinessLinkNode.order(
                k,
                order != null && order.getName() != null ? order.getName()
                        : spec.translator.apply("business.tree.unknownOrder"),
                order != null && order.getOrderNumber() != null ? order.getOrderNumber() : "—"));
    }

    private void applyFilter() {
        List<BusinessLinkNode> visible = filterText.isBlank()
                ? roots : filterTree(roots, filterText);
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
        for (BusinessLinkNode n : nodes) {
            boolean childLinked = expandLinked(n.children());
            boolean self = n.linked();
            if (childLinked || (n.kind() != BusinessLinkNode.Kind.ORDER && self)) {
                tree.expand(n);
                anyLinked = true;
            } else if (self) {
                anyLinked = true;
            }
        }
        return anyLinked;
    }

    private List<BusinessLinkNode> filterTree(List<BusinessLinkNode> nodes, String lc) {
        List<BusinessLinkNode> kept = new ArrayList<>();
        for (BusinessLinkNode n : nodes) {
            boolean self = n.matches(lc);
            List<BusinessLinkNode> filteredChildren = filterTree(n.children(), lc);
            if (self || !filteredChildren.isEmpty()) {
                BusinessLinkNode copy = cloneShallow(n);
                copy.children().addAll(self ? n.children() : filteredChildren);
                kept.add(copy);
            }
        }
        return kept;
    }

    private BusinessLinkNode cloneShallow(BusinessLinkNode n) {
        return switch (n.kind()) {
            case ORDER -> BusinessLinkNode.order(n.entityId(), n.name(), n.number());
            case ORDER_POSITION -> BusinessLinkNode.orderPosition(n.entityId(), n.name(), n.linked());
            case PURCHASE_POSITION -> BusinessLinkNode.purchasePosition(
                    n.entityId(), n.name(), n.number(), n.linked());
        };
    }

    private void expandAll(List<BusinessLinkNode> nodes) {
        for (BusinessLinkNode n : nodes) {
            tree.expand(n);
            expandAll(n.children());
        }
    }

    // ─── builder ─────────

    public static Spec spec() { return new Spec(); }

    public static class Spec {
        Function<String, String> translator = k -> k;
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

        public Spec translator(Function<String, String> v) { this.translator = v; return this; }
        public Spec linkedOrderPositions(Supplier<List<OrderPosition>> v) { this.linkedOrderPositions = v; return this; }
        public Spec linkedPurchasePositions(Supplier<List<PurchasePosition>> v) { this.linkedPurchasePositions = v; return this; }
        public Spec allOrderPositions(Supplier<List<OrderPosition>> v) { this.allOrderPositions = v; return this; }
        public Spec allPurchasePositions(Supplier<List<PurchasePosition>> v) { this.allPurchasePositions = v; return this; }
        public Spec onLinkOrderPosition(Consumer<UUID> v) { this.onLinkOrderPosition = v; return this; }
        public Spec onUnlinkOrderPosition(Consumer<UUID> v) { this.onUnlinkOrderPosition = v; return this; }
        public Spec onLinkPurchasePosition(Consumer<UUID> v) { this.onLinkPurchasePosition = v; return this; }
        public Spec onUnlinkPurchasePosition(Consumer<UUID> v) { this.onUnlinkPurchasePosition = v; return this; }
        public Spec viewKey(String v) { this.viewKey = v; return this; }
        public Spec preferenceService(UserViewPreferenceService v) { this.preferenceService = v; return this; }

        public BusinessLinksTree build() { return new BusinessLinksTree(this); }
    }
}

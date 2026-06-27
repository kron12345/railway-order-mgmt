package com.ordermgmt.railway.ui.component.business;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import com.ordermgmt.railway.domain.business.model.Business;

/**
 * Reverse-link list: shows businesses linked to a given OrderPosition / Order / PurchasePosition,
 * each row jumping to {@code /businesses/{id}}.
 *
 * <p>Caller supplies the already-resolved list and a translator. The component renders a compact
 * Bloomberg-style row per business with status pill and click-to-navigate.
 */
public class LinkedBusinessesPanel extends Div {

    private static final String EMPTY_TITLE = "—";
    private static final int SPACER_FLEX_GROW = 1;

    public LinkedBusinessesPanel(List<Business> businesses, Function<String, String> tr) {
        addClassName("biz-card");

        var titleSpan = new Span(tr.apply("business.linkedBusinesses").toUpperCase());
        titleSpan.addClassName("biz-section-title");
        add(titleSpan);

        if (businesses == null || businesses.isEmpty()) {
            var empty = new Span(tr.apply("business.noLinkedBusinesses"));
            empty.addClassName("biz-empty");
            add(empty);
            return;
        }
        var list = new Div();
        list.addClassName("biz-link-list");
        for (Business business : businesses) {
            list.add(buildRow(business, tr));
        }
        add(list);
    }

    private Component buildRow(Business business, Function<String, String> tr) {
        var row = new HorizontalLayout();
        row.addClassName("biz-link-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        var tag = new Span("BIZ");
        tag.addClassName("biz-tree-tag");
        tag.addClassName("biz-tree-tag--order");

        var title = new Span(displayTitle(business));
        title.addClassName("biz-link-row__name");

        var status = new Span(tr.apply("business.status." + business.getStatus().name()));
        status.addClassName("biz-status-pill-icon");
        status.addClassName(
                "biz-status-pill-icon--" + business.getStatus().name().toLowerCase(Locale.ROOT));

        var spacer = new Div();
        spacer.getStyle().set("flex", "1");

        var goBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
        goBtn.addThemeVariants(
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        goBtn.getElement().setAttribute("aria-label", tr.apply("business.openBusiness"));
        goBtn.addClickListener(e -> navigateToBusiness(business));

        row.add(tag, title, status, spacer, goBtn);
        row.setFlexGrow(SPACER_FLEX_GROW, spacer);
        row.getElement().getStyle().set("cursor", "pointer");
        row.getElement().addEventListener("click", e -> navigateToBusiness(business));
        return row;
    }

    private static String displayTitle(Business business) {
        return business.getTitle() == null || business.getTitle().isBlank()
                ? EMPTY_TITLE
                : business.getTitle();
    }

    private static void navigateToBusiness(Business business) {
        UI.getCurrent().navigate("businesses/" + business.getId());
    }
}

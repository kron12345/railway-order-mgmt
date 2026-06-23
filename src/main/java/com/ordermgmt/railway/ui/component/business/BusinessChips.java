package com.ordermgmt.railway.ui.component.business;

import java.util.List;
import java.util.function.Function;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

import com.ordermgmt.railway.domain.business.model.Business;

/**
 * Compact row of clickable chips for the businesses linked to an order position or purchase
 * position. Each chip jumps to the business detail. Renders nothing when the list is empty, so a
 * caller can add it unconditionally. Clicks stop propagation so they don't trigger the surrounding
 * row's own click/selection.
 */
public class BusinessChips extends Div {

    public BusinessChips(List<Business> businesses, Function<String, String> tr) {
        if (businesses == null || businesses.isEmpty()) {
            return;
        }
        addClassName("biz-chips");

        Span label = new Span(VaadinIcon.BRIEFCASE.create());
        label.addClassName("biz-chips__label");
        label.getElement().setAttribute("aria-hidden", "true");
        add(label);

        for (Business business : businesses) {
            String title =
                    business.getTitle() == null || business.getTitle().isBlank()
                            ? "—"
                            : business.getTitle();
            Span chip = new Span(title);
            chip.addClassName("biz-chip");
            chip.getElement().setAttribute("title", tr.apply("business.openBusiness"));
            chip.getElement().setAttribute("role", "link");
            chip.getElement().setAttribute("tabindex", "0");
            chip.getElement()
                    .addEventListener("click", e -> navigate(business))
                    .addEventData("event.stopPropagation()");
            chip.getElement()
                    .addEventListener("keydown", e -> navigate(business))
                    .setFilter("event.key === 'Enter'");
            add(chip);
        }
    }

    private void navigate(Business business) {
        UI.getCurrent().navigate("businesses/" + business.getId());
    }
}

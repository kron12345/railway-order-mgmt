package com.ordermgmt.railway.ui.component.masterdetail;

import java.util.UUID;

import com.vaadin.flow.component.html.Div;

/**
 * One rendered master-list card paired with its item id, so {@link MasterDetailLayout} can toggle
 * the selected styling/ARIA state and scroll the selected card into view without re-rendering.
 */
final class MasterCardWrapper {

    final UUID id;
    final Div wrapper;

    MasterCardWrapper(UUID id, Div wrapper) {
        this.id = id;
        this.wrapper = wrapper;
    }

    void applySelection(boolean selected) {
        if (selected) {
            wrapper.addClassName("md-card-wrapper--selected");
            wrapper.getElement().setAttribute("aria-selected", "true");
            wrapper.getElement().executeJs("$0.scrollIntoView({block:'nearest', behavior:'auto'})");
        } else {
            wrapper.removeClassName("md-card-wrapper--selected");
            wrapper.getElement().setAttribute("aria-selected", "false");
        }
    }
}

package com.ordermgmt.railway.ui.component.grid;

import java.util.Map;
import java.util.function.Function;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;

/**
 * Small "columns" icon button that opens a popover with per-column visibility checkboxes plus a
 * reset-to-defaults action. Designed for use in the toolbar of any grid bound via {@link
 * GridPreferenceBinder}.
 */
public class GridSettingsButton<T> extends Span {

    public GridSettingsButton(
            GridPreferenceBinder<T> binder,
            Function<String, String> tr,
            Function<String, String> labelForKey) {
        addClassName("grid-settings");

        var hiddenBadge = new Span();
        hiddenBadge.addClassName("grid-settings__badge");
        hiddenBadge.setVisible(false);

        var trigger = new Button(VaadinIcon.OPTIONS.create());
        trigger.addThemeVariants(
                ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        trigger.getElement().setAttribute("aria-label", tr.apply("grid.settings.title"));
        trigger.addClassName("grid-settings__trigger");

        var popover = buildPopover(binder, tr, labelForKey, hiddenBadge);
        popover.setTarget(trigger);

        // Initial badge state.
        refreshBadge(binder, hiddenBadge);

        binder.setOnChanged(() -> refreshBadge(binder, hiddenBadge));

        add(trigger, hiddenBadge);
    }

    private Popover buildPopover(
            GridPreferenceBinder<T> binder,
            Function<String, String> tr,
            Function<String, String> labelForKey,
            Span hiddenBadge) {
        Popover popover = new Popover();
        popover.setPosition(PopoverPosition.BOTTOM_END);
        popover.setWidth("260px");
        popover.addThemeVariants(PopoverVariant.ARROW);
        popover.addClassName("grid-settings__popover");

        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.addClassName("grid-settings__content");

        var header = new Span(tr.apply("grid.settings.columns").toUpperCase());
        header.addClassName("biz-section-title");
        content.add(header);

        Map<String, com.vaadin.flow.component.grid.Grid.Column<T>> columnsByKey =
                binder.columnsByKey();
        for (var entry : columnsByKey.entrySet()) {
            String key = entry.getKey();
            var column = entry.getValue();
            var checkbox = new Checkbox(resolveLabel(key, labelForKey), column.isVisible());
            checkbox.addClassName("grid-settings__cb");
            checkbox.addValueChangeListener(
                    event -> {
                        column.setVisible(Boolean.TRUE.equals(event.getValue()));
                        binder.saveNow();
                        refreshBadge(binder, hiddenBadge);
                    });
            content.add(checkbox);
        }

        var divider = new Span();
        divider.addClassName("grid-settings__divider");
        content.add(divider);

        var resetBtn = new Button(tr.apply("grid.settings.reset"), VaadinIcon.REFRESH.create());
        resetBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        resetBtn.addClassName("grid-settings__reset");
        resetBtn.addClickListener(
                e -> {
                    binder.reset();
                    popover.close();
                    Notification.show(
                                    tr.apply("grid.settings.resetDone"),
                                    1500,
                                    Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                });

        var footer = new HorizontalLayout(resetBtn);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();
        content.add(footer);

        popover.add((Component) content);
        return popover;
    }

    private void refreshBadge(GridPreferenceBinder<T> binder, Span badge) {
        long hiddenColumnCount =
                binder.columnsByKey().values().stream().filter(column -> !column.isVisible()).count();
        if (hiddenColumnCount == 0) {
            badge.setVisible(false);
            return;
        }
        badge.setText("+" + hiddenColumnCount);
        badge.setVisible(true);
    }

    private String resolveLabel(String key, Function<String, String> labelForKey) {
        String label = labelForKey.apply(key);
        return label == null || label.isBlank() ? key : label;
    }
}

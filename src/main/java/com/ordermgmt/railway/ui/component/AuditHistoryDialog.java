package com.ordermgmt.railway.ui.component;

import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.AuditEntry;

/** Reusable dialog that shows the Envers revision history for any entity. */
public class AuditHistoryDialog extends Dialog {

    /**
     * Creates the audit history dialog.
     *
     * @param title dialog header title
     * @param entries list of audit entries to display
     * @param translator i18n translator function
     */
    public AuditHistoryDialog(
            String title,
            List<AuditEntry> entries,
            BiFunction<String, Object[], String> translator) {
        setHeaderTitle(title);
        setWidth("700px");

        if (entries.isEmpty()) {
            Span empty = new Span(translator.apply("audit.noHistory", new Object[0]));
            empty.getStyle()
                    .set("color", "var(--rom-text-muted)")
                    .set("font-size", "13px")
                    .set("padding", "var(--lumo-space-l)");
            add(empty);
        } else {
            Grid<AuditEntry> grid = new Grid<>();
            grid.addColumn(AuditEntry::revision)
                    .setHeader(translator.apply("audit.revision", new Object[0]))
                    .setWidth("60px")
                    .setFlexGrow(0);
            grid.addColumn(AuditEntry::timestamp)
                    .setHeader(translator.apply("audit.timestamp", new Object[0]))
                    .setWidth("150px")
                    .setFlexGrow(0);
            grid.addColumn(AuditEntry::user)
                    .setHeader(translator.apply("audit.user", new Object[0]))
                    .setWidth("120px")
                    .setFlexGrow(0);
            grid.addColumn(AuditEntry::type)
                    .setHeader(translator.apply("audit.action", new Object[0]))
                    .setWidth("90px")
                    .setFlexGrow(0);
            grid.addColumn(AuditEntry::changes)
                    .setHeader(translator.apply("audit.changes", new Object[0]))
                    .setFlexGrow(1);

            grid.setItems(entries);
            grid.setWidthFull();
            grid.setHeight("400px");
            add(grid);
        }

        Button closeBtn =
                new Button(translator.apply("common.cancel", new Object[0]), e -> close());
        getFooter().add(closeBtn);
    }
}

package com.ordermgmt.railway.ui.component.a11y;

import java.util.List;
import java.util.function.Function;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;

/** Modal listing all keyboard shortcuts. Triggered globally via {@code Shift + ?}. */
public class KeyboardHelpOverlay extends Dialog {

    public KeyboardHelpOverlay(Function<String, String> tr) {
        addClassName("kbd-help");
        setHeaderTitle(tr.apply("a11y.help.title"));
        setWidth("520px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        add(
                buildSection(
                        tr.apply("a11y.help.global"),
                        List.of(
                                row("g o", tr.apply("a11y.help.gotoOrders")),
                                row("g b", tr.apply("a11y.help.gotoBusinesses")),
                                row("g h", tr.apply("a11y.help.gotoHome")),
                                row("Ctrl + K", tr.apply("a11y.help.palette")),
                                row("?", tr.apply("a11y.help.thisHelp")))));

        add(
                buildSection(
                        tr.apply("a11y.help.list"),
                        List.of(
                                row("/", tr.apply("a11y.help.focusFilter")),
                                row("↑ ↓", tr.apply("a11y.help.navList")),
                                row("Home / End", tr.apply("a11y.help.firstLast")),
                                row("Enter", tr.apply("a11y.help.activate")),
                                row("Esc", tr.apply("a11y.help.clearFilter")),
                                row("n", tr.apply("a11y.help.newItem")))));

        add(
                buildSection(
                        tr.apply("a11y.help.detail"),
                        List.of(
                                row("Alt + U", tr.apply("a11y.help.backToOrder")),
                                row("Tab / Shift+Tab", tr.apply("a11y.help.tabFields")))));
    }

    private Div buildSection(String title, List<Div> rows) {
        Div section = new Div();
        section.addClassName("kbd-help__section");

        H3 heading = new H3(title);
        heading.addClassName("kbd-help__heading");

        section.add(heading);
        rows.forEach(section::add);
        return section;
    }

    private Div row(String key, String description) {
        Div row = new Div();
        row.addClassName("kbd-help__row");

        Span keyLabel = new Span(key);
        keyLabel.addClassName("kbd-help__key");

        Span descriptionLabel = new Span(description);
        descriptionLabel.addClassName("kbd-help__desc");

        row.add(keyLabel, descriptionLabel);
        return row;
    }
}

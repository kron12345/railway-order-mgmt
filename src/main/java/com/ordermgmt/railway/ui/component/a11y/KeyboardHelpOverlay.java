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
        Div sec = new Div();
        sec.addClassName("kbd-help__section");
        H3 h = new H3(title);
        h.addClassName("kbd-help__heading");
        sec.add(h);
        rows.forEach(sec::add);
        return sec;
    }

    private Div row(String key, String description) {
        Div r = new Div();
        r.addClassName("kbd-help__row");
        Span k = new Span(key);
        k.addClassName("kbd-help__key");
        Span d = new Span(description);
        d.addClassName("kbd-help__desc");
        r.add(k, d);
        return r;
    }
}

package com.ordermgmt.railway.ui.component;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.PurchaseStatus;

/**
 * Full purchase calendar panel: summary stats + compact calendar grid + detail table.
 * Shown inline below an order position when toggled.
 */
public class PurchaseCalendarPanel extends Div {

    private final BiFunction<String, Object[], String> t;

    public PurchaseCalendarPanel(
            OrderPosition position,
            List<PurchasePosition> purchases,
            BiFunction<String, Object[], String> translator) {
        this.t = translator;
        getStyle()
                .set("background", "var(--rom-bg-primary)")
                .set("border", "1px solid var(--rom-border)")
                .set("border-radius", "6px")
                .set("padding", "16px")
                .set("margin-top", "8px");

        add(createHeader(position, purchases));
        add(createTtrBar());
        add(createCalendar(purchases));
        add(createLegend());
        add(createDetailTable(purchases));
    }

    private Div createHeader(OrderPosition pos, List<PurchasePosition> purchases) {
        Div header = new Div();
        header.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("flex-wrap", "wrap")
                .set("gap", "8px")
                .set("margin-bottom", "12px");

        Span title = new Span(tr("purchase.calendar") + " — " + pos.getName());
        title.getStyle()
                .set("font-weight", "600")
                .set("font-size", "13px")
                .set("color", "var(--rom-text-primary)");

        Div stats = new Div();
        stats.getStyle().set("display", "flex").set("gap", "12px").set("flex-wrap", "wrap");

        long confirmed = purchases.stream().filter(p -> p.getPurchaseStatus() == PurchaseStatus.BESTAETIGT).count();
        long ordered = purchases.stream().filter(p -> p.getPurchaseStatus() == PurchaseStatus.BESTELLT).count();
        long open = purchases.stream().filter(p -> p.getPurchaseStatus() == PurchaseStatus.OFFEN).count();
        long rejected = purchases.stream().filter(p -> p.getPurchaseStatus() == PurchaseStatus.ABGELEHNT).count();

        stats.add(statBadge(confirmed, tr("purchase.calendar.legend.confirmed"), "var(--rom-status-active)"));
        stats.add(statBadge(ordered, tr("purchase.calendar.legend.ordered"), "var(--rom-status-info)"));
        stats.add(statBadge(open, tr("purchase.calendar.legend.open"), "var(--rom-text-muted)"));
        if (rejected > 0) {
            stats.add(statBadge(rejected, tr("purchase.calendar.legend.rejected"), "var(--rom-status-danger)"));
        }

        header.add(title, stats);
        return header;
    }

    private Div createTtrBar() {
        Div bar = new Div();
        bar.getStyle()
                .set("display", "flex")
                .set("gap", "8px")
                .set("align-items", "center")
                .set("margin-bottom", "12px")
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)");

        bar.add(ttrBadge(tr("purchase.calendar.ttr.fpj", "2027"), "var(--rom-accent)", "rgba(45,212,191,0.1)"));
        bar.add(ttrBadge(tr("purchase.calendar.ttr.phase5"), "#FBBF24", "rgba(251,191,36,0.1)"));
        Span info = new Span(tr("purchase.calendar.ttr.change", "12.12.2026"));
        info.getStyle().set("font-family", "'JetBrains Mono', monospace").set("font-size", "10px");
        bar.add(info);

        return bar;
    }

    private Div createCalendar(List<PurchasePosition> purchases) {
        LocalDate from = LocalDate.of(2026, 12, 1);
        LocalDate to = LocalDate.of(2027, 3, 31);
        Div wrap = new Div();
        wrap.getStyle().set("margin-bottom", "12px");
        wrap.add(new PurchaseCalendarGrid(purchases, from, to));
        return wrap;
    }

    private Div createLegend() {
        Div legend = new Div();
        legend.getStyle()
                .set("display", "flex")
                .set("gap", "12px")
                .set("flex-wrap", "wrap")
                .set("margin-bottom", "12px")
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)");

        legend.add(legendItem(tr("purchase.calendar.legend.confirmed"), "rgba(52,211,153,0.2)", "var(--rom-status-active)"));
        legend.add(legendItem(tr("purchase.calendar.legend.ordered"), "rgba(96,165,250,0.2)", "var(--rom-status-info)"));
        legend.add(legendItem(tr("purchase.calendar.legend.open"), "rgba(148,163,184,0.06)", "var(--rom-text-muted)"));
        legend.add(legendItem(tr("purchase.calendar.legend.rejected"), "rgba(248,113,113,0.2)", "var(--rom-status-danger)"));

        return legend;
    }

    private Div createDetailTable(List<PurchasePosition> purchases) {
        Div section = new Div();

        Span title = new Span(tr("purchase.calendar.detail", String.valueOf(purchases.size())));
        title.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "10px")
                .set("color", "var(--rom-text-muted)")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.06em")
                .set("display", "block")
                .set("margin-bottom", "8px");
        section.add(title);

        if (purchases.isEmpty()) {
            Span empty = new Span(tr("purchase.calendar.none"));
            empty.getStyle().set("color", "var(--rom-text-muted)").set("font-size", "12px");
            section.add(empty);
            return section;
        }

        section.add(new PurchaseDetailTable(purchases, t));
        return section;
    }

    private Div statBadge(long count, String label, String color) {
        Div badge = new Div();
        badge.getStyle().set("display", "flex").set("align-items", "center").set("gap", "4px");
        Div dot = new Div();
        dot.getStyle().set("width", "8px").set("height", "8px").set("border-radius", "2px").set("background", color);
        Span num = new Span(String.valueOf(count));
        num.getStyle().set("font-family", "'JetBrains Mono', monospace").set("font-size", "13px")
                .set("font-weight", "700").set("color", "var(--rom-text-primary)");
        Span lbl = new Span(label);
        lbl.getStyle().set("font-size", "10px").set("color", "var(--rom-text-muted)");
        badge.add(dot, num, lbl);
        return badge;
    }

    private Span ttrBadge(String text, String color, String bg) {
        Span badge = new Span(text);
        badge.getStyle()
                .set("font-family", "'JetBrains Mono', monospace").set("font-size", "10px")
                .set("font-weight", "600").set("padding", "2px 8px").set("border-radius", "3px")
                .set("color", color).set("background", bg).set("border", "1px solid " + color);
        return badge;
    }

    private Div legendItem(String label, String bg, String color) {
        Div item = new Div();
        item.getStyle().set("display", "flex").set("align-items", "center").set("gap", "4px");
        Div box = new Div();
        box.getStyle().set("width", "12px").set("height", "12px").set("border-radius", "2px")
                .set("background", bg).set("border", "1px solid " + color);
        item.add(box, new Span(label));
        return item;
    }

    private String tr(String key) {
        return t.apply(key, new Object[0]);
    }

    private String tr(String key, String param) {
        return t.apply(key, new Object[]{param});
    }
}

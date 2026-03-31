package com.ordermgmt.railway.ui.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLayout;

import com.ordermgmt.railway.ui.component.LanguageSwitcher;

/** Shared application shell with navigation, breadcrumbs, and locale switching. */
public class MainLayout extends AppLayout
        implements RouterLayout, LocaleChangeObserver, AfterNavigationObserver {

    private H1 title;
    private SideNav sideNav;
    private Div breadcrumb;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        title = new H1(getTranslation("app.title"));
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("margin", "0")
                .set("color", "var(--rom-accent)");

        LanguageSwitcher languageSwitcher = new LanguageSwitcher();

        breadcrumb = new Div();
        breadcrumb.addClassName("breadcrumb");
        breadcrumb.getElement().setAttribute("role", "navigation");
        breadcrumb.getElement().setAttribute("aria-label", "Breadcrumb");

        HorizontalLayout topRow =
                new HorizontalLayout(new DrawerToggle(), title, breadcrumb, languageSwitcher);
        topRow.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        topRow.expand(breadcrumb);
        topRow.setWidthFull();
        topRow.getStyle()
                .set("padding", "0 var(--lumo-space-m)")
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)");

        addToNavbar(topRow);
    }

    private void createDrawer() {
        Div logo = new Div();
        Span brand = new Span("ROM");
        brand.getStyle()
                .set("font-family", "'JetBrains Mono', monospace")
                .set("font-size", "1.5rem")
                .set("font-weight", "700")
                .set("color", "var(--rom-accent)")
                .set("letter-spacing", "0.15em");
        Span sub = new Span("Railway Order Mgmt");
        sub.getStyle()
                .set("font-size", "0.65rem")
                .set("color", "var(--rom-text-muted)")
                .set("display", "block")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.1em");
        logo.add(brand, sub);
        logo.getStyle().set("padding", "var(--lumo-space-m)");

        sideNav = new SideNav();
        updateNavItems();

        VerticalLayout drawerLayout = new VerticalLayout(logo, sideNav);
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout
                .getStyle()
                .set("background", "var(--rom-bg-secondary)")
                .set("border-right", "1px solid var(--rom-border)");

        addToDrawer(drawerLayout);
    }

    private void updateNavItems() {
        sideNav.removeAll();
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.dashboard"), "", VaadinIcon.DASHBOARD.create()));
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.orders"), "orders", VaadinIcon.CLIPBOARD.create()));
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.customers"), "customers", VaadinIcon.USERS.create()));
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.settings"), "settings", VaadinIcon.COG.create()));
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.profile"), "profile", VaadinIcon.USER.create()));
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        title.setText(getTranslation("app.title"));
        updateNavItems();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        updateBreadcrumb(event.getLocation().getPath());
    }

    private void updateBreadcrumb(String path) {
        breadcrumb.removeAll();

        Anchor home = new Anchor("", "Dashboard");
        breadcrumb.add(home);

        if (path != null && !path.isBlank() && !path.equals("/")) {
            String[] parts = path.split("/");
            StringBuilder href = new StringBuilder();
            for (String part : parts) {
                if (part.isBlank()) continue;
                href.append(part);

                Span sep = new Span("›");
                sep.addClassName("sep");
                breadcrumb.add(sep);

                Anchor link = new Anchor(href.toString(), formatBreadcrumbLabel(part));
                breadcrumb.add(link);
                href.append("/");
            }
        }
    }

    private String formatBreadcrumbLabel(String segment) {
        return switch (segment) {
            case "orders" -> getTranslation("nav.orders");
            case "settings" -> getTranslation("nav.settings");
            case "customers" -> getTranslation("nav.customers");
            case "new" -> getTranslation("order.new");
            default -> {
                if (segment.matches("[0-9a-f\\-]{36}")) {
                    yield segment.substring(0, 8) + "…";
                }
                yield segment;
            }
        };
    }
}

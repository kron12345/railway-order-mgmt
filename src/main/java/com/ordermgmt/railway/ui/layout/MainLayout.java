package com.ordermgmt.railway.ui.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
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
import com.vaadin.flow.router.RouterLayout;

import com.ordermgmt.railway.ui.component.LanguageSwitcher;

/** Shared application shell with navigation and locale switching. */
public class MainLayout extends AppLayout implements RouterLayout, LocaleChangeObserver {

    private H1 title;
    private SideNav sideNav;

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

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title, languageSwitcher);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.getStyle()
                .set("padding", "0 var(--lumo-space-m)")
                .set("background", "var(--rom-bg-secondary)")
                .set("border-bottom", "1px solid var(--rom-border)");

        addToNavbar(header);
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
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        title.setText(getTranslation("app.title"));
        updateNavItems();
    }
}

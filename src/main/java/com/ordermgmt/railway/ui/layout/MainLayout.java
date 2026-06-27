package com.ordermgmt.railway.ui.layout;

import java.util.regex.Pattern;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
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

import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.order.service.OrderService;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;
import com.ordermgmt.railway.ui.component.LanguageSwitcher;
import com.ordermgmt.railway.ui.component.a11y.CommandPalette;
import com.ordermgmt.railway.ui.component.a11y.KeyboardHelpOverlay;
import com.ordermgmt.railway.ui.theme.UiThemeUtil;

/** Shared application shell with navigation, breadcrumbs, and locale switching. */
@NpmPackage(value = "hotkeys-js", version = "3.13.7")
@JsModule("./rom-shortcuts.ts")
public class MainLayout extends AppLayout
        implements RouterLayout, LocaleChangeObserver, AfterNavigationObserver {

    private static final Pattern UUID_SEGMENT = Pattern.compile("[0-9a-f\\-]{36}");

    private final KeycloakUserService keycloakUserService;
    private final OrderService orderService;
    private final BusinessService businessService;
    private H1 title;
    private SideNav sideNav;
    private Div breadcrumb;

    public MainLayout(
            KeycloakUserService keycloakUserService,
            OrderService orderService,
            BusinessService businessService) {
        this.keycloakUserService = keycloakUserService;
        this.orderService = orderService;
        this.businessService = businessService;
        setPrimarySection(Section.DRAWER);
        applyCurrentUserTheme();
        createHeader();
        createDrawer();
        registerGlobalShortcuts();
    }

    private void registerGlobalShortcuts() {
        // Wires global vim-style keys, Ctrl+K (command palette), and Shift+? (help)
        // through the hotkeys-js helper in frontend/rom-shortcuts.ts. The helper
        // dispatches CustomEvents that Vaadin listens for below.
        getElement().executeJs("window.romShortcuts && window.romShortcuts.registerGlobal(this);");

        // Bridge custom DOM events back into Vaadin so we can open dialogs server-side.
        getElement().addEventListener("rom-palette", e -> openCommandPalette());
        getElement().addEventListener("rom-help", e -> openHelpOverlay());
    }

    private void openCommandPalette() {
        new CommandPalette(orderService, businessService).open();
    }

    private void openHelpOverlay() {
        new KeyboardHelpOverlay(this::getTranslation).open();
    }

    private void applyCurrentUserTheme() {
        String userId = CurrentUserHelper.getUserId();
        if (userId == null) {
            UiThemeUtil.apply(UI.getCurrent(), UiThemeUtil.DEFAULT_THEME);
            return;
        }

        String themeName =
                keycloakUserService
                        .getUserAttributes(userId)
                        .getOrDefault("theme", UiThemeUtil.DEFAULT_THEME);
        UiThemeUtil.apply(UI.getCurrent(), themeName);
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
                        getTranslation("nav.r2pInbox"), "r2p-inbox", VaadinIcon.INBOX.create()));
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.offenePositionen"),
                        "offene-positionen",
                        VaadinIcon.EXCLAMATION_CIRCLE_O.create()));
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.fristen"), "fristen", VaadinIcon.ALARM.create()));
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.businesses"),
                        "businesses",
                        VaadinIcon.BRIEFCASE.create()));
        sideNav.addItem(
                new SideNavItem(
                        getTranslation("nav.customers"), "customers", VaadinIcon.USERS.create()));

        // Fahrplanmanager + Rollmaterial are temporary test stand-ins that will later be sourced
        // from the external RailOpt system. They are grouped under a collapsed "RailOpt (Demo)"
        // section so they stay reachable without cluttering the primary navigation or confusing
        // customers during the demo.
        SideNavItem external = new SideNavItem(getTranslation("nav.external"));
        external.setPrefixComponent(VaadinIcon.FLASK.create());
        external.addItem(
                new SideNavItem(
                        getTranslation("nav.pathmanager"),
                        "pathmanager",
                        VaadinIcon.TRAIN.create()));
        external.addItem(
                new SideNavItem(
                        getTranslation("nav.railcars"), "rollingstock", VaadinIcon.TRAIN.create()));
        sideNav.addItem(external);

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
                if (part.isBlank()) {
                    continue;
                }
                href.append(part);
                addBreadcrumbSegment(href.toString(), part);
                href.append("/");
            }
        }
    }

    private void addBreadcrumbSegment(String href, String segment) {
        Span separator = new Span("›");
        separator.addClassName("sep");

        Anchor link = new Anchor(href, formatBreadcrumbLabel(segment));
        breadcrumb.add(separator, link);
    }

    private String formatBreadcrumbLabel(String segment) {
        return switch (segment) {
            case "orders" -> getTranslation("nav.orders");
            case "settings" -> getTranslation("nav.settings");
            case "customers" -> getTranslation("nav.customers");
            case "new" -> getTranslation("order.new");
            case "timetable-builder" -> getTranslation("timetable.builder.title");
            case "pathmanager" -> getTranslation("nav.pathmanager");
            default -> {
                if (UUID_SEGMENT.matcher(segment).matches()) {
                    yield segment.substring(0, 8) + "…";
                }
                yield segment;
            }
        };
    }
}

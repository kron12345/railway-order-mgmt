package com.ordermgmt.railway.ui.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.ordermgmt.railway.ui.component.LanguageSwitcher;

@Layout
public class MainLayout extends AppLayout implements LocaleChangeObserver {

    private H1 title;
    private SideNav sideNav;

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        title = new H1(getTranslation("app.title"));
        title.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        LanguageSwitcher languageSwitcher = new LanguageSwitcher();

        HorizontalLayout header = new HorizontalLayout(
                new DrawerToggle(), title, languageSwitcher
        );
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(title);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void createDrawer() {
        sideNav = new SideNav();
        updateNavItems();

        VerticalLayout drawerLayout = new VerticalLayout(sideNav);
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);

        addToDrawer(drawerLayout);
    }

    private void updateNavItems() {
        sideNav.removeAll();
        // Navigation items will be added as views are implemented
        // Example:
        // sideNav.addItem(new SideNavItem(getTranslation("nav.dashboard"), DashboardView.class));
        // sideNav.addItem(new SideNavItem(getTranslation("nav.orders"), OrderListView.class));
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        title.setText(getTranslation("app.title"));
        updateNavItems();
    }
}

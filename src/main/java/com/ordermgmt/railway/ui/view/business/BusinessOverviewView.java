package com.ordermgmt.railway.ui.view.business;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.order.service.AuditService;
import com.ordermgmt.railway.domain.userprefs.service.UserViewPreferenceService;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;
import com.ordermgmt.railway.ui.component.a11y.SkipLinks;
import com.ordermgmt.railway.ui.component.business.BusinessCard;
import com.ordermgmt.railway.ui.component.masterdetail.MasterDetailLayout;
import com.ordermgmt.railway.ui.component.masterdetail.filter.DateRangeFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.SelectFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.TextFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.ToggleFilterField;
import com.ordermgmt.railway.ui.layout.MainLayout;

/**
 * Master-detail overview for businesses. Serves both <code>/businesses</code> (list with empty
 * detail) and <code>/businesses/{id}</code> (list + selected business detail).
 *
 * <p>Keyboard navigation, ARIA listbox semantics, type-ahead filter, and skip-links are provided by
 * {@link MasterDetailLayout} and {@link SkipLinks}; URL drives selection so deep links and browser
 * back/forward keep working.
 */
@Route(value = "businesses/:businessId/:mode", layout = MainLayout.class)
@RouteAlias(value = "businesses/:businessId", layout = MainLayout.class)
@RouteAlias(value = "businesses", layout = MainLayout.class)
@PageTitle("Geschäfte")
@PermitAll
public class BusinessOverviewView extends VerticalLayout implements BeforeEnterObserver {

    private final BusinessService businessService;
    private final UserViewPreferenceService prefsService;
    private final KeycloakUserService keycloakUserService;
    private final AuditService auditService;
    private final MasterDetailLayout<Business> shell;
    private final Map<UUID, int[]> linkCountsCache = new HashMap<>();

    public BusinessOverviewView(
            BusinessService businessService,
            UserViewPreferenceService prefsService,
            KeycloakUserService keycloakUserService,
            AuditService auditService) {
        this.businessService = businessService;
        this.prefsService = prefsService;
        this.keycloakUserService = keycloakUserService;
        this.auditService = auditService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "var(--rom-bg-primary)");
        addClassName("biz-overview");

        add(buildSkipLinks());

        Function<String, String> tr = this::getTranslation;
        shell =
                MasterDetailLayout.<Business>spec()
                        .idExtractor(Business::getId)
                        .cardRenderer(
                                b -> {
                                    int[] counts = countsFor(b);
                                    return new BusinessCard(
                                            b,
                                            tr,
                                            counts[0],
                                            counts[1],
                                            businessService,
                                            keycloakUserService,
                                            this::loadBusinesses);
                                })
                        .matcher(
                                (b, q) -> {
                                    String title =
                                            b.getTitle() == null ? "" : b.getTitle().toLowerCase();
                                    String desc =
                                            b.getDescription() == null
                                                    ? ""
                                                    : b.getDescription().toLowerCase();
                                    String tags =
                                            b.getTags() == null ? "" : b.getTags().toLowerCase();
                                    return title.contains(q)
                                            || desc.contains(q)
                                            || tags.contains(q);
                                })
                        .filterPlaceholder(getTranslation("business.filterPlaceholder"))
                        .filterAriaLabel(getTranslation("business.search"))
                        .filterId("biz-filter")
                        .listId("biz-list")
                        .detailId("biz-detail")
                        .listAriaLabel(getTranslation("business.list.aria"))
                        .detailAriaLabel(getTranslation("business.detail.aria"))
                        .toolbarAriaLabel(getTranslation("business.toolbar.aria"))
                        .filterFields(buildFilterFields())
                        .filterToggleLabel(getTranslation("filter.toggle"))
                        .filterClearAllLabel(getTranslation("filter.clearAll"))
                        .filterChipClearAria(getTranslation("filter.chip.clearAria"))
                        .filterPanelAria(getTranslation("filter.panel.aria"))
                        .emptyText(getTranslation("business.empty"))
                        .detailEmptyText(getTranslation("business.detail.empty"))
                        .announceTemplate(
                                (b, idx, total) ->
                                        getTranslation(
                                                "business.announce.selected",
                                                idx,
                                                total,
                                                b.getTitle() == null ? "—" : b.getTitle()))
                        .extraToolbar(canMutate() ? List.of(buildNewButton()) : List.of())
                        .shortcutNew(
                                canMutate()
                                        ? () -> UI.getCurrent().navigate("businesses/new")
                                        : null)
                        .onSelect(id -> UI.getCurrent().navigate("businesses/" + id))
                        .build();
        shell.setSizeFull();
        add(shell);
        setFlexGrow(1, shell);

        loadBusinesses();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String param = event.getRouteParameters().get("businessId").orElse(null);
        String mode = event.getRouteParameters().get("mode").orElse(null);
        loadBusinesses();
        if (param == null) {
            shell.setSelectedId(null);
            shell.setDetail(null);
            return;
        }
        if ("new".equals(param)) {
            // Defence-in-depth: non-mutators can reach /businesses/new by URL though the button is
            // hidden; bounce them to the list (service also guards on save).
            if (!canMutate()) {
                UI.getCurrent().navigate("businesses");
                return;
            }
            shell.setSelectedId(null);
            shell.setDetail(new BusinessDetailView(businessService, prefsService, null));
            return;
        }
        try {
            UUID id = UUID.fromString(param);
            Business b = businessService.getById(id).orElse(null);
            if (b == null) {
                UI.getCurrent().navigate("businesses");
                return;
            }
            shell.setSelectedId(id);
            if ("edit".equals(mode) && canMutate()) {
                shell.setDetail(new BusinessDetailView(businessService, prefsService, id));
            } else {
                shell.setDetail(
                        new BusinessReadView(
                                businessService, auditService, id, this::getTranslation));
            }
        } catch (IllegalArgumentException ex) {
            UI.getCurrent().navigate("businesses");
        }
    }

    /**
     * Filter criteria for the business list — same shape as the order list (status / validity range
     * / tags / "assigned to me") so both views look and behave identically. "Assigned to me"
     * matches USER-type assignments whose name is the current Keycloak user.
     */
    private List<FilterField<Business>> buildFilterFields() {
        String me = CurrentUserHelper.getUsername();
        return List.of(
                new SelectFilterField<>(
                        getTranslation("filter.field.status"),
                        List.of(BusinessStatus.values()),
                        s -> getTranslation("business.status." + s.name()),
                        Business::getStatus),
                new DateRangeFilterField<>(
                        getTranslation("filter.field.dateFrom"),
                        getTranslation("filter.field.dateTo"),
                        Business::getValidFrom,
                        Business::getValidTo),
                new TextFilterField<>(getTranslation("filter.field.tags"), Business::getTags),
                new ToggleFilterField<>(
                        getTranslation("filter.field.assignedToMe"),
                        b ->
                                "USER".equals(b.getAssignmentType())
                                        && me != null
                                        && me.equals(b.getAssignmentName())));
    }

    private Component buildSkipLinks() {
        return new SkipLinks(
                List.of(
                        new SkipLinks.SkipTarget("biz-filter", getTranslation("a11y.skip.filter")),
                        new SkipLinks.SkipTarget("biz-list", getTranslation("a11y.skip.list")),
                        new SkipLinks.SkipTarget(
                                "biz-detail", getTranslation("a11y.skip.detail"))));
    }

    private boolean canMutate() {
        return CurrentUserHelper.hasAnyRole("ADMIN", "DISPATCHER");
    }

    private Component buildNewButton() {
        var btn = new Button(getTranslation("business.new"), VaadinIcon.PLUS.create());
        btn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        btn.getElement().setAttribute("aria-keyshortcuts", "n");
        btn.addClickListener(e -> UI.getCurrent().navigate("businesses/new"));
        return btn;
    }

    private void loadBusinesses() {
        linkCountsCache.clear();
        linkCountsCache.putAll(businessService.linkCounts());
        shell.setItems(businessService.listAll());
    }

    private int[] countsFor(Business b) {
        return linkCountsCache.getOrDefault(b.getId(), new int[] {0, 0});
    }
}

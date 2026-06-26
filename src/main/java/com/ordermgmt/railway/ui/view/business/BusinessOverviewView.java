package com.ordermgmt.railway.ui.view.business;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import jakarta.annotation.security.PermitAll;

import org.springframework.data.domain.Slice;

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
import com.ordermgmt.railway.dto.business.BusinessListItem;
import com.ordermgmt.railway.dto.business.BusinessListQuery;
import com.ordermgmt.railway.infrastructure.keycloak.CurrentUserHelper;
import com.ordermgmt.railway.infrastructure.keycloak.KeycloakUserService;
import com.ordermgmt.railway.ui.component.a11y.SkipLinks;
import com.ordermgmt.railway.ui.component.business.BusinessCard;
import com.ordermgmt.railway.ui.component.masterdetail.MasterDetailLayout;
import com.ordermgmt.railway.ui.component.masterdetail.SliceResult;
import com.ordermgmt.railway.ui.component.masterdetail.filter.DateRangeFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.FilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.SelectFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.TextFilterField;
import com.ordermgmt.railway.ui.component.masterdetail.filter.ToggleFilterField;
import com.ordermgmt.railway.ui.layout.MainLayout;
import com.ordermgmt.railway.ui.support.OffsetPageable;

/**
 * Master-detail overview for businesses. Serves both <code>/businesses</code> (list with empty
 * detail) and <code>/businesses/{id}</code> (list + selected business detail).
 *
 * <p>The list is lazy (P4): {@link MasterDetailLayout} pulls {@link BusinessListItem} pages from
 * {@link BusinessService#searchBusinesses} via {@link #lazyLoadBusinesses}; filters become a
 * server-side {@link BusinessListQuery}, so no full business list (and no n:m link counts) is ever
 * materialized. Keyboard navigation, ARIA listbox semantics, type-ahead filter, and skip-links are
 * provided by {@link MasterDetailLayout} and {@link SkipLinks}; URL drives selection.
 */
@Route(value = "businesses/:businessId/:mode", layout = MainLayout.class)
@RouteAlias(value = "businesses/:businessId", layout = MainLayout.class)
@RouteAlias(value = "businesses", layout = MainLayout.class)
@PageTitle("Geschäfte")
@PermitAll
public class BusinessOverviewView extends VerticalLayout implements BeforeEnterObserver {

    private static final int PAGE_SIZE = 50;

    private final BusinessService businessService;
    private final UserViewPreferenceService prefsService;
    private final KeycloakUserService keycloakUserService;
    private final AuditService auditService;

    // Not final: the cardRenderer lambda (built during the spec chain) reads shell to reloadLazy().
    private MasterDetailLayout<BusinessListItem> shell;

    // Filter controls — held so the lazy loader can read their values into a BusinessListQuery.
    private SelectFilterField<BusinessListItem, BusinessStatus> statusField;
    private DateRangeFilterField<BusinessListItem> dateRangeField;
    private TextFilterField<BusinessListItem> tagsField;
    private ToggleFilterField<BusinessListItem> assignedToMeField;

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
                MasterDetailLayout.<BusinessListItem>spec()
                        .idExtractor(BusinessListItem::id)
                        .cardRenderer(
                                b ->
                                        new BusinessCard(
                                                b,
                                                tr,
                                                businessService,
                                                keycloakUserService,
                                                () -> shell.reloadLazy()))
                        // Inert in lazy mode (applyFilter delegates to the server query); kept so
                        // the
                        // spec stays valid and the legacy in-memory path would still work if
                        // reused.
                        .matcher(
                                (b, q) -> {
                                    String title = b.title() == null ? "" : b.title().toLowerCase();
                                    String tags = b.tags() == null ? "" : b.tags().toLowerCase();
                                    return title.contains(q) || tags.contains(q);
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
                        .readoutLoadedLabel(getTranslation("md.lazy.loaded"))
                        .readoutMoreLabel(getTranslation("md.lazy.more"))
                        .readoutFilteredLabel(getTranslation("md.lazy.filtered"))
                        .sentinelLabel(getTranslation("md.lazy.sentinel", PAGE_SIZE))
                        .announceTemplate(
                                (b, idx, total) ->
                                        getTranslation(
                                                "business.announce.selected",
                                                idx,
                                                total,
                                                b.title() == null ? "—" : b.title()))
                        .lazyAnnounceTemplate(
                                (b, idx) ->
                                        getTranslation(
                                                "business.announce.selected.lazy",
                                                idx,
                                                b.title() == null ? "—" : b.title()))
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

        shell.setLazyLoader(this::lazyLoadBusinesses);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String param = event.getRouteParameters().get("businessId").orElse(null);
        String mode = event.getRouteParameters().get("mode").orElse(null);
        // Bare list: (re)load page 1. A specific business: load page 1 only if nothing is loaded
        // yet, so an accumulated list keeps its pages/scroll/selection when a card is clicked.
        if (param == null) {
            shell.reloadLazy();
            shell.setSelectedId(null);
            shell.setDetail(null);
            return;
        }
        shell.ensureLoaded();
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
     * One lazy page of the business list: builds a {@link BusinessListQuery} from the search text
     * plus the held filter controls and asks the search service for the slice at {@code offset}.
     */
    private SliceResult<BusinessListItem> lazyLoadBusinesses(String text, int offset) {
        String me = CurrentUserHelper.getUsername();
        BusinessListQuery query =
                new BusinessListQuery(
                        text,
                        statusField.getSelectedValue(),
                        dateRangeField.getFrom(),
                        dateRangeField.getTo(),
                        tagsField.getTextValue(),
                        assignedToMeField.isToggled() ? me : null);
        Slice<BusinessListItem> slice =
                businessService.searchBusinesses(query, new OffsetPageable(offset, PAGE_SIZE));
        return new SliceResult<>(slice.getContent(), slice.hasNext());
    }

    /**
     * Filter criteria for the business list (status / validity range / tags / "assigned to me") —
     * same shape as the order list. "Assigned to me" matches USER-type assignments whose name is
     * the current Keycloak user. Stored as fields so {@link #lazyLoadBusinesses} can read them.
     */
    private List<FilterField<BusinessListItem>> buildFilterFields() {
        String me = CurrentUserHelper.getUsername();
        statusField =
                new SelectFilterField<>(
                        getTranslation("filter.field.status"),
                        List.of(BusinessStatus.values()),
                        s -> getTranslation("business.status." + s.name()),
                        BusinessListItem::status);
        // Server-side (searchBusinesses) this is a full validFrom/validTo overlap on the entity
        // (getFrom -> validTo >= from, getTo -> validFrom <= to). The in-memory predicate is only a
        // fallback and degrades here because BusinessListItem carries no validFrom (start-extractor
        // is null); it is never executed in lazy mode, so the server overlap is what actually runs.
        dateRangeField =
                new DateRangeFilterField<>(
                        getTranslation("filter.field.dateFrom"),
                        getTranslation("filter.field.dateTo"),
                        b -> null,
                        BusinessListItem::validTo);
        tagsField =
                new TextFilterField<>(getTranslation("filter.field.tags"), BusinessListItem::tags);
        assignedToMeField =
                new ToggleFilterField<>(
                        getTranslation("filter.field.assignedToMe"),
                        b ->
                                "USER".equals(b.assignmentType())
                                        && me != null
                                        && me.equals(b.assignmentName()));
        return List.of(statusField, dateRangeField, tagsField, assignedToMeField);
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
}

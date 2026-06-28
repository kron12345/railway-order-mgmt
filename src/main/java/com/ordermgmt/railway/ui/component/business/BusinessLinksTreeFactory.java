package com.ordermgmt.railway.ui.component.business;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.service.BusinessService;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.userprefs.service.UserViewPreferenceService;

/**
 * Builds the linked-positions tree card for {@link
 * com.ordermgmt.railway.ui.view.business.BusinessDetailView}. In draft (new-business) mode the
 * link/unlink callbacks accumulate into the passed id sets and the "linked" suppliers resolve them
 * against the catalog; in edit mode they go straight through the {@link BusinessService}. Extracted
 * so the detail view keeps only its form, header and save logic.
 */
public final class BusinessLinksTreeFactory {

    private BusinessLinksTreeFactory() {}

    public static Component build(
            boolean isNew,
            Business business,
            BusinessService businessService,
            UserViewPreferenceService prefsService,
            Set<UUID> draftOrderPositionIds,
            Set<UUID> draftPurchasePositionIds,
            Component i18n) {
        var card = new Div();
        card.addClassName("biz-card");
        card.addClassName("biz-card--flex");

        var tree =
                BusinessLinksTree.spec()
                        .translator(i18n::getTranslation)
                        .linkedOrderPositions(
                                isNew
                                        ? () ->
                                                draftOrderPositions(
                                                        businessService, draftOrderPositionIds)
                                        : () ->
                                                businessService.getLinkedOrderPositions(
                                                        business.getId()))
                        .linkedPurchasePositions(
                                isNew
                                        ? () ->
                                                draftPurchasePositions(
                                                        businessService, draftPurchasePositionIds)
                                        : () ->
                                                businessService.getLinkedPurchasePositions(
                                                        business.getId()))
                        .allOrderPositions(businessService::getAllOrderPositions)
                        .allPurchasePositions(businessService::getAllPurchasePositions)
                        .onLinkOrderPosition(
                                isNew
                                        ? draftOrderPositionIds::add
                                        : id ->
                                                businessService.linkOrderPosition(
                                                        business.getId(), id))
                        .onUnlinkOrderPosition(
                                isNew
                                        ? draftOrderPositionIds::remove
                                        : id ->
                                                businessService.unlinkOrderPosition(
                                                        business.getId(), id))
                        .onLinkPurchasePosition(
                                isNew
                                        ? draftPurchasePositionIds::add
                                        : id ->
                                                businessService.linkPurchasePosition(
                                                        business.getId(), id))
                        .onUnlinkPurchasePosition(
                                isNew
                                        ? draftPurchasePositionIds::remove
                                        : id ->
                                                businessService.unlinkPurchasePosition(
                                                        business.getId(), id))
                        .viewKey("grid.business.linksTree")
                        .preferenceService(prefsService)
                        .build();
        card.add(tree);
        return card;
    }

    private static List<OrderPosition> draftOrderPositions(
            BusinessService businessService, Set<UUID> draftOrderPositionIds) {
        if (draftOrderPositionIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> ids = new HashSet<>(draftOrderPositionIds);
        return businessService.getAllOrderPositions().stream()
                .filter(p -> ids.contains(p.getId()))
                .toList();
    }

    private static List<PurchasePosition> draftPurchasePositions(
            BusinessService businessService, Set<UUID> draftPurchasePositionIds) {
        if (draftPurchasePositionIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> ids = new HashSet<>(draftPurchasePositionIds);
        return businessService.getAllPurchasePositions().stream()
                .filter(p -> ids.contains(p.getId()))
                .toList();
    }
}

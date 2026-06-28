package com.ordermgmt.railway.domain.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.business.model.BusinessStatus;
import com.ordermgmt.railway.domain.business.repository.BusinessRepository;
import com.ordermgmt.railway.domain.order.model.CoverageType;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.model.PurchasePosition;
import com.ordermgmt.railway.domain.order.model.ResourceNeed;
import com.ordermgmt.railway.domain.order.model.ResourceType;
import com.ordermgmt.railway.dto.business.BusinessListItem;
import com.ordermgmt.railway.dto.order.OrderListItem;

/**
 * Regression guard for the lazy list search queries (P3/P4) against a real Postgres (Testcontainers
 * — the bytea bug is Postgres-specific and would not reproduce on H2).
 *
 * <p>The key case is a {@code null} text/tags bind: without the {@code cast(:text as string)} in
 * {@code searchOrders}/{@code searchBusinesses}, Postgres types {@code concat('%', NULL, '%')} as
 * bytea and the query fails with "function lower(bytea) does not exist". Also covers the Slice
 * paging contract (fetch pageSize+1, {@code hasNext}) and a text filter.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LazyListSearchQueryTest {

    private static final Sort ORDER_SORT = Sort.by("orderNumber").and(Sort.by("id"));

    @Autowired private OrderRepository orderRepository;
    @Autowired private BusinessRepository businessRepository;

    @Test
    void searchOrders_nullTextAndTags_doesNotThrow_andReturnsAll() {
        persistOrder("P7-A", "Alpha");
        persistOrder("P7-B", "Bravo");
        persistOrder("P7-C", "Charlie");

        Slice<OrderListItem> slice =
                assertNoThrow(
                        () ->
                                orderRepository.searchOrders(
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        PageRequest.of(0, 10, ORDER_SORT)));

        assertThat(slice.getContent())
                .extracting(OrderListItem::orderNumber)
                .containsExactly("P7-A", "P7-B", "P7-C");
        assertThat(slice.hasNext()).isFalse();
    }

    @Test
    void searchOrders_textFilter_matchesOnlyThatOrder() {
        persistOrder("P7-A", "Alpha");
        persistOrder("P7-B", "Bravo");

        Slice<OrderListItem> slice =
                orderRepository.searchOrders(
                        "p7-b",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        PageRequest.of(0, 10, ORDER_SORT));

        assertThat(slice.getContent())
                .extracting(OrderListItem::orderNumber)
                .containsExactly("P7-B");
    }

    @Test
    void searchOrders_sliceFetchesPageSizePlusOne_andPagesAreDisjoint() {
        persistOrder("P7-A", "Alpha");
        persistOrder("P7-B", "Bravo");
        persistOrder("P7-C", "Charlie");

        Slice<OrderListItem> page0 =
                orderRepository.searchOrders(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        PageRequest.of(0, 2, ORDER_SORT));
        Slice<OrderListItem> page1 =
                orderRepository.searchOrders(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        PageRequest.of(1, 2, ORDER_SORT));

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.hasNext()).isTrue();
        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.hasNext()).isFalse();
        assertThat(page0.getContent())
                .extracting(OrderListItem::orderNumber)
                .doesNotContainAnyElementsOf(
                        page1.getContent().stream().map(OrderListItem::orderNumber).toList());
    }

    @Test
    void searchOrders_orderTypeFilter_partitionsByLeadTime() {
        // Ordered ~11 months ahead of the first operating day → annual (JAHRESBESTELLUNG).
        persistOrder(
                "ANN", "Annual", LocalDate.of(2026, 12, 13), Instant.parse("2026-01-10T12:00:00Z"));
        // Ordered within two months of the operating day → single (EINZELBESTELLUNG).
        persistOrder(
                "SNG", "Single", LocalDate.of(2026, 3, 1), Instant.parse("2026-02-20T12:00:00Z"));

        Slice<OrderListItem> annual =
                orderRepository.searchOrders(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "JAHRESBESTELLUNG",
                        false,
                        PageRequest.of(0, 10, ORDER_SORT));
        Slice<OrderListItem> single =
                orderRepository.searchOrders(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "EINZELBESTELLUNG",
                        false,
                        PageRequest.of(0, 10, ORDER_SORT));

        assertThat(annual.getContent())
                .extracting(OrderListItem::orderNumber)
                .containsExactly("ANN");
        assertThat(single.getContent())
                .extracting(OrderListItem::orderNumber)
                .containsExactly("SNG");
    }

    @Test
    void searchOrders_validityRange_filtersAndDoesNotThrow() {
        // Seed helper sets validTo = validFrom + 1 month.
        persistOrder(
                "EARLY",
                "Ends in Feb",
                LocalDate.of(2026, 1, 1),
                Instant.parse("2026-01-01T12:00:00Z")); // validTo 2026-02-01
        persistOrder(
                "LATE",
                "Ends in Jan 2027",
                LocalDate.of(2026, 12, 1),
                Instant.parse("2026-12-01T12:00:00Z")); // validTo 2027-01-01

        // validFromMin keeps orders whose validity ends on/after the bound. A bare `? is null` on
        // this LocalDate bind used to fail on Postgres with 42P18 ("could not determine data
        // type");
        // assertNoThrow guards that the cast(:validFromMin as LocalDate) fix holds.
        Slice<OrderListItem> fromMin =
                assertNoThrow(
                        () ->
                                orderRepository.searchOrders(
                                        null,
                                        null,
                                        null,
                                        LocalDate.of(2026, 6, 1),
                                        null,
                                        null,
                                        null,
                                        null,
                                        false,
                                        PageRequest.of(0, 10, ORDER_SORT)));
        assertThat(fromMin.getContent())
                .extracting(OrderListItem::orderNumber)
                .containsExactly("LATE");

        // validToMax keeps orders whose validity starts on/before the bound.
        Slice<OrderListItem> toMax =
                assertNoThrow(
                        () ->
                                orderRepository.searchOrders(
                                        null,
                                        null,
                                        null,
                                        null,
                                        LocalDate.of(2026, 6, 1),
                                        null,
                                        null,
                                        null,
                                        false,
                                        PageRequest.of(0, 10, ORDER_SORT)));
        assertThat(toMax.getContent())
                .extracting(OrderListItem::orderNumber)
                .containsExactly("EARLY");
    }

    @Test
    void searchOrders_incompleteOnly_keepsOrdersWithUnbookedPurchase() {
        Order complete = persistOrder("CMP", "Complete");
        addPurchasePosition(complete, "PP-CMP", "BOOKED");
        Order incomplete = persistOrder("INC", "Incomplete");
        addPurchasePosition(incomplete, "PP-INC", "DRAFT_OFFERED");

        Slice<OrderListItem> slice =
                orderRepository.searchOrders(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        PageRequest.of(0, 10, ORDER_SORT));

        assertThat(slice.getContent())
                .extracting(OrderListItem::orderNumber)
                .containsExactly("INC");
    }

    @Test
    void searchBusinesses_nullTextAndTags_doesNotThrow_andReturnsAll() {
        persistBusiness("P7 Biz One");
        persistBusiness("P7 Biz Two");

        Slice<BusinessListItem> slice =
                assertNoThrow(
                        () ->
                                businessRepository.searchBusinesses(
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        PageRequest.of(
                                                0, 10, Sort.by("title").and(Sort.by("id")))));

        assertThat(slice.getContent())
                .extracting(BusinessListItem::title)
                .containsExactly("P7 Biz One", "P7 Biz Two");
    }

    @Test
    void searchBusinesses_textFilter_matchesOnlyThatBusiness() {
        persistBusiness("P7 Biz One");
        persistBusiness("P7 Biz Two");

        Slice<BusinessListItem> slice =
                businessRepository.searchBusinesses(
                        "one",
                        null,
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 10, Sort.by("title").and(Sort.by("id"))));

        assertThat(slice.getContent())
                .extracting(BusinessListItem::title)
                .containsExactly("P7 Biz One");
    }

    // ─── seed helpers ──────────────────────────────────────────

    private Order persistOrder(String number, String name) {
        return persistOrder(
                number, name, LocalDate.of(2026, 3, 1), Instant.parse("2026-03-31T12:00:00Z"));
    }

    private Order persistOrder(String number, String name, LocalDate validFrom, Instant createdAt) {
        Order order = new Order();
        order.setOrderNumber(number);
        order.setName(name);
        order.setValidFrom(validFrom);
        order.setValidTo(validFrom.plusMonths(1));
        order.setProcessStatus(ProcessStatus.AUFTRAG);
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(createdAt);
        return orderRepository.saveAndFlush(order);
    }

    /** Attaches a position + need + one purchase position with the given TTT process state. */
    private void addPurchasePosition(Order order, String purchaseNumber, String tttState) {
        Instant now = Instant.parse("2026-03-31T12:00:00Z");
        OrderPosition position = new OrderPosition();
        position.setOrder(order);
        position.setName(purchaseNumber + "-pos");
        position.setType(PositionType.FAHRPLAN);
        position.setCreatedAt(now);
        position.setUpdatedAt(now);

        ResourceNeed need = new ResourceNeed();
        need.setOrderPosition(position);
        need.setResourceType(ResourceType.CAPACITY);
        need.setCoverageType(CoverageType.EXTERNAL);
        position.getResourceNeeds().add(need);

        PurchasePosition purchase = new PurchasePosition();
        purchase.setPositionNumber(purchaseNumber);
        purchase.setOrderPosition(position);
        purchase.setResourceNeed(need);
        purchase.setPmProcessState(tttState);
        position.getPurchasePositions().add(purchase);

        order.getPositions().add(position);
        orderRepository.saveAndFlush(order);
    }

    private void persistBusiness(String title) {
        Instant now = Instant.parse("2026-03-31T12:00:00Z");
        Business business = new Business();
        business.setTitle(title);
        business.setDescription("P7 test");
        business.setStatus(BusinessStatus.IN_BEARBEITUNG);
        business.setCreatedAt(now);
        business.setUpdatedAt(now);
        businessRepository.saveAndFlush(business);
    }

    private static <X> X assertNoThrow(java.util.function.Supplier<X> call) {
        assertThatCode(call::get).doesNotThrowAnyException();
        return call.get();
    }
}

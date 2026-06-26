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
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
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
                        null, null, null, null, null, null, null, PageRequest.of(0, 2, ORDER_SORT));
        Slice<OrderListItem> page1 =
                orderRepository.searchOrders(
                        null, null, null, null, null, null, null, PageRequest.of(1, 2, ORDER_SORT));

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

    private void persistOrder(String number, String name) {
        Instant now = Instant.parse("2026-03-31T12:00:00Z");
        Order order = new Order();
        order.setOrderNumber(number);
        order.setName(name);
        order.setValidFrom(LocalDate.of(2026, 3, 1));
        order.setValidTo(LocalDate.of(2026, 3, 31));
        order.setProcessStatus(ProcessStatus.AUFTRAG);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
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

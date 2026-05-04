package com.ordermgmt.railway.domain.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ordermgmt.railway.domain.business.model.Business;
import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;
import com.ordermgmt.railway.domain.order.repository.PurchasePositionRepository;

/**
 * Verifies the new-business save flow with linked order positions:
 * <ul>
 *   <li>{@link BusinessService#create(String, String, List, List)} persists links atomically</li>
 *   <li>{@link BusinessService#getLinkedOrderPositions(UUID)} returns a detached list whose
 *       lazy associations (e.g. {@code order.orderNumber}) can be read outside the tx</li>
 *   <li>{@link BusinessService#linkOrderPosition(UUID, UUID)} can add another OP afterwards</li>
 *   <li>{@link BusinessService#unlinkOrderPosition(UUID, UUID)} removes a single link</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BusinessService.class)
@DirtiesContext
class BusinessServiceLinkPositionsTest {

    @Autowired private BusinessService businessService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderPositionRepository orderPositionRepository;
    // Required by BusinessService constructor; not exercised here but Spring needs the bean.
    @Autowired private PurchasePositionRepository purchasePositionRepository;

    @Test
    void createsBusinessWithLinkedOrderPositions() {
        Order order = persistOrder("LINK-TEST-001", "Verknüpfungstest");
        OrderPosition op1 = persistOrderPosition(order, "Position A");
        OrderPosition op2 = persistOrderPosition(order, "Position B");

        Business saved = businessService.create("Mein Geschäft", "Beschreibung",
                List.of(op1.getId(), op2.getId()), List.of());

        assertThat(saved.getId()).isNotNull();

        List<OrderPosition> linked = businessService.getLinkedOrderPositions(saved.getId());
        assertThat(linked).extracting(OrderPosition::getId)
                .containsExactlyInAnyOrder(op1.getId(), op2.getId());

        // Reading lazy associations outside the service transaction must not throw.
        assertThatCode(() -> {
            for (OrderPosition op : linked) {
                assertThat(op.getName()).isNotBlank();
                assertThat(op.getOrder().getOrderNumber()).isEqualTo("LINK-TEST-001");
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void linkAndUnlinkSingleOrderPositionAfterCreate() {
        Order order = persistOrder("LINK-TEST-002", "Nachträglich");
        OrderPosition op1 = persistOrderPosition(order, "P1");
        OrderPosition op2 = persistOrderPosition(order, "P2");

        Business saved = businessService.create("Geschäft", "desc",
                List.of(op1.getId()), List.of());
        assertThat(businessService.getLinkedOrderPositions(saved.getId())).hasSize(1);

        businessService.linkOrderPosition(saved.getId(), op2.getId());
        assertThat(businessService.getLinkedOrderPositions(saved.getId()))
                .extracting(OrderPosition::getId)
                .containsExactlyInAnyOrder(op1.getId(), op2.getId());

        businessService.unlinkOrderPosition(saved.getId(), op1.getId());
        assertThat(businessService.getLinkedOrderPositions(saved.getId()))
                .extracting(OrderPosition::getId)
                .containsExactly(op2.getId());
    }

    @Test
    void getDocumentsReturnsDetachedListEvenForBusinessWithoutDocuments() {
        Business saved = businessService.create("Plain", "no docs", List.of(), List.of());
        // Must not throw LazyInitializationException when iterated outside the tx.
        assertThatCode(() -> businessService.getDocuments(saved.getId()).size())
                .doesNotThrowAnyException();
    }

    @Test
    void setAssigneeIsIdempotentWhenValuesUnchanged() {
        Business saved = businessService.create("Z", "z", List.of(), List.of());
        var v1 = businessService.setAssignee(saved.getId(),
                com.ordermgmt.railway.domain.business.model.AssignmentType.USER, "sebastian");
        Long version1 = v1.getVersion();
        // Calling again with the same values must not bump the @Version (no save).
        var v2 = businessService.setAssignee(saved.getId(),
                com.ordermgmt.railway.domain.business.model.AssignmentType.USER, "sebastian");
        assertThat(v2.getAssignmentName()).isEqualTo("sebastian");
        assertThat(v2.getAssignmentType()).isEqualTo("USER");
        assertThat(v2.getVersion()).isEqualTo(version1);
    }

    @Test
    void closedBusinessRejectsTransitions() {
        Business saved = businessService.create("Closed", "x", List.of(), List.of());
        // IN_BEARBEITUNG -> FREIGEGEBEN -> ABGESCHLOSSEN
        businessService.setStatus(saved.getId(),
                com.ordermgmt.railway.domain.business.model.BusinessStatus.FREIGEGEBEN);
        businessService.setStatus(saved.getId(),
                com.ordermgmt.railway.domain.business.model.BusinessStatus.ABGESCHLOSSEN);
        // From ABGESCHLOSSEN no further transition is allowed.
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                businessService.setStatus(saved.getId(),
                        com.ordermgmt.railway.domain.business.model.BusinessStatus.IN_BEARBEITUNG));
        // But assignee changes are still possible on closed businesses.
        var updated = businessService.setAssignee(saved.getId(),
                com.ordermgmt.railway.domain.business.model.AssignmentType.USER, "sebastian");
        assertThat(updated.getAssignmentName()).isEqualTo("sebastian");
    }

    // ─── helpers ─────────

    private Order persistOrder(String number, String name) {
        Instant now = Instant.parse("2026-05-04T10:00:00Z");
        Order order = new Order();
        order.setOrderNumber(number);
        order.setName(name);
        order.setValidFrom(LocalDate.of(2026, 5, 1));
        order.setValidTo(LocalDate.of(2026, 12, 31));
        order.setProcessStatus(ProcessStatus.AUFTRAG);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return orderRepository.saveAndFlush(order);
    }

    private OrderPosition persistOrderPosition(Order order, String name) {
        Instant now = Instant.parse("2026-05-04T10:00:00Z");
        OrderPosition op = new OrderPosition();
        op.setOrder(order);
        op.setName(name);
        op.setType(PositionType.LEISTUNG);
        op.setInternalStatus(PositionStatus.IN_BEARBEITUNG);
        op.setCreatedAt(now);
        op.setUpdatedAt(now);
        return orderPositionRepository.saveAndFlush(op);
    }
}

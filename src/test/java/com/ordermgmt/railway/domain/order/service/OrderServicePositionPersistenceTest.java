package com.ordermgmt.railway.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ordermgmt.railway.domain.order.model.Order;
import com.ordermgmt.railway.domain.order.model.OrderPosition;
import com.ordermgmt.railway.domain.order.model.PositionStatus;
import com.ordermgmt.railway.domain.order.model.PositionType;
import com.ordermgmt.railway.domain.order.model.ProcessStatus;
import com.ordermgmt.railway.domain.order.repository.OrderPositionRepository;
import com.ordermgmt.railway.domain.order.repository.OrderRepository;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderServicePositionPersistenceTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderPositionRepository positionRepository;

    @Test
    void savesServicePositionWithValidityCommentAndTags() {
        Instant now = Instant.parse("2026-03-31T12:00:00Z");

        Order order = new Order();
        order.setOrderNumber("TEST-LEISTUNG-001");
        order.setName("Persistenztest Leistung");
        order.setValidFrom(LocalDate.of(2026, 3, 1));
        order.setValidTo(LocalDate.of(2026, 3, 31));
        order.setProcessStatus(ProcessStatus.AUFTRAG);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order = orderRepository.saveAndFlush(order);

        OrderPosition position = new OrderPosition();
        position.setOrder(order);
        position.setName("Serviceposition");
        position.setType(PositionType.LEISTUNG);
        position.setServiceType("Rangierdienst");
        position.setFromLocation("Basel");
        position.setToLocation("Olten");
        position.setStart(LocalDateTime.of(2026, 3, 3, 0, 0));
        position.setEnd(LocalDateTime.of(2026, 3, 3, 23, 59));
        position.setValidity("[{\"startDate\":\"2026-03-03\",\"endDate\":\"2026-03-03\"}]");
        position.setComment("Kommentar");
        position.setTags("Intermodal");
        position.setInternalStatus(PositionStatus.IN_BEARBEITUNG);
        position.setCreatedAt(now);
        position.setUpdatedAt(now);

        OrderPosition saved = positionRepository.saveAndFlush(position);

        assertThat(saved.getId()).isNotNull();

        var persisted = positionRepository.findByOrderId(order.getId());
        assertThat(persisted).hasSize(1);
        assertThat(persisted.getFirst().getType()).isEqualTo(PositionType.LEISTUNG);
        assertThat(persisted.getFirst().getEnd()).isEqualTo(LocalDateTime.of(2026, 3, 3, 23, 59));
        assertThat(persisted.getFirst().getValidity())
                .isEqualTo("[{\"startDate\":\"2026-03-03\",\"endDate\":\"2026-03-03\"}]");
        assertThat(persisted.getFirst().getComment()).isEqualTo("Kommentar");
        assertThat(persisted.getFirst().getTags()).isEqualTo("Intermodal");
    }
}

package com.innowise.orderservice.repository;

import com.innowise.orderservice.config.JpaAuditingConfig;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.specification.OrderSpecification;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.rsocket.RSocketProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Схема создаётся настоящими миграциями Liquibase, а Hibernate её проверяет (ddl-auto=validate):
 * тест заодно ловит расхождения между entity и миграциями. Нужен запущенный Docker.
 */
@DataJpaTest(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfig.class)
class OrderRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void save_shouldFillAuditFieldsAndCascadeOrderItems() {
        Item item = itemRepository.save(newItem("Laptop", "100.00"));

        Order order = newOrder(10L, OrderStatus.CREATED);
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(2);
        order.addItem(orderItem);

        Order saved = orderRepository.saveAndFlush(order);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertNotNull(saved.getItems().get(0).getId());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "select count(*) from order_items where order_id = ?", Long.class, saved.getId()));
    }

    @Test
    void findByUserId_shouldReturnOnlyOrdersOfRequestedUser() {
        orderRepository.save(newOrder(10L, OrderStatus.CREATED));
        orderRepository.save(newOrder(20L, OrderStatus.PAID));
        orderRepository.flush();

        List<Order> result = orderRepository.findByUserId(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getUserId());
    }

    @Test
    void findAll_shouldFilterByCreatedFrom() {
        saveTwoOrders();
        Instant now = Instant.now();

        assertTrue(findPage(OrderSpecification.createdFrom(now.plus(1, ChronoUnit.HOURS))).isEmpty());
        assertEquals(2, findPage(OrderSpecification.createdFrom(now.minus(1, ChronoUnit.HOURS))).getTotalElements());
    }

    @Test
    void findAll_shouldFilterByCreatedTo() {
        saveTwoOrders();
        Instant now = Instant.now();

        assertTrue(findPage(OrderSpecification.createdTo(now.minus(1, ChronoUnit.HOURS))).isEmpty());
        assertEquals(2, findPage(OrderSpecification.createdTo(now.plus(1, ChronoUnit.HOURS))).getTotalElements());
    }

    @Test
    void findAll_shouldFilterByStatuses() {
        orderRepository.save(newOrder(10L, OrderStatus.CREATED));
        orderRepository.save(newOrder(20L, OrderStatus.PAID));
        orderRepository.save(newOrder(30L, OrderStatus.SHIPPED));
        orderRepository.flush();

        Page<Order> result = findPage(OrderSpecification.statusIn(List.of(OrderStatus.PAID, OrderStatus.SHIPPED)));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().noneMatch(o -> o.getStatus() == OrderStatus.CREATED));
    }

    @Test
    void findAll_shouldIgnoreEmptyFilters() {
        saveTwoOrders();

        Specification<Order> specification = Specification
                .where(OrderSpecification.createdFrom(null))
                .and(OrderSpecification.createdTo(null))
                .and(OrderSpecification.statusIn(null));

        assertEquals(2, findPage(specification).getTotalElements());
    }

    @Test
    void delete_shouldSoftDeleteOrderAndHideItFromQueries() {
        Order order = orderRepository.saveAndFlush(newOrder(10L, OrderStatus.CREATED));

        orderRepository.delete(order);
        orderRepository.flush();

        assertTrue(orderRepository.findAll().isEmpty());
        assertTrue(orderRepository.findByUserId(10L).isEmpty());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "select count(*) from orders where id = ? and deleted = true", Long.class, order.getId()));
    }

    private void saveTwoOrders() {
        orderRepository.save(newOrder(10L, OrderStatus.CREATED));
        orderRepository.save(newOrder(20L, OrderStatus.PAID));
        orderRepository.flush();
    }

    private Page<Order> findPage(Specification<Order> specification) {
        return orderRepository.findAll(specification, PageRequest.of(0, 10));
    }

    private Order newOrder(Long userId, OrderStatus status) {
        Order order = new Order();
        order.setUserId(userId);
        order.setUserEmail("user" + userId + "@example.com");
        order.setStatus(status);
        order.setTotalPrice(new BigDecimal("100.00"));
        return order;
    }

    private Item newItem(String name, String price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        return item;
    }
}

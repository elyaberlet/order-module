
package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.request.CreateOrderRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.dto.response.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderMapper = Mappers.getMapper(OrderMapper.class);
    }

    @Test
    void toEntity_shouldMapCreateOrderRequest() {
        CreateOrderRequest request = new CreateOrderRequest(
                "customer@example.com",
                List.of(new OrderItemRequest(1L, 2))
        );

        Order order = orderMapper.toEntity(request);

        assertNotNull(order);

        // Поле, которое MapStruct должен заполнить
        assertEquals("customer@example.com", order.getUserEmail());

        // Поля, которые не должны заполняться маппером
        assertNull(order.getId());
        assertNull(order.getUserId());

        // Должны сохраниться значения по умолчанию из Order
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(BigDecimal.ZERO, order.getTotalPrice());
        assertFalse(order.isDeleted());

        assertNotNull(order.getItems());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void toOrderItem_shouldMapRequestAndItem() {
        OrderItemRequest request = new OrderItemRequest(1L, 3);

        Item item = new Item();
        item.setId(1L);
        item.setName("Laptop");
        item.setPrice(new BigDecimal("100.00"));

        OrderItem orderItem = orderMapper.toOrderItem(request, item);

        assertNotNull(orderItem);
        assertSame(item, orderItem.getItem());
        assertEquals(3, orderItem.getQuantity());
        assertNull(orderItem.getId());
        assertNull(orderItem.getOrder());
    }

    @Test
    void updateEntity_shouldUpdateOnlyStatus() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(20L);
        order.setUserEmail("customer@example.com");
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(new BigDecimal("150.00"));

        UpdateOrderRequest request =
                new UpdateOrderRequest(OrderStatus.PAID);

        orderMapper.updateEntity(request, order);

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(10L, order.getId());
        assertEquals(20L, order.getUserId());
        assertEquals("customer@example.com", order.getUserEmail());
        assertEquals(new BigDecimal("150.00"), order.getTotalPrice());
    }

    @Test
    void toResponse_shouldMapOrderAndUser() {

        Item item = new Item();
        item.setId(1L);
        item.setName("Laptop");
        item.setPrice(new BigDecimal("100.00"));

        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setQuantity(2);

        Order order = new Order();
        order.setId(10L);
        order.setUserId(20L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(new BigDecimal("200.00"));
        order.setItems(List.of(orderItem));

        UserDto user = new UserDto(
                20L,
                "John",
                "Doe",
                "john@example.com"
        );

        OrderResponse response = orderMapper.toResponse(order, user);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(20L, response.userId());
        assertEquals(OrderStatus.CREATED, response.status());
        assertEquals(new BigDecimal("200.00"), response.totalPrice());
        assertSame(user, response.user());
        assertTrue(response.userInfoAvailable());

        assertNotNull(response.items());
        assertEquals(1, response.items().size());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals("Laptop", response.items().get(0).item().name());
    }

    @Test
    void toResponse_shouldMarkUserInfoUnavailableWhenUserIsNull() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(20L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(BigDecimal.ZERO);
        order.setItems(List.of());

        OrderResponse response = orderMapper.toResponse(order, null);

        assertNotNull(response);
        assertNull(response.user());
        assertFalse(response.userInfoAvailable());
    }
}

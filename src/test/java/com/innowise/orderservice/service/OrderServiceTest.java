package com.innowise.orderservice.service;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.request.CreateOrderRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.dto.response.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.exception.UserServiceUnavailableException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderService orderService;


    @Test
    void create_shouldCalculateTotalPriceAndSaveOrder() {
        // Arrange
        String email = "customer@example.com";

        UserDto user = new UserDto(
                10L,
                "John",
                "Doe",
                email
        );

        CreateOrderRequest request = new CreateOrderRequest(
                email,
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 3)
                )
        );

        Item item1 = new Item();
        item1.setId(1L);
        item1.setName("Keyboard");
        item1.setPrice(new BigDecimal("25.00"));

        Item item2 = new Item();
        item2.setId(2L);
        item2.setName("Mouse");
        item2.setPrice(new BigDecimal("10.00"));

        Order order = new Order();

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(userClient.getUserByEmail(email)).thenReturn(user);

        when(itemRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(item1, item2));

        when(orderMapper.toEntity(request)).thenReturn(order);

        when(orderMapper.toOrderItem(
                any(OrderItemRequest.class),
                any(Item.class)
        )).thenAnswer(invocation -> {
            OrderItemRequest line = invocation.getArgument(0);

            OrderItem orderItem = new OrderItem();
            orderItem.setQuantity(line.quantity());

            return orderItem;
        });

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderMapper.toResponse(any(Order.class), eq(user)))
                .thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.create(request);

        // Assert
        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();

        assertEquals(10L, savedOrder.getUserId());
        assertEquals(email, savedOrder.getUserEmail());
        assertEquals(
                new BigDecimal("80.00"),
                savedOrder.getTotalPrice()
        );

        assertEquals(2, savedOrder.getItems().size());

        assertEquals(2, savedOrder.getItems().get(0).getQuantity());
        assertEquals(3, savedOrder.getItems().get(1).getQuantity());

        assertNotSame(
                savedOrder.getItems().get(0),
                savedOrder.getItems().get(1)
        );

        assertSame(expectedResponse, result);

        verify(userClient).getUserByEmail(email);
        verify(itemRepository).findAllById(List.of(1L, 2L));
        verify(orderMapper, times(2))
                .toOrderItem(any(OrderItemRequest.class), any(Item.class));
    }


    @Test
    void create_shouldThrowExceptionWhenItemDoesNotExist() {
        // Arrange
        String email = "customer@example.com";

        UserDto user = new UserDto(
                10L,
                "John",
                "Doe",
                email
        );

        CreateOrderRequest request = new CreateOrderRequest(
                email,
                List.of(new OrderItemRequest(999L, 1))
        );

        when(userClient.getUserByEmail(email)).thenReturn(user);

        when(itemRepository.findAllById(List.of(999L)))
                .thenReturn(List.of());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.create(request)
        );

        verify(userClient).getUserByEmail(email);
        verify(itemRepository).findAllById(List.of(999L));
        verify(orderRepository, never()).save(any(Order.class));
    }


    @Test
    void create_shouldThrowExceptionWhenUserServiceReturnsNull() {
        // Arrange
        String email = "customer@example.com";

        CreateOrderRequest request = new CreateOrderRequest(
                email,
                List.of(new OrderItemRequest(1L, 1))
        );

        when(userClient.getUserByEmail(email)).thenReturn(null);

        // Act & Assert
        assertThrows(
                UserServiceUnavailableException.class,
                () -> orderService.create(request)
        );

        verify(userClient).getUserByEmail(email);
        verify(itemRepository, never()).findAllById(any());
        verify(orderRepository, never()).save(any(Order.class));
    }


    @Test
    void create_shouldMergeDuplicateItems() {
        // Arrange
        String email = "customer@example.com";

        UserDto user = new UserDto(
                10L,
                "John",
                "Doe",
                email
        );

        CreateOrderRequest request = new CreateOrderRequest(
                email,
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(1L, 3)
                )
        );

        Item item = new Item();
        item.setId(1L);
        item.setName("Keyboard");
        item.setPrice(new BigDecimal("25.00"));

        when(userClient.getUserByEmail(email)).thenReturn(user);

        when(itemRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(item));

        when(orderMapper.toEntity(request)).thenReturn(new Order());

        when(orderMapper.toOrderItem(any(OrderItemRequest.class), eq(item)))
                .thenAnswer(invocation -> {
                    OrderItemRequest line = invocation.getArgument(0);

                    OrderItem orderItem = new OrderItem();
                    orderItem.setQuantity(line.quantity());

                    return orderItem;
                });

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderMapper.toResponse(any(Order.class), eq(user)))
                .thenReturn(mock(OrderResponse.class));

        // Act
        orderService.create(request);

        // Assert
        ArgumentCaptor<Order> captor =
                ArgumentCaptor.forClass(Order.class);

        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();

        assertEquals(1, savedOrder.getItems().size());
        assertEquals(5, savedOrder.getItems().get(0).getQuantity());

        assertEquals(
                new BigDecimal("125.00"),
                savedOrder.getTotalPrice()
        );

        verify(itemRepository).findAllById(List.of(1L));
        verify(orderMapper, times(1))
                .toOrderItem(any(OrderItemRequest.class), eq(item));
    }


    @Test
    void getById_shouldReturnOrderWhenOrderExists() {
        // Arrange
        Long orderId = 1L;
        String email = "customer@example.com";

        Order order = new Order();
        order.setId(orderId);
        order.setUserEmail(email);

        UserDto user = new UserDto(
                10L,
                "John",
                "Doe",
                email
        );

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(userClient.getUserByEmail(email))
                .thenReturn(user);

        when(orderMapper.toResponse(order, user))
                .thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.getById(orderId);

        // Assert
        assertSame(expectedResponse, result);

        verify(orderRepository).findById(orderId);
        verify(userClient).getUserByEmail(email);
        verify(orderMapper).toResponse(order, user);
    }


    @Test
    void getById_shouldThrowExceptionWhenOrderDoesNotExist() {
        // Arrange
        Long orderId = 999L;

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getById(orderId)
        );

        verify(orderRepository).findById(orderId);
        verifyNoInteractions(userClient);
        verifyNoInteractions(orderMapper);
    }


    @Test
    void getByUserId_shouldReturnUserOrdersAndCacheUserLookup() {
        // Arrange
        Long userId = 10L;
        String email = "customer@example.com";

        Order firstOrder = new Order();
        firstOrder.setId(1L);
        firstOrder.setUserId(userId);
        firstOrder.setUserEmail(email);

        Order secondOrder = new Order();
        secondOrder.setId(2L);
        secondOrder.setUserId(userId);
        secondOrder.setUserEmail(email);

        UserDto user = new UserDto(
                userId,
                "John",
                "Doe",
                email
        );

        OrderResponse firstResponse = mock(OrderResponse.class);
        OrderResponse secondResponse = mock(OrderResponse.class);

        when(orderRepository.findByUserId(userId))
                .thenReturn(List.of(firstOrder, secondOrder));

        when(userClient.getUserByEmail(email))
                .thenReturn(user);

        when(orderMapper.toResponse(firstOrder, user))
                .thenReturn(firstResponse);

        when(orderMapper.toResponse(secondOrder, user))
                .thenReturn(secondResponse);

        // Act
        List<OrderResponse> result = orderService.getByUserId(userId);

        // Assert
        assertEquals(2, result.size());
        assertSame(firstResponse, result.get(0));
        assertSame(secondResponse, result.get(1));

        verify(orderRepository).findByUserId(userId);

        // User Service is called only once for the same email.
        verify(userClient, times(1)).getUserByEmail(email);

        verify(orderMapper).toResponse(firstOrder, user);
        verify(orderMapper).toResponse(secondOrder, user);
    }


    @Test
    void getAll_shouldReturnPagedOrders() {
        // Arrange
        String email = "customer@example.com";

        Order order = new Order();
        order.setId(1L);
        order.setUserId(10L);
        order.setUserEmail(email);

        UserDto user = new UserDto(
                10L,
                "John",
                "Doe",
                email
        );

        OrderResponse expectedResponse = mock(OrderResponse.class);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(
                ArgumentMatchers.<Specification<Order>>any(),
                eq(pageable)
        )).thenReturn(orderPage);

        when(userClient.getUserByEmail(email))
                .thenReturn(user);

        when(orderMapper.toResponse(order, user))
                .thenReturn(expectedResponse);

        // Act
        Page<OrderResponse> result = orderService.getAll(
                null,
                null,
                List.of(),
                pageable
        );

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertSame(expectedResponse, result.getContent().get(0));

        verify(orderRepository).findAll(
                ArgumentMatchers.<Specification<Order>>any(),
                eq(pageable)
        );

        verify(userClient).getUserByEmail(email);
        verify(orderMapper).toResponse(order, user);
    }


    @Test
    void update_shouldUpdateExistingOrder() {
        // Arrange
        Long orderId = 1L;
        String email = "customer@example.com";

        Order order = new Order();
        order.setId(orderId);
        order.setUserId(10L);
        order.setUserEmail(email);

        UpdateOrderRequest request = mock(UpdateOrderRequest.class);
        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(userClient.getUserByEmail(email))
                .thenReturn(null);

        when(orderMapper.toResponse(
                eq(order),
                ArgumentMatchers.<UserDto>isNull()
        )).thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.update(orderId, request);

        // Assert
        assertSame(expectedResponse, result);

        verify(orderRepository).findById(orderId);
        verify(orderMapper).updateEntity(request, order);
        verify(userClient).getUserByEmail(email);

        verify(orderMapper).toResponse(
                eq(order),
                ArgumentMatchers.<UserDto>isNull()
        );
    }


    @Test
    void delete_shouldDeleteExistingOrder() {
        // Arrange
        Long orderId = 1L;

        Order order = new Order();
        order.setId(orderId);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        // Act
        orderService.delete(orderId);

        // Assert
        verify(orderRepository).findById(orderId);
        verify(orderRepository).delete(order);
    }


    @Test
    void delete_shouldThrowExceptionWhenOrderDoesNotExist() {
        // Arrange
        Long orderId = 999L;

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.delete(orderId)
        );

        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).delete(any(Order.class));
    }


    @Test
    void update_shouldThrowExceptionWhenOrderDoesNotExist() {
        // Arrange
        Long orderId = 999L;

        UpdateOrderRequest request = mock(UpdateOrderRequest.class);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.update(orderId, request)
        );

        verify(orderRepository).findById(orderId);
        verifyNoInteractions(orderMapper);
        verifyNoInteractions(userClient);
    }


    @Test
    void getById_shouldPropagateExceptionWhenUserServiceIsUnavailable() {
        // Arrange
        Long orderId = 1L;
        String email = "customer@example.com";

        Order order = new Order();
        order.setId(orderId);
        order.setUserEmail(email);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(userClient.getUserByEmail(email))
                .thenThrow(new UserServiceUnavailableException(
                        "User Service is currently unavailable",
                        (Throwable) null
                ));

        // Act & Assert
        assertThrows(
                UserServiceUnavailableException.class,
                () -> orderService.getById(orderId)
        );

        verify(orderRepository).findById(orderId);
        verify(userClient).getUserByEmail(email);
        verifyNoInteractions(orderMapper);
    }


    @Test
    void getById_shouldReturnOrderWhenUserDoesNotExist() {
        // Arrange
        Long orderId = 1L;
        String email = "missing@example.com";

        Order order = new Order();
        order.setId(orderId);
        order.setUserEmail(email);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(userClient.getUserByEmail(email))
                .thenThrow(new ResourceNotFoundException("User not found"));

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(orderMapper.toResponse(
                eq(order),
                ArgumentMatchers.<UserDto>isNull()
        )).thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.getById(orderId);

        // Assert
        assertSame(expectedResponse, result);

        verify(orderRepository).findById(orderId);
        verify(userClient).getUserByEmail(email);

        verify(orderMapper).toResponse(
                eq(order),
                ArgumentMatchers.<UserDto>isNull()
        );
    }

    @Test
    void create_shouldThrowExceptionWhenSomeItemsDoNotExist() {
        String email = "customer@example.com";

        UserDto user = new UserDto(
                10L,
                "John",
                "Doe",
                email
        );

        CreateOrderRequest request = new CreateOrderRequest(
                email,
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                )
        );

        Item existingItem = new Item();
        existingItem.setId(1L);
        existingItem.setName("Keyboard");
        existingItem.setPrice(new BigDecimal("25.00"));

        when(userClient.getUserByEmail(email)).thenReturn(user);

        when(itemRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(existingItem));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.create(request)
        );

        assertEquals("Items not found: [2]", exception.getMessage());

        verify(userClient).getUserByEmail(email);
        verify(itemRepository).findAllById(List.of(1L, 2L));
        verifyNoInteractions(orderMapper);
        verify(orderRepository, never()).save(any(Order.class));
    }
}

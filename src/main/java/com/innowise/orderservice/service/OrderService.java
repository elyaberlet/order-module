package com.innowise.orderservice.service;

import com.innowise.orderservice.client.UserClient;
import com.innowise.orderservice.dto.request.CreateOrderRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.dto.response.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.exception.UserServiceUnavailableException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.specification.OrderSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ItemRepository itemRepository;
    private final UserClient userClient;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        UserDto user = userClient.getUserByEmail(request.email());
        if (user == null) {
            throw new UserServiceUnavailableException("User Service is currently unavailable");
        }

        List<OrderItemRequest> lines = mergeDuplicateLines(request.items());
        Map<Long, Item> itemsById = loadItems(lines);

        Order order = orderMapper.toEntity(request);
        order.setUserId(user.id());
        order.setUserEmail(user.email());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest line : lines) {
            Item item = itemsById.get(line.itemId());
            order.addItem(orderMapper.toOrderItem(line, item));
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }
        order.setTotalPrice(total);

        return orderMapper.toResponse(orderRepository.save(order), user);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        return toResponse(findOrderById(id), new HashMap<>());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getByUserId(Long userId) {
        Map<String, Optional<UserDto>> users = new HashMap<>();
        return orderRepository.findByUserId(userId).stream()
                .map(order -> toResponse(order, users))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAll(
            Instant createdFrom,
            Instant createdTo,
            List<OrderStatus> statuses,
            Pageable pageable
    ) {
        Specification<Order> specification = Specification
                .where(OrderSpecification.createdFrom(createdFrom))
                .and(OrderSpecification.createdTo(createdTo))
                .and(OrderSpecification.statusIn(statuses));

        Map<String, Optional<UserDto>> users = new HashMap<>();
        return orderRepository.findAll(specification, pageable)
                .map(order -> toResponse(order, users));
    }

    @Transactional
    public OrderResponse update(Long id, UpdateOrderRequest request) {
        Order order = findOrderById(id);
        orderMapper.updateEntity(request, order);
        orderRepository.flush();
        return toResponse(order, new HashMap<>());
    }

    @Transactional
    public void delete(Long id) {
        orderRepository.delete(findOrderById(id));
    }

    private OrderResponse toResponse(Order order, Map<String, Optional<UserDto>> users) {
        UserDto user = users
                .computeIfAbsent(order.getUserEmail(), this::resolveUser)
                .orElse(null);
        return orderMapper.toResponse(order, user);
    }

    private Optional<UserDto> resolveUser(String email) {
        try {
            return Optional.ofNullable(userClient.getUserByEmail(email));
        } catch (ResourceNotFoundException exception) {
            return Optional.empty();
        }
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + id + " not found"));
    }

    private List<OrderItemRequest> mergeDuplicateLines(List<OrderItemRequest> lines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest line : lines) {
            quantities.merge(line.itemId(), line.quantity(), Integer::sum);
        }
        return quantities.entrySet().stream()
                .map(entry -> new OrderItemRequest(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<Long, Item> loadItems(List<OrderItemRequest> lines) {
        List<Long> ids = lines.stream().map(OrderItemRequest::itemId).toList();

        Map<Long, Item> found = new HashMap<>();
        for (Item item : itemRepository.findAllById(ids)) {
            found.put(item.getId(), item);
        }

        List<Long> missing = ids.stream().filter(id -> !found.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Items not found: " + missing);
        }
        return found;
    }
}

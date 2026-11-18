
package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.request.CreateOrderRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.ItemDto;
import com.innowise.orderservice.dto.response.OrderItemDto;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.dto.response.UserDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userEmail", source = "email")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "items", ignore = true)
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", source = "item")
    @Mapping(target = "quantity", source = "request.quantity")
    OrderItem toOrderItem(OrderItemRequest request, Item item);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "status", source = "status")
    void updateEntity(
            UpdateOrderRequest request,
            @MappingTarget Order order
    );

    ItemDto toItemDto(Item item);

    OrderItemDto toOrderItemDto(OrderItem orderItem);

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "userId", source = "order.userId")
    @Mapping(target = "status", source = "order.status")
    @Mapping(target = "totalPrice", source = "order.totalPrice")
    @Mapping(target = "createdAt", source = "order.createdAt")
    @Mapping(target = "updatedAt", source = "order.updatedAt")
    @Mapping(target = "items", source = "order.items")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "userInfoAvailable", expression = "java(user != null)")
    OrderResponse toResponse(Order order, UserDto user);
}

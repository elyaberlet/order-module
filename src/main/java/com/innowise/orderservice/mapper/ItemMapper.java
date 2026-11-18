package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.request.ItemRequest;
import com.innowise.orderservice.dto.response.ItemDto;
import com.innowise.orderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    @Mapping(target = "id", ignore = true)
    Item toEntity(ItemRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(ItemRequest request, @MappingTarget Item item);

    ItemDto toDto(Item item);
}

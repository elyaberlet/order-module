package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.request.ItemRequest;
import com.innowise.orderservice.dto.response.ItemDto;
import com.innowise.orderservice.entity.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ItemMapperTest {

    private ItemMapper itemMapper;

    @BeforeEach
    void setUp() {
        itemMapper = Mappers.getMapper(ItemMapper.class);
    }

    @Test
    void toEntity_shouldMapItemRequest() {
        ItemRequest request = new ItemRequest(
                "Keyboard",
                new BigDecimal("50.00")
        );

        Item item = itemMapper.toEntity(request);

        assertNotNull(item);
        assertEquals("Keyboard", item.getName());
        assertEquals(new BigDecimal("50.00"), item.getPrice());
        assertNull(item.getId());
    }

    @Test
    void updateEntity_shouldUpdateItemFieldsWithoutChangingId() {
        Item item = new Item();
        item.setId(1L);
        item.setName("Old Keyboard");
        item.setPrice(new BigDecimal("40.00"));

        ItemRequest request = new ItemRequest(
                "New Keyboard",
                new BigDecimal("60.00")
        );

        itemMapper.updateEntity(request, item);

        assertEquals(1L, item.getId());
        assertEquals("New Keyboard", item.getName());
        assertEquals(new BigDecimal("60.00"), item.getPrice());
    }

    @Test
    void toDto_shouldMapItem() {
        Item item = new Item();
        item.setId(1L);
        item.setName("Keyboard");
        item.setPrice(new BigDecimal("50.00"));

        ItemDto dto = itemMapper.toDto(item);

        assertNotNull(dto);
        assertEquals(1L, dto.id());
        assertEquals("Keyboard", dto.name());
        assertEquals(new BigDecimal("50.00"), dto.price());
    }
}

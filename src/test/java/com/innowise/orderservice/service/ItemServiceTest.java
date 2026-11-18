package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.request.ItemRequest;
import com.innowise.orderservice.dto.response.ItemDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void create_shouldSaveItemAndReturnDto() {
        ItemRequest request = mock(ItemRequest.class);
        Item item = mock(Item.class);
        Item savedItem = mock(Item.class);
        ItemDto expectedDto = mock(ItemDto.class);

        when(itemMapper.toEntity(request)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(savedItem);
        when(itemMapper.toDto(savedItem)).thenReturn(expectedDto);

        ItemDto result = itemService.create(request);

        assertSame(expectedDto, result);
        verify(itemMapper).toEntity(request);
        verify(itemRepository).save(item);
        verify(itemMapper).toDto(savedItem);
    }

    @Test
    void getAll_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Item item = mock(Item.class);
        ItemDto expectedDto = mock(ItemDto.class);

        Page<Item> itemPage = new PageImpl<>(List.of(item));

        when(itemRepository.findAll(pageable)).thenReturn(itemPage);
        when(itemMapper.toDto(item)).thenReturn(expectedDto);

        Page<ItemDto> result = itemService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(expectedDto, result.getContent().get(0));
        verify(itemRepository).findAll(pageable);
        verify(itemMapper).toDto(item);
    }

    @Test
    void update_shouldUpdateExistingItemAndReturnDto() {
        Long itemId = 1L;
        ItemRequest request = mock(ItemRequest.class);
        Item item = mock(Item.class);
        ItemDto expectedDto = mock(ItemDto.class);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemMapper.toDto(item)).thenReturn(expectedDto);

        ItemDto result = itemService.update(itemId, request);

        assertSame(expectedDto, result);
        verify(itemRepository).findById(itemId);
        verify(itemMapper).updateEntity(request, item);
        verify(itemMapper).toDto(item);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteExistingItemAndFlush() {
        Long itemId = 1L;
        Item item = mock(Item.class);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        itemService.delete(itemId);

        verify(itemRepository).findById(itemId);
        verify(itemRepository).delete(item);
        verify(itemRepository).flush();
    }

    @Test
    void update_shouldThrowExceptionWhenItemNotFound() {
        Long itemId = 999L;
        ItemRequest request = mock(ItemRequest.class);

        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> itemService.update(itemId, request)
        );

        verify(itemMapper, never()).updateEntity(any(), any());
    }

    @Test
    void delete_shouldThrowExceptionWhenItemNotFound() {
        Long itemId = 999L;

        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> itemService.delete(itemId)
        );

        verify(itemRepository, never()).delete(any());
        verify(itemRepository, never()).flush();
    }
}

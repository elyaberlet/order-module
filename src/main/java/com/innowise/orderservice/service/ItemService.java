package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.request.ItemRequest;
import com.innowise.orderservice.dto.response.ItemDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Transactional
    public ItemDto create(ItemRequest request) {
        Item item = itemRepository.save(itemMapper.toEntity(request));
        return itemMapper.toDto(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemDto> getAll(Pageable pageable) {
        return itemRepository.findAll(pageable).map(itemMapper::toDto);
    }

    @Transactional
    public ItemDto update(Long id, ItemRequest request) {
        Item item = findById(id);
        itemMapper.updateEntity(request, item);
        return itemMapper.toDto(item);
    }

    @Transactional
    public void delete(Long id) {
        Item item = findById(id);
        itemRepository.delete(item);
        itemRepository.flush();
    }

    private Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item " + id + " not found"));
    }
}

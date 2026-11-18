package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.request.ItemRequest;
import com.innowise.orderservice.dto.response.ItemDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Transactional
    public ItemDto create(ItemRequest request) {
        log.info("Creating item: {}", request.name());

        Item item = itemRepository.save(itemMapper.toEntity(request));

        log.info("Item created successfully with id: {}", item.getId());
        return itemMapper.toDto(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemDto> getAll(Pageable pageable) {
        log.debug("Fetching items, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return itemRepository.findAll(pageable).map(itemMapper::toDto);
    }

    @Transactional
    public ItemDto update(Long id, ItemRequest request) {
        log.info("Updating item with id: {}", id);

        Item item = findById(id);
        itemMapper.updateEntity(request, item);

        log.info("Item updated successfully with id: {}", id);
        return itemMapper.toDto(item);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting item with id: {}", id);

        Item item = findById(id);
        itemRepository.delete(item);
        itemRepository.flush();

        log.info("Item deleted successfully with id: {}", id);
    }

    private Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Item with id {} not found", id);
                    return new ResourceNotFoundException("Item " + id + " not found");
                });
    }
}

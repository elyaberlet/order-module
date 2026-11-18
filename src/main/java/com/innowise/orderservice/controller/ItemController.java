package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.request.ItemRequest;
import com.innowise.orderservice.dto.response.ItemDto;
import com.innowise.orderservice.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemDto> create(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(request));
    }

    @GetMapping
    public ResponseEntity<PagedModel<ItemDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(itemService.getAll(pageable)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ItemRequest request
    ) {
        return ResponseEntity.ok(itemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

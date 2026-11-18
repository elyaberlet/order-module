package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.dto.request.ItemRequest;
import com.innowise.orderservice.dto.response.ItemDto;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void create_shouldReturnCreatedItem() throws Exception {
        ItemRequest request = new ItemRequest("Keyboard", new BigDecimal("50.00"));
        when(itemService.create(any(ItemRequest.class)))
                .thenReturn(new ItemDto(1L, "Keyboard", new BigDecimal("50.00")));

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(50.00));

        verify(itemService).create(any(ItemRequest.class));
    }

    @Test
    void getAll_shouldReturnPagedItems() throws Exception {
        ItemDto item = new ItemDto(1L, "Keyboard", new BigDecimal("50.00"));
        when(itemService.getAll(any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/items").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Keyboard"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void update_shouldReturnUpdatedItem() throws Exception {
        ItemRequest request = new ItemRequest("Updated Keyboard", new BigDecimal("60.00"));
        when(itemService.update(eq(1L), any(ItemRequest.class)))
                .thenReturn(new ItemDto(1L, "Updated Keyboard", new BigDecimal("60.00")));

        mockMvc.perform(put("/api/items/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Keyboard"))
                .andExpect(jsonPath("$.price").value(60.00));
    }

    @Test
    void update_shouldReturnNotFoundWhenItemDoesNotExist() throws Exception {
        ItemRequest request = new ItemRequest("Keyboard", new BigDecimal("60.00"));
        when(itemService.update(eq(999L), any(ItemRequest.class)))
                .thenThrow(new ResourceNotFoundException("Item 999 not found"));

        mockMvc.perform(put("/api/items/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item 999 not found"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/items/{id}", 1L))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(itemService).delete(1L);
    }

    @Test
    void delete_shouldReturnConflictWhenItemIsUsedInOrders() throws Exception {
        doThrow(new DataIntegrityViolationException("fk_order_items_item"))
                .when(itemService).delete(1L);

        mockMvc.perform(delete("/api/items/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void create_shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        ItemRequest request = new ItemRequest("", new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemService);
    }

    @Test
    void update_shouldReturnBadRequestWhenRequestIsInvalid() throws Exception {
        ItemRequest request = new ItemRequest("", new BigDecimal("-10.00"));

        mockMvc.perform(put("/api/items/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(itemService);
    }
}

package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.dto.request.CreateOrderRequest;
import com.innowise.orderservice.dto.request.OrderItemRequest;
import com.innowise.orderservice.dto.request.UpdateOrderRequest;
import com.innowise.orderservice.dto.response.OrderResponse;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.exception.UserServiceUnavailableException;
import com.innowise.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderResponse orderResponse(OrderStatus status) {
        return new OrderResponse(1L, 10L, status, new BigDecimal("200.00"),
                null, null, List.of(), null, false);
    }

    private CreateOrderRequest validCreateRequest() {
        return new CreateOrderRequest("customer@example.com", List.of(new OrderItemRequest(1L, 2)));
    }

    // ---------- create ----------

    @Test
    void create_shouldReturnCreated() throws Exception {
        when(orderService.create(any(CreateOrderRequest.class)))
                .thenReturn(orderResponse(OrderStatus.CREATED));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void create_shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "invalid-email", List.of(new OrderItemRequest(1L, 2)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void create_shouldReturnBadRequestWhenItemsAreEmpty() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("customer@example.com", List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void create_shouldReturnBadRequestWhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request body"));

        verifyNoInteractions(orderService);
    }

    @Test
    void create_shouldReturnServiceUnavailableWhenUserServiceIsDown() throws Exception {
        when(orderService.create(any(CreateOrderRequest.class)))
                .thenThrow(new UserServiceUnavailableException("User Service is currently unavailable"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    // ---------- read ----------

    @Test
    void getById_shouldReturnOrder() throws Exception {
        when(orderService.getById(1L)).thenReturn(orderResponse(OrderStatus.CREATED));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getById_shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.getById(999L)).thenThrow(new ResourceNotFoundException("Order 999 not found"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order 999 not found"));
    }

    @Test
    void getByUserId_shouldReturnOrders() throws Exception {
        when(orderService.getByUserId(10L)).thenReturn(List.of(orderResponse(OrderStatus.CREATED)));

        mockMvc.perform(get("/api/orders/user/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(10));
    }

    @Test
    void getAll_shouldReturnPagedOrders() throws Exception {
        when(orderService.getAll(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderResponse(OrderStatus.CREATED)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/orders").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void getAll_shouldPassFiltersToService() throws Exception {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");
        List<OrderStatus> statuses = List.of(OrderStatus.CREATED, OrderStatus.PAID);

        when(orderService.getAll(eq(from), eq(to), eq(statuses), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderResponse(OrderStatus.PAID)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/orders")
                        .param("createdFrom", "2026-01-01T00:00:00Z")
                        .param("createdTo", "2026-01-31T23:59:59Z")
                        .param("statuses", "CREATED", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PAID"));

        verify(orderService).getAll(eq(from), eq(to), eq(statuses), any(Pageable.class));
    }

    @Test
    void getAll_shouldReturnBadRequestWhenDateIsInvalid() throws Exception {
        mockMvc.perform(get("/api/orders").param("createdFrom", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: createdFrom"));

        verifyNoInteractions(orderService);
    }

    @Test
    void getAll_shouldReturnBadRequestWhenStatusIsUnknown() throws Exception {
        mockMvc.perform(get("/api/orders").param("statuses", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: statuses"));

        verifyNoInteractions(orderService);
    }

    // ---------- update ----------

    @Test
    void update_shouldReturnUpdatedOrder() throws Exception {
        when(orderService.update(eq(1L), any(UpdateOrderRequest.class)))
                .thenReturn(orderResponse(OrderStatus.PAID));

        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderRequest(OrderStatus.PAID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void update_shouldReturnBadRequestWhenStatusIsNull() throws Exception {
        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderRequest(null))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void update_shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderService.update(eq(999L), any(UpdateOrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("Order 999 not found"));

        mockMvc.perform(put("/api/orders/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderRequest(OrderStatus.PAID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order 999 not found"));
    }

    // ---------- delete ----------

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(orderService).delete(1L);
    }

    @Test
    void delete_shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Order 999 not found")).when(orderService).delete(999L);

        mockMvc.perform(delete("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order 999 not found"));
    }

    @Test
    void unsupportedHttpMethod_shouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(patch("/api/orders/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}

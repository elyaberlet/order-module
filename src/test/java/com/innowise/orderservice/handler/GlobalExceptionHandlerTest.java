package com.innowise.orderservice.handler;

import com.innowise.orderservice.dto.response.ErrorResponse;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.exception.UserServiceUnavailableException;
import com.innowise.orderservice.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_shouldReturn404() {
        assertError(handler.handleResourceNotFound(new ResourceNotFoundException("Item not found")),
                HttpStatus.NOT_FOUND, "Item not found");
    }

    @Test
    void handleUserServiceUnavailable_shouldReturn503() {
        assertError(handler.handleUserServiceUnavailable(new UserServiceUnavailableException("User Service unavailable")),
                HttpStatus.SERVICE_UNAVAILABLE, "User Service unavailable");
    }

    @Test
    void handleValidation_shouldReturn400WithAllFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "item");
        bindingResult.addError(new FieldError("item", "name", "must not be blank"));
        bindingResult.addError(new FieldError("item", "price", "must not be negative"));

        assertError(handler.handleValidation(new MethodArgumentNotValidException(null, bindingResult)),
                HttpStatus.BAD_REQUEST, "name: must not be blank; price: must not be negative");
    }

    @Test
    void handleTypeMismatch_shouldReturn400() {
        var exception = new MethodArgumentTypeMismatchException("abc", Long.class, "status", null, null);

        assertError(handler.handleTypeMismatch(exception),
                HttpStatus.BAD_REQUEST, "Invalid value for parameter: status");
    }

    @Test
    void handleMissingParameter_shouldReturn400() {
        var exception = new MissingServletRequestParameterException("userId", "Long");

        assertError(handler.handleMissingParameter(exception),
                HttpStatus.BAD_REQUEST, "Missing parameter: userId");
    }

    @Test
    void handleInvalidSort_shouldReturn400() {
        var exception = new PropertyReferenceException("unknownField", TypeInformation.of(Order.class), List.of());

        assertError(handler.handleInvalidSort(exception),
                HttpStatus.BAD_REQUEST, "Invalid sort property: unknownField");
    }

    @Test
    void handleDataIntegrity_shouldReturn409() {
        assertError(handler.handleDataIntegrity(new DataIntegrityViolationException("fk violation")),
                HttpStatus.CONFLICT, "Operation violates data integrity (for example, the item is used in orders)");
    }

    @Test
    void handleInvalidBody_shouldReturn400() {
        assertError(handler.handleInvalidBody(mock(HttpMessageNotReadableException.class)),
                HttpStatus.BAD_REQUEST, "Invalid request body");
    }

    @Test
    void handleUnexpected_shouldReturn500() {
        assertError(handler.handleUnexpected(new RuntimeException("boom")),
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @Test
    void handleUnexpected_shouldKeepStatusOfSpringMvcExceptions() {
        assertError(handler.handleUnexpected(new HttpRequestMethodNotSupportedException("PATCH")),
                HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");

        assertError(handler.handleUnexpected(new NoResourceFoundException(HttpMethod.GET, "/unknown")),
                HttpStatus.NOT_FOUND, "Not Found");
    }

    private void assertError(ResponseEntity<ErrorResponse> response, HttpStatus expectedStatus, String expectedMessage) {
        assertEquals(expectedStatus, response.getStatusCode());

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(expectedStatus.value(), body.status());
        assertEquals(expectedMessage, body.message());
        assertNotNull(body.timestamp());
    }
}

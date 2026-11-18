package com.innowise.orderservice.client;

import com.innowise.orderservice.dto.response.UserDto;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@SuppressWarnings({"unchecked", "rawtypes"})
class UserClientTest {

    private static final String EMAIL = "customer@example.com";
    private static final String USER_URL_PREFIX = "http://user-service:8081/api/users/email/";
    private static final String USER_JSON =
            "{\"id\":10,\"name\":\"John\",\"surname\":\"Doe\",\"email\":\"" + EMAIL + "\"}";

    private MockRestServiceServer server;
    private CircuitBreaker circuitBreaker;
    private ServiceTokenProvider tokenProvider;
    private UserClient userClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service:8081");
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        CircuitBreakerFactory circuitBreakerFactory = mock(CircuitBreakerFactory.class);
        circuitBreaker = mock(CircuitBreaker.class);
        tokenProvider = mock(ServiceTokenProvider.class);

        when(circuitBreakerFactory.create("userService")).thenReturn(circuitBreaker);
        passThroughCircuitBreaker();

        userClient = new UserClient(restClient, circuitBreakerFactory, tokenProvider);
    }

    @Test
    void getUserByEmail_shouldReturnUserAndSendServiceToken() {
        when(tokenProvider.get()).thenReturn("service-token");
        server.expect(once(), requestTo(startsWith(USER_URL_PREFIX)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token"))
                .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));

        UserDto result = userClient.getUserByEmail(EMAIL);

        assertEquals(new UserDto(10L, "John", "Doe", EMAIL), result);
        server.verify();
    }

    @Test
    void getUserByEmail_shouldThrowResourceNotFoundWhenUserDoesNotExist() {
        when(tokenProvider.get()).thenReturn("service-token");
        server.expect(once(), requestTo(startsWith(USER_URL_PREFIX)))
                .andRespond(withStatus(NOT_FOUND));

        assertThrows(ResourceNotFoundException.class, () -> userClient.getUserByEmail(EMAIL));

        verify(tokenProvider, never()).invalidate();
    }

    @Test
    void getUserByEmail_shouldRefreshTokenAndRetryAfterUnauthorized() {
        when(tokenProvider.get()).thenReturn("old-token", "new-token");
        server.expect(once(), requestTo(startsWith(USER_URL_PREFIX)))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer old-token"))
                .andRespond(withUnauthorizedRequest());
        server.expect(once(), requestTo(startsWith(USER_URL_PREFIX)))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer new-token"))
                .andRespond(withSuccess(USER_JSON, MediaType.APPLICATION_JSON));

        UserDto result = userClient.getUserByEmail(EMAIL);

        assertEquals(10L, result.id());
        verify(tokenProvider).invalidate();
        verify(tokenProvider, times(2)).get();
        server.verify();
    }

    @Test
    void getUserByEmail_shouldReturnNullWhenRetryAlsoReturnsUnauthorized() {
        when(tokenProvider.get()).thenReturn("old-token", "new-token");
        server.expect(ExpectedCount.times(2), requestTo(startsWith(USER_URL_PREFIX)))
                .andRespond(withUnauthorizedRequest());

        assertNull(userClient.getUserByEmail(EMAIL));

        verify(tokenProvider, times(1)).invalidate();
        verify(tokenProvider, times(2)).get();
        server.verify();
    }

    @Test
    void getUserByEmail_shouldReturnNullWhenUserServiceFails() {
        when(tokenProvider.get()).thenReturn("service-token");
        server.expect(once(), requestTo(startsWith(USER_URL_PREFIX)))
                .andRespond(withServerError());

        assertNull(userClient.getUserByEmail(EMAIL));
    }

    @Test
    void getUserByEmail_shouldReturnNullWithoutHttpCallWhenCircuitIsOpen() {
        reset(circuitBreaker);
        doAnswer(invocation -> {
            Function<Throwable, ?> fallback = invocation.getArgument(1);
            return fallback.apply(new RuntimeException("circuit is open"));
        }).when(circuitBreaker).run(any(Supplier.class), any(Function.class));

        assertNull(userClient.getUserByEmail(EMAIL));

        server.verify(); // ни одного HTTP-запроса не ожидалось и не было
    }

    /** Имитация circuit breaker: выполняет вызов, а при исключении вызывает fallback. */
    private void passThroughCircuitBreaker() {
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            Function<Throwable, ?> fallback = invocation.getArgument(1);
            try {
                return supplier.get();
            } catch (Throwable exception) {
                return fallback.apply(exception);
            }
        }).when(circuitBreaker).run(any(Supplier.class), any(Function.class));
    }
}

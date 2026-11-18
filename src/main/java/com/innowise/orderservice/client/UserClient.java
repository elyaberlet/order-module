package com.innowise.orderservice.client;

import com.innowise.orderservice.dto.response.UserDto;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient restClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final ServiceTokenProvider tokenProvider;

    public UserClient(
            RestClient restClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            ServiceTokenProvider tokenProvider
    ) {
        this.restClient = restClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.tokenProvider = tokenProvider;
    }

    public UserDto getUserByEmail(String email) {
        Optional<UserDto> result = circuitBreakerFactory.create("userService").run(
                () -> fetch(email, false),
                this::fallback
        );

        if (result == null) {
            return null;
        }
        return result.orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Optional<UserDto> fetch(String email, boolean retried) {
        String token = tokenProvider.get();
        try {
            return Optional.ofNullable(
                    restClient.get()
                            .uri("/api/users/email/{email}", email)
                            .headers(headers -> headers.setBearerAuth(token))
                            .retrieve()
                            .body(UserDto.class));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (HttpClientErrorException.Unauthorized exception) {
            if (retried) {
                throw exception;
            }
            tokenProvider.invalidate();
            return fetch(email, true);
        }
    }

    private Optional<UserDto> fallback(Throwable throwable) {
        log.warn("User Service call failed: {}", throwable.toString());
        return null;
    }
}

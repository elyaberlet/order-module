package com.innowise.orderservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;

import java.time.Duration;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerConfigurationTest {

    @Captor
    private ArgumentCaptor<Consumer<Resilience4JConfigBuilder>> configurerCaptor;

    @Test
    void customizer_shouldConfigureUserServiceCircuitBreakerAndTimeout() {
        Resilience4JCircuitBreakerFactory factory = mock(Resilience4JCircuitBreakerFactory.class);

        new CircuitBreakerConfiguration()
                .userServiceCircuitBreakerCustomizer()
                .customize(factory);

        verify(factory).configure(configurerCaptor.capture(), eq("userService"));

        Resilience4JConfigBuilder builder = new Resilience4JConfigBuilder("userService");
        configurerCaptor.getValue().accept(builder);
        var configuration = builder.build();

        assertEquals(Duration.ofSeconds(5), configuration.getTimeLimiterConfig().getTimeoutDuration());
        assertEquals(5, configuration.getCircuitBreakerConfig().getSlidingWindowSize());
        assertEquals(5, configuration.getCircuitBreakerConfig().getMinimumNumberOfCalls());
        assertEquals(50f, configuration.getCircuitBreakerConfig().getFailureRateThreshold());
    }
}

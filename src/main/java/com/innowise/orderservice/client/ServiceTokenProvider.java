package com.innowise.orderservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class ServiceTokenProvider {

    private final RestClient authClient;
    private final String username;
    private final String password;
    private volatile String token;

    public ServiceTokenProvider(
            RestClient.Builder builder,
            @Value("${auth-service.url}") String authServiceUrl,
            @Value("${service-account.username}") String username,
            @Value("${service-account.password}") String password
    ) {
        this.authClient = builder.baseUrl(authServiceUrl).build();
        this.username = username;
        this.password = password;
    }

    public String get() {
        String current = token;
        return current != null ? current : refresh();
    }

    public synchronized String refresh() {
        TokenResponse response = authClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Auth Service returned no access token");
        }
        token = response.accessToken();
        return token;
    }

    public void invalidate() {
        token = null;
    }

    private record TokenResponse(String accessToken) {
    }
}

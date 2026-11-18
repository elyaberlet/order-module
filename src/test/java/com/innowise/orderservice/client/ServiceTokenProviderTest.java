package com.innowise.orderservice.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class ServiceTokenProviderTest {

    private static final String LOGIN_URL = "http://auth-service:8082/api/auth/login";

    private MockRestServiceServer server;
    private ServiceTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        tokenProvider = new ServiceTokenProvider(
                builder,
                "http://auth-service:8082",
                "service-user",
                "service-password"
        );
    }

    @Test
    void get_shouldLoginOnceAndCacheToken() {
        expectLogin("access-token");

        assertEquals("access-token", tokenProvider.get());
        assertEquals("access-token", tokenProvider.get());

        server.verify();
    }

    @Test
    void get_shouldSendServiceAccountCredentials() {
        server.expect(once(), requestTo(LOGIN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("service-user"))
                .andExpect(jsonPath("$.password").value("service-password"))
                .andRespond(withSuccess(tokenJson("access-token"), MediaType.APPLICATION_JSON));

        tokenProvider.get();

        server.verify();
    }

    @Test
    void refresh_shouldReplaceCachedToken() {
        expectLogin("first-token");
        expectLogin("second-token");

        assertEquals("first-token", tokenProvider.get());
        assertEquals("second-token", tokenProvider.refresh());
        assertEquals("second-token", tokenProvider.get());

        server.verify();
    }

    @Test
    void invalidate_shouldForceNewLoginOnNextGet() {
        expectLogin("first-token");
        expectLogin("second-token");

        assertEquals("first-token", tokenProvider.get());
        tokenProvider.invalidate();
        assertEquals("second-token", tokenProvider.get());

        server.verify();
    }

    @Test
    void refresh_shouldThrowWhenResponseHasNoAccessToken() {
        server.expect(once(), requestTo(LOGIN_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> tokenProvider.refresh());
    }

    @Test
    void refresh_shouldPropagateAuthenticationFailure() {
        server.expect(once(), requestTo(LOGIN_URL))
                .andRespond(withUnauthorizedRequest());

        assertThrows(HttpClientErrorException.class, () -> tokenProvider.refresh());
    }

    private void expectLogin(String accessToken) {
        server.expect(once(), requestTo(LOGIN_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenJson(accessToken), MediaType.APPLICATION_JSON));
    }

    private String tokenJson(String accessToken) {
        return "{\"accessToken\":\"" + accessToken + "\",\"refreshToken\":\"refresh\",\"tokenType\":\"Bearer\"}";
    }
}

package com.innowise.orderservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientConfigTest {

    @Test
    void restClient_shouldUseConfiguredBaseUrl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        RestClient client = new RestClientConfig().restClient(builder, "http://localhost:8081");

        server.expect(requestTo("http://localhost:8081/api/ping"))
                .andRespond(withSuccess("pong", MediaType.TEXT_PLAIN));

        assertEquals("pong", client.get().uri("/api/ping").retrieve().body(String.class));
        server.verify();
    }
}

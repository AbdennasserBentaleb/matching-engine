package com.portfolio.matchingengine;

import com.portfolio.matchingengine.dto.OrderRequestDTO;
import com.portfolio.matchingengine.model.Side;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MatchingEngineIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testEndToEndOrderMatching() {
        OrderRequestDTO sellRequest = new OrderRequestDTO("META", Side.SELL, new BigDecimal("500.00"), new BigDecimal("10"));
        ResponseEntity<List> sellResponse = restTemplate.postForEntity("/api/v1/orders", sellRequest, List.class);
        assertEquals(HttpStatus.OK, sellResponse.getStatusCode());
        assertEquals(0, sellResponse.getBody().size(), "Sell order should rest on the book");

        OrderRequestDTO buyRequest = new OrderRequestDTO("META", Side.BUY, new BigDecimal("500.00"), new BigDecimal("10"));
        ResponseEntity<List> buyResponse = restTemplate.postForEntity("/api/v1/orders", buyRequest, List.class);
        assertEquals(HttpStatus.OK, buyResponse.getStatusCode());
        assertEquals(1, buyResponse.getBody().size(), "Buy order should immediately aggregate into 1 trade against the resting sell");
    }
}

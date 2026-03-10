package io.ownera.ledger.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractHealthEndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthShouldReturnOk() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/health", String.class);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("OK", resp.getBody());
    }

    @Test
    void livenessShouldReturnOk() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/health/liveness", String.class);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("OK", resp.getBody());
    }

    @Test
    void readinessShouldReturnOkWhenReady() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/health/readiness", String.class);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("OK", resp.getBody());
    }
}

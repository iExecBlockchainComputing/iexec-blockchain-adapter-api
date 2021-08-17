package com.iexec.blockchain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class IntegrationTests {

    public static final String USER = "admin";
    public static final String PASSWORD = "whatever";
    public static final String BASE_URL = "http://localhost:13010";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void getMetrics() {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(BASE_URL + "/metrics");
        ResponseEntity<String> responseEntity =
                this.restTemplate.exchange(uri.toUriString(), HttpMethod.GET, getRequest(), String.class);
        System.out.println("Metrics response code: " + responseEntity.getStatusCode());
        System.out.println("Metrics response body: " + responseEntity.getBody());
        Assertions.assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
        Assertions.assertNotNull(responseEntity.getBody());
        Assertions.assertFalse(responseEntity.getBody().isEmpty());
    }

    /**
     * TODO when match order is ready
     */
    @Test
    public void initialize() {
        initialize("0x06d2fc195ea17e059a176e329ff63bb7f23e5392daded776b9070f127ebd63a8", 0);
    }

    private ResponseEntity<String> initialize(String dealId, int taskIndex) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(BASE_URL + "/tasks/initialize")
                .queryParam("chainDealId", dealId)
                .queryParam("taskIndex", taskIndex);
        ResponseEntity<String> responseEntity =
                this.restTemplate.postForEntity(uri.toUriString(), getRequest(), String.class);
        System.out.println("Initialize response code: " + responseEntity.getStatusCode());
        System.out.println("Initialize response body: " + responseEntity.getBody());
        return responseEntity;
    }

    private HttpEntity<Object> getRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(USER, PASSWORD);
        return new HttpEntity<>(headers);
    }

}

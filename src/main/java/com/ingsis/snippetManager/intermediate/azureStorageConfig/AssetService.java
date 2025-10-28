package com.ingsis.snippetManager.intermediate.azureStorageConfig;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Objects;
import java.util.UUID;

@Component
public class AssetService {

    private final RestTemplate restTemplate;
    private final String bucketUrl;
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Autowired
    public AssetService(@Value("${bucket.url}") String bucketUrl) {
        this.restTemplate = new RestTemplate();
        this.bucketUrl = bucketUrl;
    }

    public ResponseEntity<String> saveSnippet(UUID snippetId, String content) {
        try {
            HttpEntity<String> request = new HttpEntity<>(content, getHeaders());
            restTemplate.put(bucketUrl + "/" + snippetId.toString(), request);
            return ResponseEntity.ok("Snippet saved successfully.");
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving snippet: " + e.getMessage());
        }
    }

    public ResponseEntity<String> updateSnippet(UUID snippetId, String content) {
        try {
            HttpEntity<String> request = new HttpEntity<>(content, getHeaders());
            restTemplate.put(bucketUrl + "/" + snippetId.toString(), request);
            return ResponseEntity.ok("Snippet updated successfully.");
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating snippet: " + e.getMessage());
        }
    }

    public ResponseEntity<String> getSnippet(UUID snippetId) {
        try {
            return restTemplate.getForEntity(bucketUrl + "/" + snippetId.toString(), String.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    public ResponseEntity<String> deleteSnippet(UUID snippetId) {
        try {
            restTemplate.delete(bucketUrl + "/" + snippetId.toString());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (Objects.nonNull(correlationId)) {
            headers.set("X-Correlation-Id", correlationId);
        }
        return headers;
    }
}
package com.ingsis.snippetManager.authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthenticationService {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthenticationService(@Value("${auth.service.base-url}") String authServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.authServiceUrl = authServiceUrl;
    }

    public String getUserNameById(String userId, Jwt jwt) {
        try {
            String url = authServiceUrl + "/users/" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public boolean userExists(String userId, Jwt jwt) {
        try {
            String url = authServiceUrl + "/users/exists/" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Boolean> response = restTemplate.exchange(url, HttpMethod.GET, request, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error checking user existence for " + userId + ": " + e.getMessage(), e);
        }
    }
}

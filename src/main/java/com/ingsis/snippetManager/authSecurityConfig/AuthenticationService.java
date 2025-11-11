package com.ingsis.snippetManager.authSecurityConfig;

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

    public AuthenticationService(@Value("http://localhost:8089/") String authServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.authServiceUrl = authServiceUrl;
    }

    public String getUserNameById(String userId, Jwt jwt) {
        try {
            String url = authServiceUrl + "/api/find?userID=" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );
            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new RuntimeException("user with ID " + userId + " name not defined or not assigned");
            }
            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Error getting user name from AuthService: " + e.getMessage(), e);
        }
    }

    public boolean userExists(String userId, Jwt jwt) {
        try {
            String url = authServiceUrl + "/api/users/exists/" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Boolean.class
            );

            return response.getBody() != null && response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Error verifying the user " + userId + ": " + e.getMessage(), e);
        }
    }
}

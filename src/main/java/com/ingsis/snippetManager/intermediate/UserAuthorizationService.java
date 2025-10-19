package com.ingsis.snippetManager.intermediate;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserAuthorizationService {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public UserAuthorizationService(@Value("http://localhost:8081/") String authServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.authServiceUrl = authServiceUrl;
    }

    public boolean createUser(String userId, UUID roleId) {
        try {
            ResponseEntity<Boolean> response =
                    restTemplate.getForEntity(
                            authServiceUrl + "/users?roleId=" + roleId, Boolean.class, userId);
            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            return false;
        }
    }
}

package com.ingsis.snippetManager.intermediate.permissions;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class UserPermissionService {

    private final RestTemplate restTemplate;
    private final String authorizationServiceUrl;

    public UserPermissionService(@Value("http://localhost:8086/") String authServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.authorizationServiceUrl = authServiceUrl;
    }

    public ResponseEntity<String> createUser(String userId, AuthorizationActions user_action, UUID snippetId) {
        try {
            AuthorizationActions action = AuthorizationActions.valueOf(user_action.name());
            CreatePermission createPermission = new CreatePermission(userId, snippetId, action);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreatePermission> request = new HttpEntity<>(createPermission, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(authorizationServiceUrl + "/permissions",
                    request, String.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    public List<UUID> getUserSnippets(String userId, AuthorizationActions action) {
        try {
            FilterDTO permissionDTO = new FilterDTO(action);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<FilterDTO> request = new HttpEntity<>(permissionDTO, headers);

            ResponseEntity<List<UUID>> response = restTemplate.exchange(
                    authorizationServiceUrl + "/permissions/getSnippets?userId=" + userId, HttpMethod.POST, request,
                    new ParameterizedTypeReference<List<UUID>>() {
                    });

            return response.getBody();
        } catch (Exception e) {
            return List.of();
        }
    }

    public boolean updateUserAuthorization(String userId, AuthorizationActions action, UUID snippetId) {
        try {
            CreatePermission updatePermission = new CreatePermission(userId, snippetId,
                    AuthorizationActions.valueOf(action.name()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CreatePermission> request = new HttpEntity<>(updatePermission, headers);

            ResponseEntity<String> response = restTemplate
                    .postForEntity(authorizationServiceUrl + "/permissions/update", request, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public ResponseEntity<String> deleteUserAuthorization(String userId, UUID snippetId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UUID> request = new HttpEntity<>(snippetId, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    authorizationServiceUrl + "/permissions/delete?userId=" + userId, HttpMethod.DELETE, request,
                    String.class);

            return ResponseEntity.ok().body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    public ResponseEntity<String> deleteSnippetUserAuthorization(UUID snippetId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UUID> request = new HttpEntity<>(snippetId, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    authorizationServiceUrl + "/permissions?snippetId=" + snippetId, HttpMethod.DELETE, request,
                    String.class);

            return ResponseEntity.ok().body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    public String getUserIdBySnippetId(UUID snippetId) {
        try {
            String url = authorizationServiceUrl + "/permissions?snippetId=" + snippetId;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new RuntimeException("Not userId found for snippetId: " + snippetId);
            }
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Error getting userId: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error getting userId for SnippetId: " + e.getMessage(), e);
        }
    }
}

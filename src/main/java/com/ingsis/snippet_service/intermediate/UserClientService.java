package com.ingsis.snippet_service.intermediate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserClientService {

  private final RestTemplate restTemplate;
  private final String authServiceUrl;

  public UserClientService(@Value("${auth.service.url}") String authServiceUrl) {
    this.restTemplate = new RestTemplate();
    this.authServiceUrl = authServiceUrl;
  }

  public boolean userExists(String userId) {
    try {
      ResponseEntity<Boolean> response =
          restTemplate.getForEntity(
              authServiceUrl + "/users/exists/{userId}", Boolean.class, userId);
      return response.getBody() != null && response.getBody();
    } catch (Exception e) {
      return false;
    }
  }
}

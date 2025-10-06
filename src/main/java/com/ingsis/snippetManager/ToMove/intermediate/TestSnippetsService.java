package com.ingsis.snippetManager.ToMove.intermediate;

import com.ingsis.snippetManager.ToMove.snippet.dto.TestDTO;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TestSnippetsService {

  private final RestTemplate restTemplate;
  private final String testingServiceUrl;

  public TestSnippetsService(@Value("http://localhost:8082/") String testingServiceUrl) {
    this.restTemplate = new RestTemplate();
    this.testingServiceUrl = testingServiceUrl;
  }

  public boolean createTest(String userId, TestDTO testDTO) {
    try {
      String url = testingServiceUrl + "/tests?userId=" + userId;
      ResponseEntity<Boolean> response = restTemplate.postForEntity(url, testDTO, Boolean.class);
      return response.getBody() != null && response.getBody();
    } catch (Exception e) {
      return false;
    }
  }

  public Map<UUID, String> getTestBySnippetId(UUID snippetId) {
    try {
      String url = testingServiceUrl + "/tests?snippetId=" + snippetId;
      ResponseEntity<Map<UUID, String>> response =
          restTemplate.exchange(
              url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<UUID, String>>() {});
      return response.getBody();
    } catch (Exception e) {
      throw new RuntimeException(
          "Error fetching tests for snippet " + snippetId + ": " + e.getMessage(), e);
    }
  }
}

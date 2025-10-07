package com.ingsis.snippetManager.ToMove.intermediate;

import com.ingsis.snippetManager.ToMove.snippet.dto.TestDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

  public void streamTestExecution(UUID snippetId, UUID testId, String userId, SseEmitter emitter) {
    String url = String.format("%s/tests/run/stream?snippetId=%s&testId=%s&userId=%s",
            testingServiceUrl, snippetId, testId, userId);

    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new URL(url).openStream()))) {

      String line;
      while ((line = reader.readLine()) != null) {
        emitter.send(SseEmitter.event().data(line));
      }
    } catch (Exception e) {
      throw new RuntimeException("Error streaming test output", e);
    } finally {
      emitter.complete();
    }
  }
}

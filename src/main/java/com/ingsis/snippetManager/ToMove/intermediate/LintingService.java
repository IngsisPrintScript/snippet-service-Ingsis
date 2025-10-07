package com.ingsis.snippetManager.ToMove.intermediate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingsis.snippetManager.ToMove.snippet.dto.FormatDTO;
import com.ingsis.snippetManager.ToMove.snippet.dto.LintingDTO;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LintingService {

  private final RestTemplate restTemplate;
  private final String lintingServiceUrl;

  public LintingService(@Value("http://localhost:8085/") String testingServiceUrl) {
    this.restTemplate = new RestTemplate();
    this.lintingServiceUrl = testingServiceUrl;
  }

  public boolean createLinting(String userId, LintingDTO lintingDTO) {
    try {
      String url = lintingServiceUrl + "/linting?userId=" + userId;
      ResponseEntity<Boolean> response = restTemplate.postForEntity(url, lintingDTO, Boolean.class);
      return response.getBody() != null && response.getBody();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean validLinting(UUID lintingId, String contentUrl) {
    try {
      URL content = new URL(contentUrl);
      BufferedReader br =
          new BufferedReader(new InputStreamReader(content.openStream(), StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      String code = sb.toString();
      String url = lintingServiceUrl + "/linting/" + lintingId;
      ResponseEntity<Boolean> response = restTemplate.postForEntity(url, code, Boolean.class);
      return response.getBody() != null && response.getBody();
    } catch (Exception e) {
      return false;
    }
  }

  public List<String> evaluate(String contentUrl) {
    try {
      URL content = new URL(contentUrl);
      StringBuilder sb = new StringBuilder();

      try (BufferedReader br =
          new BufferedReader(new InputStreamReader(content.openStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line).append("\n");
        }
      }

      String code = sb.toString();
      String url = lintingServiceUrl + "/linting/analyze";

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      Map<String, String> requestBody = Map.of("code", code);
      HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode jsonNode = mapper.readTree(response.getBody());

      List<String> failed = new ArrayList<>();
      if (jsonNode.has("errors")) {
        for (JsonNode err : jsonNode.get("errors")) {
          failed.add(err.asText());
        }
      }
      return failed;
    } catch (Exception e) {
      throw new RuntimeException("Error evaluating snippet: " + e.getMessage(), e);
    }
  }

  public boolean updateLintingRules(String userId, LintingDTO rulesDTO) {
    try {
      String url = lintingServiceUrl + "/rules?userId=" + userId;
      ResponseEntity<Boolean> response =
              restTemplate.postForEntity(url, rulesDTO, Boolean.class);
      return Boolean.TRUE.equals(response.getBody());
    } catch (Exception e) {
      return false;
    }
  }
}

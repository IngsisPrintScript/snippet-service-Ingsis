package com.ingsis.snippetManager.ToMove.intermediate;

import com.ingsis.snippetManager.ToMove.snippet.dto.FormatDTO;
import com.ingsis.snippetManager.snippet.Snippet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FormatService {

  private final RestTemplate restTemplate;
  private final String formatServiceUrl;

  public FormatService(@Value("http://localhost:8084/") String testingServiceUrl) {
    this.restTemplate = new RestTemplate();
    this.formatServiceUrl = testingServiceUrl;
  }

  public boolean createFormat(String userId, FormatDTO formatDTO) {
    try {
      String url = formatServiceUrl + "/format?userId=" + userId;
      ResponseEntity<Boolean> response = restTemplate.postForEntity(url, formatDTO, Boolean.class);
      return response.getBody() != null && response.getBody();
    } catch (Exception e) {
      return false;
    }
  }

  public Snippet formatSnippet(UUID snippetId) {
    String url = formatServiceUrl + "/format/" + snippetId;
    ResponseEntity<Snippet> response = restTemplate.postForEntity(url, null, Snippet.class);
    return response.getBody();
  }


  public boolean updateFormatRules(String userId, FormatDTO rulesDTO) {
    try {
      String url = formatServiceUrl + "/rules?userId=" + userId;
      ResponseEntity<Boolean> response =
              restTemplate.postForEntity(url, rulesDTO, Boolean.class);
      return Boolean.TRUE.equals(response.getBody());
    } catch (Exception e) {
      return false;
    }
  }
}

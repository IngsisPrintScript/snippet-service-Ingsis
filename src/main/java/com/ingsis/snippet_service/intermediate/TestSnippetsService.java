package com.ingsis.snippet_service.intermediate;

import com.ingsis.snippet_service.snippet.dto.TestDTO;
import org.springframework.beans.factory.annotation.Value;
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
            ResponseEntity<Boolean> response =
                    restTemplate.postForEntity(url, testDTO, Boolean.class);
            return response.getBody() != null && response.getBody();
        } catch (Exception e) {
            return false;
        }
    }
}

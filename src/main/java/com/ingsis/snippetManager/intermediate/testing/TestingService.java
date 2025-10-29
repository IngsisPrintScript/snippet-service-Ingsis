package com.ingsis.snippetManager.intermediate.testing;

import com.azure.core.exception.ResourceNotFoundException;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.redis.testing.TestRequestProducer;
import com.ingsis.snippetManager.redis.testing.dto.TestRequestEvent;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.testing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TestingService {

    private final RestTemplate restTemplate;
    private final String testingServiceUrl;
    private final SnippetRepo snippetRepo;
    private final AssetService assetService;
    private final TestRequestProducer testRequestProducer;
    private static final Logger logger = LoggerFactory.getLogger(TestingService.class);


    public TestingService(@Value("http://localhost:8084/test") String testingServiceUrl, SnippetRepo snippetRepo, AssetService assetService, TestRequestProducer testRequestProducer) {
        this.restTemplate = new RestTemplate();
        this.testingServiceUrl = testingServiceUrl;
        this.snippetRepo = snippetRepo;
        this.assetService = assetService;
        this.testRequestProducer = testRequestProducer;
    }

    public ResponseEntity<GetTestDTO> createTest(String userId, CreateTestDTO createDTO) {
        try {
            logger.info("Creating linting rules for user {}", userId);
            String url = testingServiceUrl + "/create?userId=" + userId;
            logger.info("Creating at url: {}", url);
            return restTemplate.postForEntity(url, createDTO, GetTestDTO.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<?> updateTest(String userId, UpdateTestDTO rulesDTO) {
        logger.info("Updating linting rules for user {}", userId);
        String url = testingServiceUrl + "/update?userId=" + userId;
        logger.info("Updating at url: {}", url);
        restTemplate.put(url, rulesDTO);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<List<GetTestDTO>> getTestsBySnippetIdAndTestOwner(String userId, UUID snippetId) {
        try {
            logger.info("Fetching tests for snippet {} and user {}", snippetId, userId);
            String url = testingServiceUrl + "/getBySnippet?userId=" + userId + "&snippetId=" + snippetId;
            logger.info("GET request to {}", url);

            ResponseEntity<List<GetTestDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<GetTestDTO>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            } else {
                logger.warn("No tests found for snippet {} and user {}", snippetId, userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }
        } catch (Exception e) {
            logger.error("Error fetching tests for snippet {} and user {}: {}", snippetId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    public ResponseEntity<List<GetTestDTO>> getTestsBySnippetId(UUID snippetId) {
        try {
            logger.info("Fetching tests for snippet {}", snippetId);
            String url = testingServiceUrl + "/getSnippetTests?snippetId=" + snippetId;
            logger.info("GET request to {}", url);

            ResponseEntity<List<GetTestDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<GetTestDTO>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return ResponseEntity.ok(response.getBody());
            } else {
                logger.warn("No tests found for snippet {}", snippetId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }
        } catch (Exception e) {
            logger.error("Error fetching tests for snippet {}: {}", snippetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    public ResponseEntity<String> deleteTest(String userId, UUID snippetId) {
        try {
            logger.info("Deleting test {} for user {}", snippetId, userId);
            String url = testingServiceUrl + "/delete?userId=" + userId + "&testId=" + snippetId;
            restTemplate.delete(url);
            return ResponseEntity.ok("Test " + snippetId + " deleted successfully.");
        } catch (ResourceNotFoundException e) {
            logger.error("Test {} not found for user {}", snippetId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Test not found: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting test {} for user {}: {}", snippetId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting test: " + e.getMessage());
        }
    }

    public ResponseEntity<String> deleteParticularTest(String userId, UUID testId) {
        try {
            logger.info("Deleting test {} for user {}", testId, userId);
            String url = testingServiceUrl + "?userId=" + userId + "&testId=" + testId;
            restTemplate.delete(url);
            return ResponseEntity.ok("Test " + testId + " deleted successfully.");
        } catch (ResourceNotFoundException e) {
            logger.error("Test {} not found for user {}", testId, userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Test not found: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting test {} for user {}: {}", testId, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting test: " + e.getMessage());
        }
    }

    public void runAllTestsForSnippet(String userId, UUID snippetId) {
        try {
            logger.info("Running all tests for snippet {}", snippetId);
            Snippet snippet = snippetRepo.findById(snippetId).orElseThrow();
            List<UUID> testsId = snippet.getTestId();
            for (UUID test : testsId) {
                TestToRunDTO dto = new TestToRunDTO(test);
                runTestCase(userId, dto, snippetId);
                logger.info("Test {} sent", dto.testCaseId());
            }
        } catch (Exception e) {
            logger.error("Error running tests automatically for snippet {}: {}", snippetId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void runTestCase(String userId, TestToRunDTO dto, UUID snippetId) {
        logger.info("Testing {} case for user {}", dto.testCaseId(), userId);
        Snippet snippet = snippetRepo.findById(snippetId).orElseThrow(NoSuchElementException::new);
        String content = assetService.getSnippet(snippet.getId()).getBody();
        TestRequestEvent event = new TestRequestEvent(
                userId,
                snippetId,
                snippet.getLanguage(),
                content
        );
        testRequestProducer.publish(event);
    }

    public void runParticularTest(String userId, TestToRunDTO dto, UUID snippetId) {
        logger.info("Testing {} case for user {}", dto.testCaseId(), userId);
        Snippet snippet = snippetRepo.findById(snippetId).orElseThrow(NoSuchElementException::new);
        String content = assetService.getSnippet(snippet.getId()).getBody();
        String url = testingServiceUrl + "/run?userId=" + userId;
        logger.info("Url: {}",url);
        ResponseEntity.ok(restTemplate.postForEntity(url, new ParticularTestToRun(dto.testCaseId(), content), TestRunResultDTO.class).getBody());
    }

    public List<Snippet> getAllSnippetByOwner(List<UUID> snippetsId){
        return snippetRepo.findAllById(snippetsId);
    }
}

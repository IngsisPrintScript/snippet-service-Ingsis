package com.ingsis.snippetManager.intermediate.testing;

import com.azure.core.exception.ResourceNotFoundException;
import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import com.ingsis.snippetManager.redis.testing.TestRequestProducer;
import com.ingsis.snippetManager.redis.testing.dto.TestRequestEvent;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.testing.GetTestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestRunResultDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestToRunDTO;
import com.ingsis.snippetManager.snippet.dto.testing.UpdateDTO;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TestingService {

    private final RestTemplate restTemplate;
    private final String testingServiceUrl;
    private final SnippetRepo snippetRepo;
    private final TestRequestProducer testRequestProducer;
    private final UserPermissionService userPermissionService;
    private static final Logger logger = LoggerFactory.getLogger(TestingService.class);

    public TestingService(@Value("http://localhost:8084/test") String testingServiceUrl, SnippetRepo snippetRepo,
            TestRequestProducer testRequestProducer, UserPermissionService userPermissionService) {
        this.restTemplate = new RestTemplate();
        this.testingServiceUrl = testingServiceUrl;
        this.snippetRepo = snippetRepo;
        this.testRequestProducer = testRequestProducer;
        this.userPermissionService = userPermissionService;
    }

    public ResponseEntity<GetTestDTO> createTest(TestDTO testDTO, String jwtToken) {
        try {
            String url = testingServiceUrl + "/create";
            logger.info("POST {}", url);
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            logger.info("Outputs {}, inputs {}", testDTO.input(), testDTO.output());
            HttpEntity<TestDTO> entity = new HttpEntity<>(testDTO, headers);
            ResponseEntity<GetTestDTO> getTestDTO = restTemplate.exchange(url, HttpMethod.POST, entity,
                    GetTestDTO.class);
            logger.info("getTestDTO {}", getTestDTO.getBody());
            Snippet snippet = snippetRepo.findById(testDTO.snippetId()).orElseThrow(NoSuchElementException::new);
            logger.info("snippet {}", snippet.getId());
            try {
                logger.info("Body {}", getTestDTO.getBody() != null);
                snippet.getTestId().add(getTestDTO.getBody().testId());
                snippetRepo.save(snippet);
                return getTestDTO;
            } catch (NoSuchElementException e) {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error creating test: {}", e.getMessage());
            deleteParticularTest(testDTO.snippetId(), jwtToken);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<GetTestDTO> updateTest(UpdateDTO dto, String jwtToken) {
        try {
            String url = testingServiceUrl + "/update";
            logger.info("PUT {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<UpdateDTO> entity = new HttpEntity<>(dto, headers);

            ResponseEntity<GetTestDTO> response = restTemplate.exchange(url, HttpMethod.PUT, entity, GetTestDTO.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error updating test: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<List<GetTestDTO>> getTestsBySnippetId(UUID snippetId, String jwtToken) {
        try {
            String url = testingServiceUrl + "?snippetId=" + snippetId;
            logger.info("GET {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<GetTestDTO>> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<GetTestDTO>>() {
                    });
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching tests for snippet {}: {}", snippetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<String> deleteTestsBySnippet(UUID snippetId, String jwtToken) {
        try {
            String url = testingServiceUrl + "/delete?snippetId=" + snippetId;
            logger.info("DELETE {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);

            return ResponseEntity.ok("Tests for snippet " + snippetId + " deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting tests for snippet {}: {}", snippetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting tests: " + e.getMessage());
        }
    }

    public ResponseEntity<String> deleteParticularTest(UUID testId, String jwtToken) {
        try {
            String url = testingServiceUrl + "?testId=" + testId;
            logger.info("DELETE {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            Snippet snippet = snippetRepo.findById(findSnippetById(testId, jwtToken))
                    .orElseThrow(NoSuchElementException::new);
            snippet.getTestId().remove(testId);
            snippetRepo.save(snippet);
            return ResponseEntity.ok("Test " + testId + " deleted successfully.");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Test not found: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting test {}: {}", testId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting test: " + e.getMessage());
        }
    }

    public void runAllTestsForSnippet(UUID snippetId) {
        try {
            logger.info("Running all tests for snippet {}", snippetId);
            Snippet snippet = snippetRepo.findById(snippetId).orElseThrow();
            List<UUID> testsId = snippet.getTestId();
            for (UUID test : testsId) {
                TestToRunDTO dto = new TestToRunDTO(test, snippetId);
                runTestCase(dto, snippetId);
                logger.info("Test {} sent", dto.testCaseId());
            }
        } catch (Exception e) {
            logger.error("Error running tests automatically for snippet {}: {}", snippetId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void runTestCase(TestToRunDTO dto, UUID snippetId) {
        logger.info("Testing {} case ", dto.testCaseId());
        Snippet snippet = snippetRepo.findById(snippetId).orElseThrow(NoSuchElementException::new);
        TestRequestEvent event = new TestRequestEvent(snippetId, snippet.getLanguage());
        testRequestProducer.publish(event);
    }

    public ResponseEntity<TestRunResultDTO> runParticularTest(TestToRunDTO dto, String jwtToken) {
        try {
            String url = testingServiceUrl + "/run";
            logger.info("POST {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<TestToRunDTO> entity = new HttpEntity<>(dto, headers);

            ResponseEntity<TestRunResultDTO> response = restTemplate.exchange(url, HttpMethod.POST, entity,
                    TestRunResultDTO.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            logger.error("Error running test {}: {}", dto.testCaseId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public List<Snippet> getAllSnippetByOwner(List<UUID> snippetsId) {
        return snippetRepo.findAllById(snippetsId);
    }

    public boolean validateTest(String subject, UUID snippetId, AuthorizationActions authorizationActions) {
        return !userPermissionService.getUserSnippets(subject, authorizationActions).contains(snippetId);
    }

    public List<UUID> getUserSnippets(String userId, AuthorizationActions authorizationActions) {
        return userPermissionService.getUserSnippets(userId, authorizationActions);
    }

    public UUID findSnippetById(UUID testId, String jwtToken) {
        try {
            String url = testingServiceUrl + "/" + testId;
            logger.info("GET {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<UUID> response = restTemplate.exchange(url, HttpMethod.GET, entity, UUID.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching test {}: {}", testId, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

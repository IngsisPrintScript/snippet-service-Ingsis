package com.ingsis.snippetManager.intermediate.testing;

import com.ingsis.snippetManager.intermediate.azureStorageConfig.StorageService;
import com.ingsis.snippetManager.redis.testing.TestRequestProducer;
import com.ingsis.snippetManager.redis.testing.dto.TestRequestEvent;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.TestStatus;
import com.ingsis.snippetManager.snippet.dto.testing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class TestingService {

    private final RestTemplate restTemplate;
    private final String testingServiceUrl;
    private final SnippetRepo snippetRepo;
    private final StorageService storageService;
    private final TestRequestProducer testRequestProducer;
    private static final Logger logger = LoggerFactory.getLogger(TestingService.class);


    public TestingService(@Value("http://localhost:8084/test") String testingServiceUrl, SnippetRepo snippetRepo, StorageService storageService, TestRequestProducer testRequestProducer) {
        this.restTemplate = new RestTemplate();
        this.testingServiceUrl = testingServiceUrl;
        this.snippetRepo = snippetRepo;
        this.storageService = storageService;
        this.testRequestProducer = testRequestProducer;
    }

    public ResponseEntity<GetTestDTO> createTest(String userId, List<CreateTestDTO> createDTO) {
        try {
            logger.info("Creating linting rules for user {}", userId);
            String url = testingServiceUrl + "/create?userId=" + userId;
            logger.info("Creating at url: {}", url);
            return restTemplate.postForEntity(url, createDTO, GetTestDTO.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<?> updateTest(String userId, List<UpdateTestDTO> rulesDTO) {
        logger.info("Updating linting rules for user {}", userId);
        String url = testingServiceUrl + "/update?ownerId=" + userId;
        logger.info("Updating at url: {}", url);
        restTemplate.put(url, rulesDTO);
        return ResponseEntity.ok().build();
    }

    public void runAllTestsForSnippet(String userId, UUID snippetId, String snippetContent) {
        try {
            logger.info("Running all tests for snippet {}", snippetId);
            Snippet snippet = snippetRepo.findSnippetByIdAndSnippetOwnerId(snippetId,userId);
            List<UUID> testsId = snippet.getTestId();
            for (UUID test : testsId) {
                TestToRunDTO dto = new TestToRunDTO(test, snippetContent, snippet.getLanguage());
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
        byte[] contentBytes = storageService.download(dto.content());
        logger.info("Downloaded content for snippet {} from {}", snippetId, dto.content());
        String content = new String(contentBytes, StandardCharsets.UTF_8);
        TestRequestEvent event = new TestRequestEvent(
                userId,
                snippetId,
                dto.language(),
                content
        );
        testRequestProducer.publish(event);
    }

    public ResponseEntity<TestStatus> runParticularTest(String userId,TestToRunDTO dto){
        logger.info("Testing {} case for user {}", dto.testCaseId(), userId);
        byte[] contentBytes = storageService.download(dto.content());
        String content = new String(contentBytes, StandardCharsets.UTF_8);
        String url = testingServiceUrl + "/run?userId=" + userId;
        logger.info("Url: {}",url);
        return ResponseEntity.ok(restTemplate.postForEntity(url, new ParticularTestToRun(dto.testCaseId(), content), TestStatus.class).getBody());
    }
}

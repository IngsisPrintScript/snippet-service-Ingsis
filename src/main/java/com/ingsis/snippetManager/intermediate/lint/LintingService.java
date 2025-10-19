package com.ingsis.snippetManager.intermediate.lint;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.ingsis.snippetManager.intermediate.azureStorageConfig.StorageService;
import com.ingsis.snippetManager.redis.lint.LintRequestProducer;
import com.ingsis.snippetManager.redis.lint.dto.LintRequestEvent;
import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.EvaluateSnippet;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.CreateDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.Result;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.UpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LintingService {

    private final RestTemplate restTemplate;
    private final String lintingServiceUrl;
    private final SnippetRepo snippetRepo;
    private final StorageService storageService;
    private final LintRequestProducer lintRequestProducer;
    private static final Logger logger = LoggerFactory.getLogger(LintingService.class);

    public LintingService(@Value("http://localhost:8081/linting") String testingServiceUrl,
                          SnippetRepo snippetRepo,
                          StorageService storageService,
                          LintRequestProducer lintRequestProducer) {
        this.restTemplate = new RestTemplate();
        this.lintingServiceUrl = testingServiceUrl;
        this.snippetRepo = snippetRepo;
        this.storageService = storageService;
        this.lintRequestProducer = lintRequestProducer;
    }

    public ResponseEntity<?> createLinting(String userId, List<CreateDTO> createDTO) {
        try {
            logger.info("Creating linting rules for user {}", userId);
            String url = lintingServiceUrl + "/create?ownerId=" + userId;
            logger.info("Creating at url: {}", url);
            return restTemplate.postForEntity(url, createDTO, Void.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    public SnippetLintStatus validLinting(String content, String ownerId) {
        try {
            logger.info("Evaluating snippet for user {}", ownerId);
            EvaluateSnippet request = new EvaluateSnippet(content, ownerId);
            logger.info("Created the request: {}", request);
            String url = lintingServiceUrl + "/evaluate";
            logger.info("Evaluating at url: {}", url);
            ResponseEntity<SnippetLintStatus> response =
                    restTemplate.postForEntity(url, request, SnippetLintStatus.class);
            if (response.getBody() == null) {
                return SnippetLintStatus.FAILED;
            }
            return response.getBody().equals(SnippetLintStatus.PASSED) ? SnippetLintStatus.PASSED : SnippetLintStatus.FAILED;
        } catch (Exception e) {
            return SnippetLintStatus.FAILED;
        }
    }

    public ResponseEntity<List<Result>> failedLinting(String content, String ownerId) {
        try {
            logger.info("Evaluating snippet for user {}", ownerId);
            EvaluateSnippet request = new EvaluateSnippet(content, ownerId);
            logger.info("Created the request: {}", request);
            String url = lintingServiceUrl + "/evaluate/pass";
            logger.info("Evaluating at url: {}", url);
            return restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<List<Result>>() {
                    }
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }


    public ResponseEntity<?> updateLintingRules(String userId, List<UpdateDTO> rulesDTO) {
        logger.info("Updating linting rules for user {}", userId);
        String url = lintingServiceUrl + "/update?ownerId=" + userId;
        logger.info("Updating at url: {}", url);
        restTemplate.put(url, rulesDTO);
        reLintAllSnippets(userId);
        return ResponseEntity.ok().build();
    }

    public void reLintAllSnippets(String userId) {
        List<Snippet> snippets = snippetRepo.findAllAccessibleByUserId(userId);
        logger.info("Linting {} snippets for user {}", snippets.size(), userId);
        for (Snippet snippet : snippets) {
            snippet.setLintStatus(SnippetLintStatus.PENDING);
            logger.info("Linting snippet {} set to {}", snippet.getId(), snippet.getLintStatus());
            byte[] contentBytes = storageService.download(snippet.getContentUrl());
            logger.info("Downloaded content for snippet {} from {}", snippet.getId(), snippet.getContentUrl());
            String content = new String(contentBytes, StandardCharsets.UTF_8);
            LintRequestEvent event = new LintRequestEvent(
                    userId,
                    snippet.getId(),
                    snippet.getLanguage(),
                    content
            );
            lintRequestProducer.publish(event);
        }
    }
}

package com.ingsis.snippetManager.intermediate.format;

import com.ingsis.snippetManager.intermediate.azureStorageConfig.StorageService;
import com.ingsis.snippetManager.redis.format.FormatRequestProducer;
import com.ingsis.snippetManager.redis.format.dto.FormatRequestEvent;
import com.ingsis.snippetManager.redis.format.dto.SnippetFormatStatus;
import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.format.FormatResult;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.EvaluateSnippet;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.CreateDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.UpdateDTO;
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
public class FormatService {

    private final RestTemplate restTemplate;
    private final String formatServiceUrl;
    private final SnippetRepo snippetRepo;
    private final StorageService storageService;
    private final FormatRequestProducer formatRequestProducer;
    private static final Logger logger = LoggerFactory.getLogger(FormatService.class);

    public FormatService(@Value("http://localhost:8082/formater") String testingServiceUrl,
                          SnippetRepo snippetRepo,
                          StorageService storageService,
                          FormatRequestProducer formatRequestProducer) {
        this.restTemplate = new RestTemplate();
        this.formatServiceUrl = testingServiceUrl;
        this.snippetRepo = snippetRepo;
        this.storageService = storageService;
        this.formatRequestProducer = formatRequestProducer;
    }

    public ResponseEntity<?> createLinting(String userId, List<CreateDTO> createDTO) {
        try {
            logger.info("Creating format rules for user {}", userId);
            String url = formatServiceUrl + "/create?ownerId=" + userId;
            logger.info("Creating at url: {}", url);
            return restTemplate.postForEntity(url, createDTO, Void.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    public SnippetFormatStatus formatContent(Snippet snippet, String ownerId) {
        try {
            byte[] contentBytes = storageService.download(snippet.getContentUrl());
            String content = new String(contentBytes, StandardCharsets.UTF_8);
            logger.info("Evaluating snippet for user {}", ownerId);
            EvaluateSnippet request = new EvaluateSnippet(content, ownerId);
            logger.info("Created the request: {}", request);
            String url = formatServiceUrl + "/format";
            logger.info("Evaluating at url: {}", url);
            ResponseEntity<FormatResult> response =
                    restTemplate.postForEntity(url, request, FormatResult.class);
            if(response.getBody() == null || response.getBody().content().isEmpty()){
                return SnippetFormatStatus.PASSED;
            }
            String newContent = response.getBody().content();
            if (!newContent.equals(content)) {
                byte[] contentToUpdate = newContent.getBytes(StandardCharsets.UTF_8);
                String blobNameFromUrl = storageService.getBlobNameFromUrl(snippet.getContentUrl());
                snippet.setContentUrl(storageService.upload("snippets", blobNameFromUrl, contentToUpdate)); // overwrite=true
            }
            snippetRepo.save(snippet);
            return SnippetFormatStatus.PASSED;
        } catch (Exception e) {
            return SnippetFormatStatus.FAILED;
        }
    }

    public ResponseEntity<?> updateFormatRules(String userId, List<UpdateDTO> rulesDTO) {
        logger.info("Updating format rules for user {}", userId);
        String url = formatServiceUrl + "/update?ownerId=" + userId;
        logger.info("Updating at url: {}", url);
        restTemplate.put(url, rulesDTO);
        reFormatAllSnippets(userId);
        return ResponseEntity.ok().build();
    }

    public void reFormatAllSnippets(String userId) {
        List<Snippet> snippets = snippetRepo.findAllAccessibleByUserId(userId);
        logger.info("Format {} snippets for user {}", snippets.size(), userId);
        for (Snippet snippet : snippets) {
            snippet.setLintStatus(SnippetLintStatus.PENDING);
            logger.info("Format snippet {} set to {}", snippet.getId(), snippet.getLintStatus());
            logger.info("Downloading content for snippet {} from {}", snippet.getId(), snippet.getContentUrl());
            byte[] contentBytes;
            try {
                contentBytes = storageService.download(snippet.getContentUrl());
            } catch (RuntimeException e) {
                if (e.getMessage().contains("BlobNotFound")) {
                    logger.warn("Blob not found for snippet {}, skipping format", snippet.getId());
                    continue;
                } else {
                    throw e;
                }
            }
            logger.info("Downloaded content for snippet {} from {}", snippet.getId(), snippet.getContentUrl());
            String content = new String(contentBytes, StandardCharsets.UTF_8);
            FormatRequestEvent event = new FormatRequestEvent(
                    userId,
                    snippet.getId(),
                    snippet.getLanguage(),
                    content
            );
            formatRequestProducer.publish(event);
        }
    }
}

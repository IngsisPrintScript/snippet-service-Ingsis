package com.ingsis.snippetManager.intermediate.engine;

import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.intermediate.engine.dto.request.FormatRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.request.LintRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.request.RunSnippetRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.request.SimpleRunSnippet;
import com.ingsis.snippetManager.intermediate.engine.dto.request.TestRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.response.TestResponseDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.response.ValidationResult;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class EngineService {

    private final Logger logger = LoggerFactory.getLogger(EngineService.class);
    private final String engineUrl;
    private final RestTemplate restTemplate;
    private final AssetService assetService;

    public EngineService(@Value("${engine.service.base-url}") String engine, AssetService assetService) {
        this.engineUrl = engine;
        this.restTemplate = new RestTemplate();
        this.assetService = assetService;
    }

    private <T, R> ResponseEntity<R> post(String path, T body, String token, Class<R> clazz) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            HttpEntity<T> request = new HttpEntity<>(body, headers);

            ResponseEntity<R> response = restTemplate.postForEntity(engineUrl + path, request, clazz);

            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException e) {
            logger.error("Engine error: {} - Status {} - URL {}", e.getMessage(), e.getStatusCode(), engineUrl + path);
            return ResponseEntity.status(e.getStatusCode()).build();

        } catch (Exception e) {
            logger.error("Engine internal error: {} - URL {}", e.getMessage(), engineUrl + path);
            return ResponseEntity.internalServerError().build();
        }
    }

    public ResponseEntity<RunSnippetResponseDTO> execute(RunSnippetRequestDTO dto, String token) {
        return post("/run/execute", dto, token, RunSnippetResponseDTO.class);
    }

    public ResponseEntity<UUID> format(FormatRequestDTO dto, String token) {
        return post("/run/format", dto, token, UUID.class);
    }

    public ResponseEntity<ValidationResult> analyze(LintRequestDTO dto, String token) {
        return post("/run/analyze", dto, token, ValidationResult.class);
    }
    public ResponseEntity<ValidationResult> validate(SimpleRunSnippet dto, String token) {
        return post("/run/validate", dto, token, ValidationResult.class);
    }

    public ResponseEntity<TestResponseDTO> test(TestRequestDTO dto, String token) {
        return post("/run/test", dto, token, TestResponseDTO.class);
    }

    private UUID saveSnippet(UUID snippetId, UUID formatId, String formattedContent) {
        try {
            assetService.saveOriginalSnippet(snippetId, formatId);
            assetService.saveSnippet(snippetId, formattedContent);
            return snippetId;
        } catch (Exception e) {
            return formatId;
        }
    }
}

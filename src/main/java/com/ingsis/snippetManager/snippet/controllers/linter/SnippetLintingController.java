package com.ingsis.snippetManager.snippet.controllers.linter;


import com.ingsis.snippetManager.intermediate.lint.LintingService;
import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetController;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.CreateDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.Result;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.SnippetValidLintingDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.UpdateDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetContentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lint")
public class SnippetLintingController {

    private final LintingService lintingService;
    private final SnippetController snippetController;
    private final SnippetRepo snippetRepo;
    private static final Logger logger = LoggerFactory.getLogger(SnippetLintingController.class);

    public SnippetLintingController(
            LintingService lintingService,
            SnippetController snippetController,
            SnippetRepo snippetRepo) {
        this.lintingService = lintingService;
        this.snippetController = snippetController;
        this.snippetRepo = snippetRepo;
    }

    @GetMapping("/validate/{snippetId}")
    public ResponseEntity<SnippetLintStatus> validateSnippetLinting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snippetId
    ) {
        try {
            String ownerId = getString(jwt);
            logger.info("Validating linting for snippet {} by user {}", snippetId, ownerId);
            SnippetContentDTO contentDTO = (snippetController.getSnippet(jwt, snippetId)).getBody();
            logger.info("SnippetDTO exist? {}", contentDTO != null);
            if (contentDTO == null) {
                return ResponseEntity.badRequest().build();
            }
            ;
            SnippetLintStatus passes = lintingService.validLinting(contentDTO.content(), ownerId);
            return ResponseEntity.ok(passes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SnippetLintStatus.FAILED);
        }
    }

    @GetMapping("/failed/{snippetId}")
    public ResponseEntity<List<Result>> getFailedLinting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snippetId
    ) {
        try {
            String ownerId = getString(jwt);
            SnippetContentDTO snippetUrl = snippetController.getSnippet(jwt, snippetId).getBody();
            if (snippetUrl == null) {
                return ResponseEntity.badRequest().build();
            }
            return lintingService.failedLinting(snippetUrl.content(), ownerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateLintingRules(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<UpdateDTO> rulesDTO
    ) {
        try {
            String ownerId = getString(jwt);
            return lintingService.updateLintingRules(ownerId, rulesDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating linting rules: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createLintingRules(@AuthenticationPrincipal Jwt jwt, @RequestBody List<CreateDTO> rulesDTO) {
        try {
            String ownerId = getString(jwt);
            return ResponseEntity.ok(lintingService.createLinting(ownerId, rulesDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating linting rules: " + e.getMessage());
        }

    }

    @GetMapping("/snippets/lint-status")
    public ResponseEntity<List<SnippetValidLintingDTO>> getLintStatuses(@AuthenticationPrincipal Jwt jwt) {
        String userId = getString(jwt);
        List<Snippet> snippets = snippetRepo.findAll();
        List<SnippetValidLintingDTO> response = snippets.stream()
                .map(snippet -> new SnippetValidLintingDTO(snippet, snippet.getLintStatus()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{userId}")
    public List<Snippet> getLintStatus(@PathVariable String userId) {
        return snippetRepo.findAll();
    }

    private static String getString(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }
}

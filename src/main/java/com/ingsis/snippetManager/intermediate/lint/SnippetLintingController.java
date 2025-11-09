package com.ingsis.snippetManager.intermediate.lint;


import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetController;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.*;
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
    private final UserPermissionService userPermissionService;
    private static final Logger logger = LoggerFactory.getLogger(SnippetLintingController.class);

    public SnippetLintingController(
            LintingService lintingService,
            SnippetController snippetController,
            UserPermissionService userPermissionService) {
        this.lintingService = lintingService;
        this.snippetController = snippetController;
        this.userPermissionService = userPermissionService;
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
        List<UUID> snippetsOwner = userPermissionService.getUserSnippets(userId, AuthorizationActions.ALL);
        List<Snippet> snippets = lintingService.getAllSnippetByOwner(snippetsOwner);
        List<SnippetValidLintingDTO> response = snippets.stream()
                .map(snippet -> new SnippetValidLintingDTO(snippet, snippet.getLintStatus()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<List<GetSnippetLintingStatusDTO>> getLintStatus(@AuthenticationPrincipal Jwt jwt) {
        String userId = getString(jwt);
        List<UUID> snippetsOwner = userPermissionService.getUserSnippets(userId, AuthorizationActions.ALL);
        List<Snippet> snippets = lintingService.getAllSnippetByOwner(snippetsOwner);
        return ResponseEntity.ok(snippets.stream().map(snippet -> new GetSnippetLintingStatusDTO(snippet.getId(), snippet.getLintStatus())).toList());
    }

    private static String getString(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }
}

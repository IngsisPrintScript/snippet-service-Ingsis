package com.ingsis.snippetManager.snippet.controllers.format;

import com.ingsis.snippetManager.intermediate.format.FormatService;
import com.ingsis.snippetManager.redis.format.dto.SnippetFormatStatus;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetController;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.format.SnippetValidFormatDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.CreateDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.SnippetValidLintingDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.UpdateDTO;
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
@RequestMapping("/format")
public class SnippetFormatController {

    private final FormatService formatService;
    private final SnippetController snippetController;
    private final SnippetRepo snippetRepo;
    private static final Logger logger = LoggerFactory.getLogger(SnippetFormatController.class);

    public SnippetFormatController(
            FormatService formatService,
            SnippetController snippetController,
            SnippetRepo snippetRepo) {
        this.formatService = formatService;
        this.snippetController = snippetController;
        this.snippetRepo = snippetRepo;
    }

    @GetMapping("/{snippetId}")
    public ResponseEntity<SnippetFormatStatus> formatSnippet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snippetId
    ) {
        try {
            String ownerId = getString(jwt);
            logger.info("Validating linting for snippet {} by user {}", snippetId, ownerId);
            Snippet contentDTO = (snippetController.getAllSnippetData(jwt, snippetId)).getBody();
            logger.info("SnippetDTO exist? {}", contentDTO != null);
            if (contentDTO == null) {
                return ResponseEntity.badRequest().build();
            }
            SnippetFormatStatus passes = formatService.formatContent(contentDTO, ownerId);
            return ResponseEntity.ok(passes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SnippetFormatStatus.FAILED);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateFormatRules(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<UpdateDTO> rulesDTO
    ) {
        try {
            String ownerId = getString(jwt);
            return formatService.updateFormatRules(ownerId, rulesDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating format rules: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createFormatRule(@AuthenticationPrincipal Jwt jwt, @RequestBody List<CreateDTO> rulesDTO) {
        try {
            String ownerId = getString(jwt);
            return ResponseEntity.ok(formatService.createLinting(ownerId, rulesDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating linting rules: " + e.getMessage());
        }

    }

    @GetMapping("/snippets/format-status")
    public ResponseEntity<List<SnippetValidFormatDTO>> getFormatStatuses(@AuthenticationPrincipal Jwt jwt) {
        String userId = getString(jwt);
        List<Snippet> snippets = snippetRepo.findAll();
        List<SnippetValidFormatDTO> response = snippets.stream()
                .map(snippet -> new SnippetValidFormatDTO(snippet, snippet.getFormatStatus()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{userId}")
    public List<Snippet> getFormatStatus(@PathVariable String userId) {
        return snippetRepo.findAll();
    }

    private static String getString(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }
}

package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.intermediate.LintingService;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestFileDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestSnippetDTO;
import com.ingsis.snippetManager.snippet.dto.Converter;
import com.ingsis.snippetManager.intermediate.UserAuthorizationService;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetContentDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetFilterDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/snippets")
public class SnippetController {

    private final SnippetService snippetService;
    private final UserAuthorizationService userAuthorizationService;
    private static final Logger logger = LoggerFactory.getLogger(SnippetController.class);

    public SnippetController(
            SnippetService snippetService,
            UserAuthorizationService userAuthorizationService) {
        this.snippetService = snippetService;
        this.userAuthorizationService = userAuthorizationService;
    }

    // User Story 1
    @PostMapping("/create/file")
    public ResponseEntity<?> createSnippetFromFile(
            @ModelAttribute RequestFileDTO fileDTO, @AuthenticationPrincipal Jwt jwt) throws IOException {
        Snippet snippet = getSnippet(fileDTO);
        ValidationResult saved = snippetService.createSnippet(snippet, getOwnerId(jwt));

        if (!saved.isValid()) {
            String errorMsg =
                    String.format(
                            "Invalid Snippet: %s in line: %d, column: %d",
                            saved.getMessage(), saved.getLine(), saved.getColumn());
            return ResponseEntity.unprocessableEntity().body(errorMsg);
        }
        return ResponseEntity.ok(saved);
    }

    private Snippet getSnippet(RequestFileDTO fileDTO) throws IOException {
        String contentUrl = snippetService.uploadSnippetContent(
                "snippets", fileDTO.file().getOriginalFilename(), new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8));
        return new Converter().convertFileToSnippet(fileDTO, contentUrl);
    }

    // User Story 3
    @PostMapping("/create/text")
    public ResponseEntity<?> createSnippet(
            @RequestBody RequestSnippetDTO snippet, @AuthenticationPrincipal Jwt jwt) {
        String contentUrl = snippetService.uploadSnippetContent(
                "snippets", "code", snippet.content());

        ValidationResult saved =
                snippetService.createSnippet(
                        new Converter().convertToSnippet(snippet, contentUrl), getOwnerId(jwt));

        if (!saved.isValid()) {
            String errorMsg =
                    String.format(
                            "Invalid Snippet: %s in line: %d, column: %d",
                            saved.getMessage(), saved.getLine(), saved.getColumn());
            return ResponseEntity.unprocessableEntity().body(errorMsg);
        }
        return ResponseEntity.ok(saved);
    }

    private static String getOwnerId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }

    // User Story 2
    @PutMapping("/{id}/update/file")
    public ResponseEntity<?> updateSnippetFromFile(
            @PathVariable UUID id,
            @ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Snippet snippet = getSnippet(fileDTO);
            ValidationResult result = snippetService.updateSnippet(id, snippet, getOwnerId(jwt));
            if (!result.isValid()) {
                String errorMsg =
                        String.format(
                                "Invalid Snippet: %s in line: %d, column: %d",
                                result.getMessage(), result.getLine(), result.getColumn());
                return ResponseEntity.unprocessableEntity().body(errorMsg);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }

    // User Story 4
    @PutMapping("/{id}/update/text")
    public ResponseEntity<?> updateSnippet(
            @PathVariable UUID id,
            @RequestBody RequestSnippetDTO updatedSnippet,
            @AuthenticationPrincipal Jwt jwt) {
        String contentUrl = snippetService.uploadSnippetContent(
                "snippets", "code", updatedSnippet.content());
        ValidationResult result =
                snippetService.updateSnippet(
                        id, new Converter().convertToSnippet(updatedSnippet, contentUrl), getOwnerId(jwt));
        if (!result.isValid()) {
            String errorMsg =
                    String.format(
                            "Invalid Snippet: %s in line: %d, column: %d",
                            result.getMessage(), result.getLine(), result.getColumn());
            return ResponseEntity.unprocessableEntity().body(errorMsg);
        }
        return ResponseEntity.ok(result);
    }

    // User story 5
    @GetMapping
    public ResponseEntity<?> getSnippets(
            @AuthenticationPrincipal Jwt jwt, @RequestBody SnippetFilterDTO filterDTO) {
        try {
            List<Snippet> snippets =
                    filterDTO == null
                            ? snippetService.getAllSnippetsByOwner(getOwnerId(jwt))
                            : snippetService.getSnippetsBy(getOwnerId(jwt), filterDTO);
            return ResponseEntity.ok(snippetService.filterValidSnippets(snippets, filterDTO, getOwnerId(jwt)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting the snippets: " + e.getMessage());
        }
    }

    //User story 5
    @GetMapping("/{id}")
    public ResponseEntity<SnippetContentDTO> getSnippet(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        logger.info("Getting snippet with id: {} by {}", id, getOwnerId(jwt));
        Snippet snippet = snippetService.getSnippetById(id, getOwnerId(jwt));
        logger.info("Snippet exist? {}", snippet != null);
        if (snippet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(new SnippetContentDTO(
                snippet.getName(), snippet.getDescription(), snippet.getLanguage(),
                snippetService.downloadSnippetContent(snippet.getId())));

    }
}

package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.intermediate.testing.TestingService;
import com.ingsis.snippetManager.redis.testing.dto.TestReturnDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.*;
import com.ingsis.snippetManager.snippet.dto.Converter;
import com.ingsis.snippetManager.intermediate.UserAuthorizationService;
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
    private final TestingService testingService;
    private static final Logger logger = LoggerFactory.getLogger(SnippetController.class);

    public SnippetController(
            SnippetService snippetService,
            UserAuthorizationService userAuthorizationService,TestingService testingService) {
        this.snippetService = snippetService;
        this.userAuthorizationService = userAuthorizationService;
        this.testingService = testingService;
    }

    // User Story 1
    @PostMapping("/create/file")
    public ResponseEntity<?> createSnippetFromFile(
            @ModelAttribute RequestFileDTO fileDTO, @AuthenticationPrincipal Jwt jwt) throws IOException {
        Snippet snippet = getSnippetFromFile(fileDTO);
        snippetService.uploadSnippetContent(snippet.getId(),new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8));
        ValidationResult saved = snippetService.createSnippet(snippet);
        if (!saved.isValid()) {
            String errorMsg =
                    String.format(
                            "Invalid Snippet: %s in line: %d, column: %d",
                            saved.getMessage(), saved.getLine(), saved.getColumn());
            return ResponseEntity.unprocessableEntity().body(errorMsg);
        }
        return ResponseEntity.ok(saved);
    }

    // User Story 3
    @PostMapping("/create/text")
    public ResponseEntity<?> createSnippet(
            @RequestBody RequestSnippetDTO snippetDTO, @AuthenticationPrincipal Jwt jwt) {
        Snippet snippet = getSnippetFromText(snippetDTO);
        snippetService.uploadSnippetContent(snippet.getId(), snippetDTO.content());
        ValidationResult saved =
                snippetService.createSnippet(snippet);
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
            Snippet snippet = getSnippetFromFile(fileDTO);
            ValidationResult result = snippetService.updateSnippet(id, snippet,new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8));
            testingService.runAllTestsForSnippet(getOwnerId(jwt), id);
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
        Snippet snippet = getSnippetFromText(updatedSnippet);
        ValidationResult result = snippetService.updateSnippet(id, snippet,updatedSnippet.content());
        testingService.runAllTestsForSnippet(getOwnerId(jwt), id);
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
        Snippet snippet = snippetService.getSnippetById(id);
        logger.info("Snippet exist? {}", snippet != null);
        if (snippet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(new SnippetContentDTO(
                snippet.getName(), snippet.getDescription(), snippet.getLanguage(),
                snippetService.downloadSnippetContent(snippet.getId())));

    }
    @GetMapping("/snippet")
    public ResponseEntity<Snippet> getAllSnippetData( @AuthenticationPrincipal Jwt jwt, @RequestParam UUID id) {
        logger.info("Getting snippet with id: {} by {}", id, getOwnerId(jwt));
        Snippet snippet = snippetService.getSnippetById(id);
        logger.info("Snippet exist? {}", snippet != null);
        if (snippet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(snippet);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSnippet(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getOwnerId(jwt);
        logger.info("Deleting snippet {} for user {}", id, userId);

        try {
            Snippet snippet = snippetService.getSnippetById(id);
            if (snippet == null) {
                return ResponseEntity.status(404).body("Snippet not found or not accessible.");
            }
            boolean deletedPermissions = userAuthorizationService.deleteUserAuthorization(userId, id);
            if (!deletedPermissions) {
                logger.warn("Permissions for snippet {} could not be deleted for user {}", id, userId);
                return ResponseEntity.status(404).body("Permissions for snippet " + id + " could not be deleted.");
            }
            testingService.deleteSnippetTest(id);
            logger.info("Snippet {} deleted successfully for user {}", id, userId);
            return snippetService.deleteSnippet(id);
        } catch (Exception e) {
            logger.error("Error deleting snippet {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Error deleting snippet: " + e.getMessage());
        }
    }

    private Snippet getSnippetFromFile(RequestFileDTO fileDTO){
        return new Converter().convertFileToSnippet(fileDTO);
    }

    private Snippet getSnippetFromText(RequestSnippetDTO fileDTO){
        return new Converter().convertToSnippet(fileDTO);
    }
}

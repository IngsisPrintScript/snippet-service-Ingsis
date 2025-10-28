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
        try {
            System.out.println("=== DEBUG: createSnippet llamado ===");
            System.out.println("Snippet name: " + snippet.name());
            System.out.println("Snippet language: " + snippet.language());
            
            // TEMPORAL: Para desarrollo sin Azure Storage, usar content directamente
            String contentUrl = snippet.content(); // Usar content directo en vez de subir a Azure
            
            // TODO: Habilitar upload a Azure cuando esté configurado
            // String contentUrl = snippetService.uploadSnippetContent("snippets", "code", snippet.content());

            String ownerId = getOwnerId(jwt);
            System.out.println("OwnerId: " + ownerId);
            
            Snippet converted = new Converter().convertToSnippet(snippet, contentUrl, ownerId);
            System.out.println("Snippet convertido: " + converted.getName());
            
            ValidationResult saved =
                    snippetService.createSnippet(converted, ownerId);
            
            System.out.println("ValidationResult: " + saved.isValid());
            if (!saved.isValid()) {
                String errorMsg =
                        String.format(
                                "Invalid Snippet: %s in line: %d, column: %d",
                                saved.getMessage(), saved.getLine(), saved.getColumn());
                return ResponseEntity.unprocessableEntity().body(errorMsg);
            }
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.out.println("ERROR en createSnippet: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private static String getOwnerId(Jwt jwt) {
        // Development mode: allow missing JWT and use a fallback owner id
        return jwt != null ? jwt.getClaimAsString("sub") : "dev-user";
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
            testingService.runAllTestsForSnippet(getOwnerId(jwt), id, snippet.getContentUrl());
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
        // TEMPORAL: Para desarrollo sin Azure Storage, usar content directamente
        String contentUrl = updatedSnippet.content(); // Usar content directo en vez de subir a Azure
        
        // TODO: Habilitar upload a Azure cuando esté configurado
        // String contentUrl = snippetService.uploadSnippetContent("snippets", "code", updatedSnippet.content());
        
        String ownerId = getOwnerId(jwt);
        ValidationResult result =
                snippetService.updateSnippet(
                        id, new Converter().convertToSnippet(updatedSnippet, contentUrl, ownerId), ownerId);
        testingService.runAllTestsForSnippet(ownerId, id, updatedSnippet.content());
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
    @PostMapping("/list")
    public ResponseEntity<?> getSnippets(
            @AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) SnippetFilterDTO filterDTO) {
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
    @GetMapping("/snippet")
    public ResponseEntity<Snippet> getAllSnippetData( @AuthenticationPrincipal Jwt jwt, @RequestParam UUID id) {
        logger.info("Getting snippet with id: {} by {}", id, getOwnerId(jwt));
        Snippet snippet = snippetService.getSnippetById(id, getOwnerId(jwt));
        logger.info("Snippet exist? {}", snippet != null);
        if (snippet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(snippet);
    }
}

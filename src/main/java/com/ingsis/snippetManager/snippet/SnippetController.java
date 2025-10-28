package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.testing.TestingService;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.*;
import com.ingsis.snippetManager.snippet.dto.Converter;
import com.ingsis.snippetManager.intermediate.UserPermissionService;
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
    private final UserPermissionService userPermissionService;
    private final TestingService testingService;


    public SnippetController(
            SnippetService snippetService,
            UserPermissionService userPermissionService, TestingService testingService,
            UserPermissionService permissionService) {
        this.snippetService = snippetService;
        this.userPermissionService = userPermissionService;
        this.testingService = testingService;
    }

    //Create from a file
    @PostMapping("/create/file")
    public ResponseEntity<?> createSnippetFromFile(
            @ModelAttribute RequestFileDTO fileDTO, @AuthenticationPrincipal Jwt jwt) throws IOException {
        Snippet snippet = getSnippetFromFile(fileDTO);
        ResponseEntity<String> response = userPermissionService.createUser(jwt.getSubject(),AuthorizationActions.ALL,snippet.getId());
        if(!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.internalServerError().body(response.getBody());
        }
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

    // Create from txt
    @PostMapping("/create/text")
    public ResponseEntity<?> createSnippet(
            @RequestBody RequestSnippetDTO snippetDTO, @AuthenticationPrincipal Jwt jwt) {
        Snippet snippet = getSnippetFromText(snippetDTO);
        ResponseEntity<String> response = userPermissionService.createUser(jwt.getSubject(),AuthorizationActions.ALL,snippet.getId());
        if(!response.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.internalServerError().body(response.getBody());
        }
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
    }

    private static String getOwnerId(Jwt jwt) {
        // Development mode: allow missing JWT and use a fallback owner id
        return jwt != null ? jwt.getClaimAsString("sub") : "dev-user";
    }

    // Update from File
    @PutMapping("/{id}/update/file")
    public ResponseEntity<String> updateSnippetFromFile(
            @PathVariable UUID id,
            @ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Snippet snippet = getSnippetFromFile(fileDTO);
            if(!userPermissionService.getUserSnippets(
                    jwt.getSubject(),AuthorizationActions.ALL).contains(snippet.getId())){
                return ResponseEntity.unprocessableEntity().body("Not Authorized to modify snippet");
            }
            ValidationResult result = snippetService.updateSnippet(id, snippet,new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8));
            testingService.runAllTestsForSnippet(getOwnerId(jwt), id);
            if (!result.isValid()) {
                String errorMsg =
                        String.format(
                                "Invalid Snippet: %s in line: %d, column: %d",
                                result.getMessage(), result.getLine(), result.getColumn());
                return ResponseEntity.unprocessableEntity().body(errorMsg);
            }
            return ResponseEntity.ok(result.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }

    // Update from txt
    @PutMapping("/{id}/update/text")
    public ResponseEntity<String> updateSnippet(
            @PathVariable UUID id,
            @RequestBody RequestSnippetDTO updatedSnippet,
            @AuthenticationPrincipal Jwt jwt) {
        Snippet snippet = getSnippetFromText(updatedSnippet);
        if(!userPermissionService.getUserSnippets(
                jwt.getSubject(),AuthorizationActions.ALL).contains(snippet.getId())){
            return ResponseEntity.unprocessableEntity().body("Not Authorized to modify snippet");
        }
        ValidationResult result = snippetService.updateSnippet(id, snippet,updatedSnippet.content());
        testingService.runAllTestsForSnippet(getOwnerId(jwt), id);
        if (!result.isValid()) {
            String errorMsg =
                    String.format(
                            "Invalid Snippet: %s in line: %d, column: %d",
                            result.getMessage(), result.getLine(), result.getColumn());
            return ResponseEntity.unprocessableEntity().body(errorMsg);
        }
        return ResponseEntity.ok(result.getMessage());
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

    @GetMapping("/{id}")
    public ResponseEntity<SnippetContentDTO> getSnippet(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Snippet snippet = snippetService.getSnippetById(id);
        if (snippet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(new SnippetContentDTO(
                snippet.getName(), snippet.getDescription(), snippet.getLanguage(),
                snippetService.downloadSnippetContent(snippet.getId())));

    }

    @GetMapping("/snippet")
    public ResponseEntity<Snippet> getAllSnippetData( @AuthenticationPrincipal Jwt jwt, @RequestParam UUID id) {
        Snippet snippet = snippetService.getSnippetById(id);
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
        try {
            Snippet snippet = snippetService.getSnippetById(id);
            if (snippet == null) {
                return ResponseEntity.status(404).body("Snippet not found or not accessible.");
            }
            ResponseEntity<String> deletedPermissions = userPermissionService.deleteUserAuthorization(userId,id);
            if (!deletedPermissions.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(404).body("Permissions for snippet " + id + " could not be deleted.");
            }
            ResponseEntity<String> deleteTest = testingService.deleteTest(userId, id);
            if (!deleteTest.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(404).body("Permissions for snippet " + id + " could not be deleted.");
            }
            return snippetService.deleteSnippet(id);
        } catch (Exception e) {
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

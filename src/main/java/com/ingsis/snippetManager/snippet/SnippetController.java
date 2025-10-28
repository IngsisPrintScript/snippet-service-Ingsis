package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.PermissionDTO;
import com.ingsis.snippetManager.intermediate.testing.TestingService;
import com.ingsis.snippetManager.snippet.controllers.filters.Property;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.*;
import com.ingsis.snippetManager.snippet.dto.Converter;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return ResponseEntity.ok(saved);
    }

    private static String getOwnerId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
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

    // Filter Snippets
    @GetMapping("/filter")
    public ResponseEntity<?> getSnippets(
            @AuthenticationPrincipal Jwt jwt, @RequestBody SnippetFilterDTO filterDTO) {
        try {
            if(filterDTO == null) {
                return ResponseEntity.ok(snippetService.getAllSnippetsByOwner(getAllUuids(jwt,Property.BOTH)));
            }
            List<UUID> userSnippets = getAllUuids(jwt,filterDTO.property());
            List<Snippet> snippets = snippetService.getSnippetsBy(userSnippets,filterDTO);
            return ResponseEntity.ok(snippetService.filterValidSnippets(snippets, filterDTO, getOwnerId(jwt)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting the snippets: " + e.getMessage());
        }
    }

    private @NotNull List<UUID> getAllUuids(Jwt jwt, Property principal) {
        if(principal == Property.BOTH) {
            Set<UUID> set = Stream.concat(
                    userPermissionService.getUserSnippets(getOwnerId(jwt), AuthorizationActions.ALL).stream(),
                    userPermissionService.getUserSnippets(getOwnerId(jwt), AuthorizationActions.READ).stream()
            ).collect(Collectors.toSet());
            return List.copyOf(set);
        }
        AuthorizationActions authorizationActions = principal == Property.OWNER ? AuthorizationActions.ALL : AuthorizationActions.READ;
        return userPermissionService.getUserSnippets(getOwnerId(jwt),authorizationActions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SnippetContentDTO> getSnippet(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        if(!userPermissionService.getUserSnippets(jwt.getSubject(),AuthorizationActions.ALL).contains(id)
        || !userPermissionService.getUserSnippets(jwt.getSubject(),AuthorizationActions.READ).contains(id)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Snippet snippet = snippetService.getSnippetById(id);
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
            ResponseEntity<String> deletedPermissions = userPermissionService.deleteSnippetUserAuthorization(id);
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

    @PutMapping("/{snippetId}/share")
    public ResponseEntity<String> shareSnippet(@AuthenticationPrincipal Jwt jwt, @RequestBody PermissionDTO shareSnippetDTO,@PathVariable UUID snippetId) {
        if(!userPermissionService.getUserSnippets(jwt.getSubject(),AuthorizationActions.ALL).contains(snippetId)){
            return ResponseEntity.unprocessableEntity().body("Not Authorized to modify snippet");
        }
        ResponseEntity<String> response = userPermissionService.createUser(shareSnippetDTO.userName(),AuthorizationActions.READ,snippetId);
        return response;
    }

    private Snippet getSnippetFromFile(RequestFileDTO fileDTO){
        return new Converter().convertFileToSnippet(fileDTO);
    }

    private Snippet getSnippetFromText(RequestSnippetDTO fileDTO){
        return new Converter().convertToSnippet(fileDTO);
    }
}

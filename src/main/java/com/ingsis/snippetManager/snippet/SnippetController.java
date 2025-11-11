package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.PermissionDTO;
import com.ingsis.snippetManager.snippet.dto.DataDTO;
import com.ingsis.snippetManager.snippet.dto.filters.Property;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.SnippetValidLintingDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.*;
import com.ingsis.snippetManager.snippet.dto.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    private static final Logger logger = LoggerFactory.getLogger(SnippetController.class);


    public SnippetController(
            SnippetService snippetService) {
        this.snippetService = snippetService;
    }

    private static String getOwnerId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }

    //Create from a file
    @PostMapping("/create/file")
    public ResponseEntity<String> createSnippetFromFile(
            @ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        Snippet snippet = getSnippetFromFile(fileDTO);
        String content = new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8);

        return createSnippetCommon(snippet, content, jwt);
    }

    @PostMapping("/create/text")
    public ResponseEntity<String> createSnippetFromText(
            @RequestBody RequestSnippetDTO snippetDTO,
            @AuthenticationPrincipal Jwt jwt) {

        Snippet snippet = getSnippetFromText(snippetDTO);
        String content = snippetDTO.content();

        return createSnippetCommon(snippet, content, jwt);
    }

    private Snippet getSnippetFromFile(RequestFileDTO fileDTO) {
        return new Converter().convertFileToSnippet(fileDTO);
    }

    private Snippet getSnippetFromText(RequestSnippetDTO fileDTO) {
        return new Converter().convertToSnippet(fileDTO);
    }

    private ResponseEntity<String> createSnippetCommon(Snippet snippet, String content, Jwt jwt) {
        ResponseEntity<String> response = snippetService.createUser(getOwnerId(jwt),AuthorizationActions.ALL,snippet.getId());
        if (!response.getStatusCode().is2xxSuccessful()) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        ValidationResult result =
                snippetService.createSnippet(snippet);
        if (!result.isValid()) {
            String errorMsg = String.format(
                    "Invalid Snippet: %s in line: %d, column: %d",
                    result.getMessage(),
                    result.getLine(),
                    result.getColumn()
            );
            return ResponseEntity.ok().body(errorMsg);
        }
        return snippetService.saveSnippetContent(snippet.getId(), content);
    }


    // Update from File
    @PutMapping("/{id}/update/file")
    public ResponseEntity<String> updateSnippetFromFile(
            @PathVariable UUID id,
            @ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        Snippet snippet = getSnippetFromFile(fileDTO);
        String content = new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8);
        String owner = getOwnerId(jwt);
        return updateSnippetCommon(id, snippet, content, owner);
    }

    //Update from text
    @PutMapping("/{id}/update/text")
    public ResponseEntity<String> updateSnippetFromText(
            @PathVariable UUID id,
            @RequestBody RequestSnippetDTO snippetDTO,
            @AuthenticationPrincipal Jwt jwt) {

        Snippet snippet = getSnippetFromText(snippetDTO);
        String content = snippetDTO.content();
        String owner = getOwnerId(jwt);
        return updateSnippetCommon(id,snippet, content, owner);
    }

    private ResponseEntity<String> updateSnippetCommon(UUID id,Snippet snippet, String content, String owner) {
        if (!snippetService.validateSnippet(
                        owner,id,AuthorizationActions.ALL)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        ValidationResult result = snippetService.updateSnippet(id, owner, snippet, content);
        if (!result.isValid()) {
            String errorMsg = String.format(
                    "Invalid Snippet: %s in line: %d, column: %d",
                    result.getMessage(),
                    result.getLine(),
                    result.getColumn()
            );
            return ResponseEntity.unprocessableEntity().body(errorMsg);
        }
        return ResponseEntity.ok(result.getMessage());
    }

    // Filter Snippets
    @PostMapping("/filter")
    public ResponseEntity<?> getSnippets(
            @AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) SnippetFilterDTO filterDTO) {
        try {
            if (filterDTO == null) {
                List<Snippet> snippets = snippetService.getAllSnippetsByOwner(getOwnerId(jwt), Property.BOTH);
                return ResponseEntity.ok(snippets.stream().map(s -> (new DataDTO(s,snippetService.findUserBySnippetId(s.getId(),jwt)))));
            }
            List<Snippet> snippets = snippetService.getSnippetsBy(getOwnerId(jwt), filterDTO);
            return ResponseEntity.ok(snippetService.filterValidSnippets(snippets, filterDTO, getOwnerId(jwt)).stream().map(s -> (new SnippetWithLintData(s.snippet(),s.valid(),snippetService.findUserBySnippetId(s.snippet().getId(),jwt)))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting the snippets: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Snippet> getSnippetById(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String ownerId = getOwnerId(jwt);
        Snippet snippets = snippetService.getSnippetById(UUID.fromString(id));
//        if (snippets == null || snippets.isEmpty()) {
//            return ResponseEntity.ok(Collections.emptyList());
//        }
//        List<Snippet> allowedSnippets = snippets.stream()
//                .filter(s -> !snippetService.validateSnippet(ownerId, s.getId(), AuthorizationActions.ALL)
//                        || !snippetService.validateSnippet(ownerId, s.getId(), AuthorizationActions.READ))
//                .toList();
        if(snippetService.validateSnippet(ownerId,UUID.fromString(id),AuthorizationActions.ALL)||
            snippetService.validateSnippet(ownerId,UUID.fromString(id),AuthorizationActions.READ)) {
            return ResponseEntity.status(HttpStatus.OK).body(snippets);
        }
        return ResponseEntity.ok(snippets);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSnippet(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = getOwnerId(jwt);
        try {
            logger.info("Snippet id {}",id);
            if (snippetService.validateSnippet(
                    userId,id,AuthorizationActions.ALL)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            Snippet snippet = snippetService.getSnippetById(id);
            if (snippet == null) {
                return ResponseEntity.status(404).body("Snippet not found or not accessible.");
            }
            ResponseEntity<String> deletedPermissions = snippetService.deleteSnippetUserAuthorization(id);
            if (!deletedPermissions.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(404).body("Permissions for snippet " + id + " could not be deleted.");
            }
            ResponseEntity<String> deleteTest = snippetService.deleteTest(id,jwt.getTokenValue());
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
    public ResponseEntity<String> shareSnippet(@AuthenticationPrincipal Jwt jwt, @RequestBody PermissionDTO shareSnippetDTO, @PathVariable UUID snippetId) {
        if (!snippetService.validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not Authorized to modify snippet");
        }
        return snippetService.createUser(shareSnippetDTO.userId(), AuthorizationActions.READ, snippetId);
    }
}

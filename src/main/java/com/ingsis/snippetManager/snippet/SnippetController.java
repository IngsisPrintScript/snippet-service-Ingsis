package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.snippet.dto.Converter;
import com.ingsis.snippetManager.snippet.dto.DataDTO;
import com.ingsis.snippetManager.snippet.dto.filters.Property;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestFileDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestSnippetDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.ShareDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetFilterDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetWithLintData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/snippet")
public class SnippetController {

    private final SnippetService snippetService;
    private static final Logger logger = LoggerFactory.getLogger(SnippetController.class);

    public SnippetController(SnippetService snippetService) {
        this.snippetService = snippetService;
    }

    private static String getOwnerId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }

    // Create from a file
    @PostMapping("/create/file")
    public ResponseEntity<String> createSnippetFromFile(@ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        Snippet snippet = getSnippetFromFile(fileDTO);
        String content = new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8);

        return createSnippetCommon(snippet, content);
    }

    @PostMapping("/create/text")
    public ResponseEntity<String> createSnippetFromText(@RequestBody RequestSnippetDTO snippetDTO) {
        Snippet snippet = getSnippetFromText(snippetDTO);
        logger.info("Snippet created {}", snippet.getId());
        String content = snippetDTO.content();
        ResponseEntity<String> str2 = createSnippetCommon(snippet, content);
        logger.info("Saved");
        return str2;
    }

    private Snippet getSnippetFromFile(RequestFileDTO fileDTO) {
        return new Converter().convertFileToSnippet(fileDTO);
    }

    private Snippet getSnippetFromText(RequestSnippetDTO fileDTO) {
        return new Converter().convertToSnippet(fileDTO);
    }
    String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNiNVozeWpZVnpXRkV6ZUhSUER5dyJ9."
            + "eyJpc3MiOiJodHRwczovL2Rldi02dWMwa3VhajdmaGdzeGVhLnVzLmF1dGgwLmNvbS8iLCJzdWIiOiJnb29n"
            + "bGUtb2F1dGgyfDExMzU4MzA1MjUyNDM3ODQ2MzU1NSIsImF1ZCI6WyJodHRwczovL3NuaXBwZXQtc2VhcmNo"
            + "LWluZ3NpcyIsImh0dHBzOi8vZGV2LTZ1YzBrdWFqN2ZoZ3N4ZWEudXMuYXV0aDAuY29tL3VzZXJpbmZvIl0s"
            + "ImlhdCI6MTc2NTEzMDI5NSwiZXhwIjoxNzY1MjE2Njk1LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWls"
            + "IiwiYXpwIjoiV2pnVW1vSEhnYXFyNzljR2duRXlxa0tQZGJLNVBwT3YifQ."
            + "X2FFzD8_I8I4pSWmAAjGjUBFggLv3SPzS7FlWXbFt7sEdryu3YuwhOG0mlO7fEwUI5ytV-h0XBM_AL9It2os"
            + "_6ba6qj_X6J1FOtk3dyfNWYK9b9hXGepc5W5Y9ZH0gFrGMXKhG9wVgiRTewJRw3KgASCwkQzSnPfn581N48Xa"
            + "u2pV-YP9cqUCFx1jr49Yx0kKCxmtLfH0xFOTinBcAtR0MI1fcoxrxPxvxquYlHRDdvBMQ4I2tKTByRzAVLc9x"
            + "WZUvSih-YtdU2dRG22Ai_50dc1y02aKJnU_C5l_4ncdh8XBW_3tdN6xVG9wPRpk-8A-2n0nhHrI2QT-fjE7XtBeA";

    private ResponseEntity<String> createSnippetCommon(Snippet snippet, String content) {
        ResponseEntity<String> response = snippetService.createUser("google-oauth2|113583052524378463555",
                AuthorizationActions.ALL, snippet.getId(),
                token);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        ValidationResult result = snippetService.createSnippet(snippet);
        logger.info("Asnippet");
        if (!result.isValid()) {
            String errorMsg = String.format("Invalid Snippet: %s in line: %d, column: %d", result.getMessage(),
                    result.getLine(), result.getColumn());
            return ResponseEntity.ok().body(errorMsg);
        }
        logger.info("Asnippet2");
        return snippetService.saveSnippetContent(snippet.getId(), content);
    }

    // Update from File
    @PutMapping("/{id}/update/file")
    public ResponseEntity<String> updateSnippetFromFile(@PathVariable UUID id, @ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        Snippet snippet = getSnippetFromFile(fileDTO);
        String content = new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8);
        String owner = getOwnerId(jwt);
        return updateSnippetCommon(id, snippet, content, owner, getToken(jwt));
    }

    // Update from text
    @PutMapping("/{id}/update/text")
    public ResponseEntity<String> updateSnippetFromText(@PathVariable UUID id,
            @RequestBody RequestSnippetDTO snippetDTO, @AuthenticationPrincipal Jwt jwt) {

        Snippet snippet = getSnippetFromText(snippetDTO);
        String content = snippetDTO.content();
        String owner = getOwnerId(jwt);
        return updateSnippetCommon(id, snippet, content, owner, getToken(jwt));
    }

    private ResponseEntity<String> updateSnippetCommon(UUID id, Snippet snippet, String content, String owner,
            String jwt) {
        if (snippetService.validateSnippet(owner, id, AuthorizationActions.ALL, jwt)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        ValidationResult result = snippetService.updateSnippet(id, owner, snippet, content);
        if (!result.isValid()) {
            String errorMsg = String.format("Invalid Snippet: %s in line: %d, column: %d", result.getMessage(),
                    result.getLine(), result.getColumn());
            return ResponseEntity.unprocessableEntity().body(errorMsg);
        }
        return ResponseEntity.ok(result.getMessage());
    }

    // Filter Snippets
    @PostMapping("/filter")
    public ResponseEntity<?> getSnippets(@AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) SnippetFilterDTO filterDTO) {
        try {
            if (filterDTO == null) {
                List<Snippet> snippets = snippetService.getAllSnippetsByOwner(getOwnerId(jwt), Property.BOTH,
                        getToken(jwt));
                return ResponseEntity.ok(
                        snippets.stream().map(s -> (new DataDTO(s, snippetService.findUserBySnippetId(s.getId(), jwt),
                                snippetService.downloadSnippetContent(s.getId())))));
            }
            List<Snippet> snippets = snippetService.getSnippetsBy(getOwnerId(jwt), filterDTO, getToken(jwt));
            return ResponseEntity.ok(snippetService.filterValidSnippets(snippets, filterDTO, getOwnerId(jwt)).stream()
                    .map(s -> (new SnippetWithLintData(s.snippet(), s.valid(),
                            snippetService.findUserBySnippetId(s.snippet().getId(), jwt),
                            snippetService.downloadSnippetContent(s.snippet().getId())))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting the snippets: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Snippet> getSnippetById(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        String ownerId = getOwnerId(jwt);
        Snippet snippets = snippetService.getSnippetById(UUID.fromString(id));
        // if (snippets == null || snippets.isEmpty()) {
        // return ResponseEntity.ok(Collections.emptyList());
        // }
        // List<Snippet> allowedSnippets = snippets.stream()
        // .filter(s -> !snippetService.validateSnippet(ownerId, s.getId(),
        // AuthorizationActions.ALL)
        // || !snippetService.validateSnippet(ownerId, s.getId(),
        // AuthorizationActions.READ))
        // .toList();
        if (snippetService.validateSnippet(ownerId, UUID.fromString(id), AuthorizationActions.ALL, getToken(jwt))
                || snippetService.validateSnippet(ownerId, UUID.fromString(id), AuthorizationActions.READ,
                        getToken(jwt))) {
            return ResponseEntity.status(HttpStatus.OK).body(snippets);
        }
        return ResponseEntity.ok(snippets);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSnippet(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = getOwnerId(jwt);
        try {
            logger.info("Snippet id {}", id);
            if (snippetService.validateSnippet(userId, id, AuthorizationActions.ALL, getToken(jwt))) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            Snippet snippet = snippetService.getSnippetById(id);
            if (snippet == null) {
                return ResponseEntity.status(404).body("Snippet not found or not accessible.");
            }
            ResponseEntity<String> deletedPermissions = snippetService.deleteSnippetUserAuthorization(id,
                    getToken(jwt));
            if (!deletedPermissions.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(404).body("Permissions for snippet " + id + " could not be deleted.");
            }
            ResponseEntity<String> deleteTest = snippetService.deleteTest(id, jwt.getTokenValue());
            if (!deleteTest.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(404).body("Permissions for snippet " + id + " could not be deleted.");
            }
            return snippetService.deleteSnippet(id);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting snippet: " + e.getMessage());
        }
    }

    @PutMapping("/{snippetId}/share")
    public ResponseEntity<String> shareSnippet(@AuthenticationPrincipal Jwt jwt, @RequestBody ShareDTO shareSnippetDTO,
            @PathVariable UUID snippetId) {
        if (!snippetService.validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL, getToken(jwt))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not Authorized to modify snippet");
        }
        return snippetService.createUser(shareSnippetDTO.userId(), AuthorizationActions.READ, snippetId,
                getOwnerId(jwt));
    }

    private String getToken(Jwt token) {
        return token.getTokenValue();
    }
}

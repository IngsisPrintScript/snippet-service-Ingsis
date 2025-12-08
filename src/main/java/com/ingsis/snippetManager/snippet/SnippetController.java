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
        String ownerId = getOwnerId(jwt);
        String token = getToken(jwt);

        return createSnippetCommon(snippet, content, ownerId, token);
    }

    @PostMapping("/create/text")
    public ResponseEntity<String> createSnippetFromText(@RequestBody RequestSnippetDTO snippetDTO,
            @AuthenticationPrincipal Jwt jwt) {
        Snippet snippet = getSnippetFromText(snippetDTO);
        logger.info("Snippet created {}", snippet.getId());
        String content = snippetDTO.content();
        String ownerId = getOwnerId(jwt);
        String token = getToken(jwt);
        ResponseEntity<String> str2 = createSnippetCommon(snippet, content, ownerId, token);
        logger.info("Saved");
        return str2;
    }

    private Snippet getSnippetFromFile(RequestFileDTO fileDTO) {
        return new Converter().convertFileToSnippet(fileDTO);
    }

    private Snippet getSnippetFromText(RequestSnippetDTO fileDTO) {
        return new Converter().convertToSnippet(fileDTO);
    }
    private ResponseEntity<String> createSnippetCommon(Snippet snippet, String content, String ownerId, String token) {
        ResponseEntity<String> response = snippetService.createUser(ownerId,
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
            logger.info("Getting snippets for user: {}", getOwnerId(jwt));
            if (filterDTO == null) {
                List<Snippet> snippets = snippetService.getAllSnippetsByOwner(getOwnerId(jwt), Property.BOTH,
                        getToken(jwt));
                logger.info("Found {} snippets", snippets.size());
                List<DataDTO> result = snippets.stream().map(s -> {
                    try {
                        String userName = snippetService.findUserBySnippetId(s.getId(), jwt);
                        String content = snippetService.downloadSnippetContent(s.getId());
                        return new DataDTO(s, userName, content);
                    } catch (Exception e) {
                        logger.error("Error processing snippet {}: {}", s.getId(), e.getMessage(), e);
                        // Return snippet with default values if there's an error
                        return new DataDTO(s, "Unknown", "");
                    }
                }).toList();
                return ResponseEntity.ok(result);
            }
            List<Snippet> snippets = snippetService.getSnippetsBy(getOwnerId(jwt), filterDTO, getToken(jwt));
            List<SnippetWithLintData> result = snippetService.filterValidSnippets(snippets, filterDTO, getOwnerId(jwt)).stream()
                    .map(s -> {
                        try {
                            String userName = snippetService.findUserBySnippetId(s.snippet().getId(), jwt);
                            String content = snippetService.downloadSnippetContent(s.snippet().getId());
                            return new SnippetWithLintData(s.snippet(), s.valid(), userName, content);
                        } catch (Exception e) {
                            logger.error("Error processing snippet {}: {}", s.snippet().getId(), e.getMessage(), e);
                            // Return snippet with default values if there's an error
                            return new SnippetWithLintData(s.snippet(), s.valid(), "Unknown", "");
                        }
                    }).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting snippets: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error getting the snippets: " + e.getMessage() + 
                    (e.getCause() != null ? " - Cause: " + e.getCause().getMessage() : ""));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataDTO> getSnippetById(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        try {
            String ownerId = getOwnerId(jwt);
            Snippet snippet = snippetService.getSnippetById(UUID.fromString(id));
            if (snippet == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Verificar permisos - validateSnippet retorna true si NO tiene permisos
            // Si no tiene permisos ALL Y no tiene permisos READ, entonces está prohibido
            boolean hasAllPermission = !snippetService.validateSnippet(ownerId, UUID.fromString(id), AuthorizationActions.ALL, getToken(jwt));
            boolean hasReadPermission = !snippetService.validateSnippet(ownerId, UUID.fromString(id), AuthorizationActions.READ, getToken(jwt));
            
            if (!hasAllPermission && !hasReadPermission) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Obtener el contenido y el owner
            String userName = "Unknown";
            String content = "";
            try {
                userName = snippetService.findUserBySnippetId(snippet.getId(), jwt);
                content = snippetService.downloadSnippetContent(snippet.getId());
            } catch (Exception e) {
                logger.warn("Error getting snippet details for {}: {}", id, e.getMessage());
                // Continuar con valores por defecto
            }
            
            DataDTO dataDTO = new DataDTO(snippet, userName, content);
            return ResponseEntity.ok(dataDTO);
        } catch (Exception e) {
            logger.error("Error getting snippet by id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
            // TODO: Descomentar cuando el microservicio de tests esté disponible
            // ResponseEntity<String> deleteTest = snippetService.deleteTest(id, jwt.getTokenValue());
            // if (!deleteTest.getStatusCode().is2xxSuccessful()) {
            //     return ResponseEntity.status(404).body("Permissions for snippet " + id + " could not be deleted.");
            // }
            return snippetService.deleteSnippet(id);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting snippet: " + e.getMessage());
        }
    }

    @PutMapping("/{snippetId}/share")
    public ResponseEntity<String> shareSnippet(@AuthenticationPrincipal Jwt jwt, @RequestBody ShareDTO shareSnippetDTO,
            @PathVariable UUID snippetId) {
        try {
            // validateSnippet retorna true si el usuario NO tiene permisos
            // Por lo tanto, si retorna true, debemos denegar el acceso
            boolean hasNoPermissions = snippetService.validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL, getToken(jwt));
            
            if (hasNoPermissions) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not Authorized to modify snippet");
            }
            
            return snippetService.createUser(shareSnippetDTO.userId(), AuthorizationActions.READ, snippetId,
                    getToken(jwt));
        } catch (Exception e) {
            logger.error("SnippetController.shareSnippet - Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sharing snippet: " + e.getMessage());
        }
    }

    private String getToken(Jwt token) {
        return token.getTokenValue();
    }
}

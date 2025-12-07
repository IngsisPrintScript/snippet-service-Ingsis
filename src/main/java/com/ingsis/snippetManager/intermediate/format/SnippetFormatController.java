package com.ingsis.snippetManager.intermediate.format;

import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import com.ingsis.snippetManager.redis.format.dto.SnippetFormatStatus;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetController;
import com.ingsis.snippetManager.snippet.dto.format.GetSnippetFormatStatusDTO;
import com.ingsis.snippetManager.snippet.dto.format.SnippetValidFormatDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.CreateDTO;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.UpdateDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/format")
public class SnippetFormatController {

    private final FormatService formatService;
    private final SnippetController snippetController;
    private final UserPermissionService userPermissionService;

    public SnippetFormatController(FormatService formatService, SnippetController snippetController,
            UserPermissionService userPermissionService) {
        this.formatService = formatService;
        this.snippetController = snippetController;
        this.userPermissionService = userPermissionService;
    }

    @GetMapping("/{snippetId}")
    public ResponseEntity<SnippetFormatStatus> formatSnippet(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String snippetId) {
        try {
            String ownerId = getString(jwt);
            Snippet contentDTO = (snippetController.getSnippetById(jwt, snippetId)).getBody();
            if (contentDTO == null) {
                return ResponseEntity.badRequest().build();
            }
            SnippetFormatStatus passes = formatService.formatContent(contentDTO, ownerId);
            return ResponseEntity.ok(passes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(SnippetFormatStatus.FAILED);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateFormatRules(@AuthenticationPrincipal Jwt jwt,
            @RequestBody List<UpdateDTO> rulesDTO) {
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
        List<UUID> snippetsOwner = userPermissionService.getUserSnippets(userId, AuthorizationActions.ALL,
                getToken(jwt));
        List<Snippet> snippets = formatService.getAllSnippetByOwner(snippetsOwner);
        List<SnippetValidFormatDTO> response = snippets.stream()
                .map(snippet -> new SnippetValidFormatDTO(snippet, snippet.getFormatStatus())).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<List<GetSnippetFormatStatusDTO>> getFormatStatus(@AuthenticationPrincipal Jwt jwt) {
        String userId = getString(jwt);
        List<UUID> snippetsOwner = userPermissionService.getUserSnippets(userId, AuthorizationActions.ALL,
                getToken(jwt));
        List<Snippet> snippets = formatService.getAllSnippetByOwner(snippetsOwner);
        return ResponseEntity.ok(snippets.stream()
                .map(snippet -> new GetSnippetFormatStatusDTO(snippet.getId(), snippet.getFormatStatus())).toList());
    }

    private static String getString(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }
    private String getToken(Jwt token) {
        return token.getTokenValue();
    }
}

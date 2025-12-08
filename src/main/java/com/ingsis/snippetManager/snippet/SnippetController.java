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
import java.util.NoSuchElementException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
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

    private Snippet getSnippetFromFile(RequestFileDTO fileDTO) {
        return new Converter().convertFileToSnippet(fileDTO);
    }

    private Snippet getSnippetFromText(RequestSnippetDTO fileDTO) {
        return new Converter().convertToSnippet(fileDTO);
    }

    private ResponseEntity<String> createSnippetCommon(Snippet snippet, String content, Jwt jwt) {
        try {
            return ResponseEntity.ok(snippetService.createSnippet(snippet, jwt, AuthorizationActions.ALL, content));
        } catch (NoSuchElementException a) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(a.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<String> updateSnippetCommon(UUID id, Snippet snippet, String content, Jwt jwt) {
        try {
            return ResponseEntity.ok(snippetService.updateSnippet(id, jwt, snippet, content));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception a) {
            return ResponseEntity.badRequest().body(a.getMessage());
        }
    }

    @NotNull
    private ResponseEntity<List<SnippetWithLintData>> getAll(Jwt jwt) {
        List<Snippet> snippets = snippetService.getAllSnippetsByOwner(jwt, Property.BOTH);
        return ResponseEntity.ok(snippets.stream()
                .map(s -> new SnippetWithLintData(s, s.getLintStatus(),
                        snippetService.findUserBySnippetId(s.getId(), jwt),
                        snippetService.downloadSnippetContent(s.getId())))
                .toList());
    }

    @PostMapping("/create/file")
    public ResponseEntity<String> createSnippetFromFile(@ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        Snippet snippet = getSnippetFromFile(fileDTO);
        String content = new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8);
        return createSnippetCommon(snippet, content, jwt);
    }

    @PostMapping("/create/text")
    public ResponseEntity<String> createSnippetFromText(@RequestBody RequestSnippetDTO snippetDTO,
            @AuthenticationPrincipal Jwt jwt) {
        Snippet snippet = getSnippetFromText(snippetDTO);
        String content = snippetDTO.content();
        return createSnippetCommon(snippet, content, jwt);
    }

    @PutMapping("/{id}/update/file")
    public ResponseEntity<String> updateSnippetFromFile(@PathVariable UUID id, @ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        Snippet snippet = getSnippetFromFile(fileDTO);
        String content = new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8);
        return updateSnippetCommon(id, snippet, content, jwt);
    }

    @PutMapping("/{id}/update/text")
    public ResponseEntity<String> updateSnippetFromText(@PathVariable UUID id,
            @RequestBody RequestSnippetDTO snippetDTO, @AuthenticationPrincipal Jwt jwt) {
        Snippet snippet = getSnippetFromText(snippetDTO);
        String content = snippetDTO.content();
        return updateSnippetCommon(id, snippet, content, jwt);
    }

    @PostMapping("/filter")
    public ResponseEntity<List<SnippetWithLintData>> getSnippets(@AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) SnippetFilterDTO filterDTO) {
        try {
            if (filterDTO == null) {
                return getAll(jwt);
            }
            List<Snippet> snippets = snippetService.getSnippetsWithFilter(filterDTO, jwt);
            return ResponseEntity.ok(snippetService.filterValidSnippets(snippets, filterDTO, jwt).stream()
                    .map(s -> (new SnippetWithLintData(s.snippet(), s.valid(),
                            snippetService.findUserBySnippetId(s.snippet().getId(), jwt),
                            snippetService.downloadSnippetContent(s.snippet().getId()))))
                    .toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/All")
    public ResponseEntity<List<SnippetWithLintData>> getAllSnippets(@AuthenticationPrincipal Jwt jwt) {
        return getAll(jwt);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataDTO> getSnippetById(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        try {
            return ResponseEntity.ok(snippetService.getSnippetById(UUID.fromString(id), jwt));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSnippet(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return snippetService.deleteSnippet(id, jwt);
    }

    @PutMapping("/{snippetId}/share")
    public ResponseEntity<String> shareSnippet(@AuthenticationPrincipal Jwt jwt, @RequestBody ShareDTO shareSnippetDTO,
            @PathVariable UUID snippetId) {
        return snippetService.shareSnippet(snippetId, jwt, shareSnippetDTO);
    }
}

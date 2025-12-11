package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.intermediate.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.snippet.dto.Converter;
import com.ingsis.snippetManager.snippet.dto.DataDTO;
import com.ingsis.snippetManager.snippet.dto.PaginatedSnippets;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestFileDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestSnippetDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.ShareDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetFilterDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetResponseDTO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/snippet")
public class SnippetController {

    private final SnippetService snippetService;
    private final Converter converter;

    public SnippetController(SnippetService snippetService, Converter converter) {
        this.snippetService = snippetService;
        this.converter = converter;
    }

    private Snippet getSnippetFromFile(RequestFileDTO fileDTO) {
        return converter.convertFileToSnippet(fileDTO);
    }

    private Snippet getSnippetFromText(RequestSnippetDTO fileDTO) {
        return converter.convertToSnippet(fileDTO);
    }

    private ResponseEntity<SnippetResponseDTO> createSnippetCommon(Snippet snippet, String content, Jwt jwt) {
        try {

            return ResponseEntity.ok(snippetService.createSnippet(snippet, jwt, AuthorizationActions.ALL, content));
        } catch (NoSuchElementException a) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private ResponseEntity<SnippetResponseDTO> updateSnippetCommon(UUID id, Snippet snippet, String content, Jwt jwt) {
        try {
            return ResponseEntity.ok(snippetService.updateSnippet(id, jwt, snippet, content));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception a) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create/file")
    public ResponseEntity<SnippetResponseDTO> createSnippetFromFile(@ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        Snippet snippet = getSnippetFromFile(fileDTO);
        String content = new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8);
        return createSnippetCommon(snippet, content, jwt);
    }

    @PostMapping("/create/text")
    public ResponseEntity<SnippetResponseDTO> createSnippetFromText(@RequestBody RequestSnippetDTO snippetDTO,
            @AuthenticationPrincipal Jwt jwt) {
        Snippet snippet = getSnippetFromText(snippetDTO);
        String content = snippetDTO.content();
        return createSnippetCommon(snippet, content, jwt);
    }

    @PutMapping("/{id}/update/file")
    public ResponseEntity<SnippetResponseDTO> updateSnippetFromFile(@PathVariable UUID id, @ModelAttribute RequestFileDTO fileDTO,
            @AuthenticationPrincipal Jwt jwt) throws IOException {
        Snippet snippet = getSnippetFromFile(fileDTO);
        String content = new String(fileDTO.file().getBytes(), StandardCharsets.UTF_8);
        return updateSnippetCommon(id, snippet, content, jwt);
    }

    @PutMapping("/{id}/update/text")
    public ResponseEntity<SnippetResponseDTO> updateSnippetFromText(@PathVariable UUID id,
            @RequestBody RequestSnippetDTO snippetDTO, @AuthenticationPrincipal Jwt jwt) {
        Snippet snippet = getSnippetFromText(snippetDTO);
        String content = snippetDTO.content();
        return updateSnippetCommon(id, snippet, content, jwt);
    }

    @PostMapping("/filter")
    public ResponseEntity<PaginatedSnippets> getSnippets(@AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) SnippetFilterDTO filterDTO, @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
        try {
            return ResponseEntity.ok(snippetService.getFilteredSnippets(filterDTO, page, pageSize, jwt));
        } catch (Exception e) {
            throw new NoSuchElementException(e.getMessage());
        }
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

    @GetMapping("/{snippetId}/download")
    public ResponseEntity<byte[]> downloadSnippet(@PathVariable UUID snippetId,
            @RequestParam(defaultValue = "original") String version, @AuthenticationPrincipal Jwt jwt) {
        try {
            return ResponseEntity.ok(snippetService.download(jwt, version, snippetId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{snippetId}/execute")
    public ResponseEntity<RunSnippetResponseDTO> executeSnippet(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snippetId, @RequestBody List<String> inputs) {
        return ResponseEntity.ok(snippetService.execute(snippetId, jwt, inputs));
    }
}

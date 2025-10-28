package com.ingsis.snippetManager.snippet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ingsis.snippetManager.intermediate.lint.LintingService;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.controllers.filters.Order;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.SnippetValidLintingDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetFilterDTO;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SnippetService {

    private final SnippetRepo repository;
    private final LintingService lintingService;
    private final AssetService assetService;
    private static final Logger logger = LoggerFactory.getLogger(SnippetService.class);
    // private final PrintScriptParser parser;

    // PrintScriptParser parser to add
    public SnippetService(
            SnippetRepo repository, AssetService assetService, LintingService lintingService) {
        this.repository = repository;
        this.assetService = assetService;
        this.lintingService = lintingService;
        // this.parser = parser;
    }

    public ValidationResult createSnippet(Snippet snippet) {
        // ValidationResult result = parser.validate(snippet.getContent()); To DO
        ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
        if (result.isValid()) {
            Snippet newSnippet = new Snippet(
                    snippet.getName(),
                    snippet.getDescription(),
                    snippet.getLanguage(),
                    snippet.getVersion());
            repository.save(newSnippet);
        }
        return result;
    }

    public ValidationResult updateSnippet(UUID id, Snippet updatedSnippet, String content) {
        Snippet existingSnippet = repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));
        // ValidationResult result = parserClient.validate(content);
        ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
        if (result.isValid()) {
            // Update fields of the existing snippet
            existingSnippet.setName(updatedSnippet.getName());
            existingSnippet.setDescription(updatedSnippet.getDescription());
            existingSnippet.setLanguage(updatedSnippet.getLanguage());
            existingSnippet.setVersion(updatedSnippet.getVersion());
            existingSnippet.setContentUrl(updatedSnippet.getContentUrl());
            repository.save(existingSnippet);
            // Persist content to storage
            uploadSnippetContent(id, content);
        }
        return result;
    }

    public List<Snippet> getAllSnippetsByOwner(String snippetOwnerId) {
        return repository.findAll();
    }

    public List<Snippet> getSnippetsBy(String snippetOwnerId, SnippetFilterDTO filter) {
        Sort sort = Sort.unsorted();
        if (filter.sortBy() != null && filter.order() != null) {
            Sort.Direction direction =
                    (filter.order() == Order.DESC) ? Sort.Direction.DESC : Sort.Direction.ASC;

            String sortField = switch (filter.sortBy()) {
                case LANGUAGE -> "language";
                case NAME -> "name";
                case VALID -> "";
            };

            if (!sortField.isEmpty()) {
                sort = Sort.by(direction, sortField);
            }
        }

        boolean noFilters = (filter.name() == null || filter.name().isEmpty()) &&
                (filter.language() == null || filter.language().isEmpty()) &&
                filter.valid() == null &&
                filter.property() == null;

        if (noFilters) {
            return repository.findAll();
        }

        String nameFilter = (filter.name() != null && !filter.name().isEmpty()) ? filter.name() : null;
        String languageFilter = (filter.language() != null && !filter.language().isEmpty()) ? filter.language() : null;
        String relationFilter = filter.property() != null ? filter.property().name() : null;

        return repository.findFilteredSnippets(
                nameFilter,
                languageFilter,
                sort
        );
    }

    public List<SnippetValidLintingDTO> filterValidSnippets(List<Snippet> snippets, SnippetFilterDTO filterDTO, String snippetOwnerId) {
        List<SnippetValidLintingDTO> validatedSnippets = new ArrayList<>();
        for (Snippet snippet : snippets) {
            SnippetLintStatus linting = lintingService.validLinting(assetService.getSnippet(snippet.getId()).getBody(), snippetOwnerId);
            if (filterDTO.valid() != null) {
                if (filterDTO.valid() == linting) {
                    validatedSnippets.add(new SnippetValidLintingDTO(snippet, linting));
                }
            } else {
                validatedSnippets.add(new SnippetValidLintingDTO(snippet, linting));
            }
        }
        return validatedSnippets;
    }

    public Snippet getSnippetById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));
    }

    public String downloadSnippetContent(UUID snippetId) {
        logger.info("Downloading snippet content for snippet with id: {}", snippetId);
        Snippet snippet = repository.findById(snippetId)
                .orElseThrow(() -> new RuntimeException("Snippet not found"));
        logger.info("Snippet with id {}", snippet.getId());
        String content = assetService.getSnippet(snippetId).getBody();
        logger.info("Content bytes: {}", content);
        return content;
    }

    public void uploadSnippetContent(UUID snippetId , String content) {
        if (content == null) {
            content = "";
        }
        assetService.saveSnippet(snippetId, content);
    }

    public ResponseEntity<String> deleteSnippet(UUID snippetId) {
        Snippet snippet = repository.findById(snippetId)
                .orElseThrow(() -> new RuntimeException("Snippet not found"));
        repository.delete(snippet);
        try {
            assetService.deleteSnippet(snippetId);
            return ResponseEntity.ok("Snippet deleted");
        } catch (Exception e) {
            logger.warn("Failed to delete snippet content from storage for snippet {}: {}", snippetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete snippet content");
        }
    }
}

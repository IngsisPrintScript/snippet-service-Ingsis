package com.ingsis.snippetManager.snippet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ingsis.snippetManager.intermediate.LintingService;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.StorageService;
import com.ingsis.snippetManager.snippet.controllers.filters.Order;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.SnippetValidLintingDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetFilterDTO;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class SnippetService {

  private final SnippetRepo repository;
  private final LintingService lintingService;
  private final StorageService storageService;
  // private final PrintScriptParser parser;

  // PrintScriptParser parser to add
  public SnippetService(
      SnippetRepo repository,StorageService storageService, LintingService lintingService) {
    this.repository = repository;
    this.storageService = storageService;
    this.lintingService = lintingService;
    // this.parser = parser;
  }

  public ValidationResult createSnippet(Snippet snippet, String snippetOwnerId) {
    // ValidationResult result = parser.validate(snippet.getContent()); To DO
    ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
    if (result.isValid()) {
      repository.save(
          new Snippet(
              snippet.getName(),
              snippet.getDescription(),
              snippet.getLanguage(),
              snippet.getVersion(),
              snippet.getContentUrl(),
              snippetOwnerId));
    }
    return result;
  }

  public ValidationResult updateSnippet(UUID id, Snippet updatedSnippet, String snippetOwnerId) {
    repository
        .findByIdAndSnippetOwnerId(id, snippetOwnerId)
        .orElseThrow(() -> new RuntimeException("Snippet not found"));
    // ValidationResult result = parserClient.validate(updatedSnippet.content());
    ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
    if (result.isValid()) {
      repository.save(
          new Snippet(
              updatedSnippet.getName(),
              updatedSnippet.getDescription(),
              updatedSnippet.getLanguage(),
              updatedSnippet.getVersion(),
              updatedSnippet.getContentUrl(),
              updatedSnippet.getSnippetOwnerId()));
    }
    return result;
  }

  public List<Snippet> getAllSnippetsByOwner(String snippetOwnerId) {
    return repository.findAllAccessibleByUserId(snippetOwnerId);
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
      return repository.findAllAccessibleByUserId(snippetOwnerId, sort);
    }

    String nameFilter = (filter.name() != null && !filter.name().isEmpty()) ? filter.name() : null;
    String languageFilter = (filter.language() != null && !filter.language().isEmpty()) ? filter.language() : null;
    String relationFilter = filter.property() != null ? filter.property().name() : null;

    return repository.findFilteredSnippets(
            snippetOwnerId,
            relationFilter,
            nameFilter,
            languageFilter,
            sort
    );
  }

  public List<SnippetValidLintingDTO> filterValidSnippets(List<Snippet> snippets, SnippetFilterDTO filterDTO) {
    List<SnippetValidLintingDTO> validatedSnippets= new ArrayList<>();
    for (Snippet snippet : snippets) {
        boolean linting = lintingService.validLinting(filterDTO.lintingRuleId(),snippet.getId());
        if(filterDTO.valid() != null){
            if(linting == filterDTO.valid()) {
            validatedSnippets.add(new SnippetValidLintingDTO(snippet,linting));
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
    Snippet snippet = repository.findById(snippetId)
            .orElseThrow(() -> new RuntimeException("Snippet not found"));

    String contentUrl = snippet.getContentUrl();
    if (contentUrl == null || contentUrl.isEmpty()) {
      return "";
    }

    byte[] contentBytes = storageService.download(contentUrl);
    return new String(contentBytes, StandardCharsets.UTF_8);
  }

  public String uploadSnippetContent(String folder, String fileNamePrefix, String content) {
    if (content == null) {
      content = "";
    }

    String blobName = UUID.randomUUID() + "-" + fileNamePrefix;

    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

    return storageService.upload(folder, blobName, contentBytes);
  }
}

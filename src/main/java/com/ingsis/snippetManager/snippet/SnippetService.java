package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.ToMove.intermediate.UserClientService;
import com.ingsis.snippetManager.ToMove.snippet.dto.FilterDTO;
import com.ingsis.snippetManager.ToMove.snippetShare.SnippetShare;
import com.ingsis.snippetManager.ToMove.snippetShare.SnippetShareRepo;
import com.ingsis.snippetManager.ToMove.userStory5Filters.Order;
import com.ingsis.snippetManager.ToMove.userStory5Filters.Relation;
import com.ingsis.snippetManager.ToMove.validationResult.ValidationResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class SnippetService {

  private final SnippetRepo repository;
  private final SnippetShareRepo snippetShareRepo;
  private final UserClientService userClientService;

  // private final PrintScriptParser parser;

  // PrintScriptParser parser to add
  public SnippetService(
      SnippetRepo repository, SnippetShareRepo snippetRepo, UserClientService userClientService) {
    this.repository = repository;
    this.snippetShareRepo = snippetRepo;
    this.userClientService = userClientService;
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

  public List<Snippet> getSnippetsBy(String snippetOwnerId, FilterDTO filter) {
    Sort sort = Sort.unsorted();
    if (filter.sortBy() != null && filter.order() != null) {
      Sort.Direction direction =
          (filter.order() == Order.DESC) ? Sort.Direction.DESC : Sort.Direction.ASC;

      String sortField = switch (filter.sortBy()) {
          case LANGUAGE -> "language";
          case VALID -> "validationResult.valid";
          case NAME -> "name";
      };

      sort = Sort.by(direction, sortField);
    }

    Relation relation = filter.property();

    return repository.findFilteredSnippets(
        snippetOwnerId,
        relation != null ? relation.name() : null,
        filter.name(),
        filter.language(),
        filter.valid(),
        sort);
  }

  public Snippet findByIdAndSnippetOwnerId(UUID id, String snippetOwnerId) {
    return repository
        .findByIdAndSnippetOwnerId(id, snippetOwnerId)
        .orElseThrow(() -> new RuntimeException("Snippet not found"));
  }

  public SnippetShare shareSnippet(UUID snippetId, String sharedWithUserId, String snippetOwnerId) {
    Snippet snippet =
        repository
            .findByIdAndSnippetOwnerId(snippetId, snippetOwnerId)
            .orElseThrow(() -> new RuntimeException("Snippet not found"));
    if (!userClientService.userExists(sharedWithUserId)) {
      throw new RuntimeException("User not found");
    }
    SnippetShare share =
        new SnippetShare(
            new Snippet(
                snippet.getName(),
                snippet.getDescription(),
                snippet.getLanguage(),
                snippet.getVersion(),
                snippet.getContentUrl(),
                snippetOwnerId),
            sharedWithUserId,
            true);
    snippet.getSnippetShare().add(share);
    return snippetShareRepo.save(share);
  }

  public Snippet downloadOriginalSnippet(String snippetOwnerId, UUID snippetId) {
    return repository
        .findByIdAndSnippetOwnerId(snippetId, snippetOwnerId)
        .orElseThrow(() -> new RuntimeException("Snippet not found"));
  }

  public Snippet downloadFormattedSnippet(String snippetOwnerId, UUID snippetId) {
    return repository
        .findByIdAndSnippetOwnerId(snippetId, snippetOwnerId)
        .orElseThrow(() -> new RuntimeException("Snippet not found"));
  }
}

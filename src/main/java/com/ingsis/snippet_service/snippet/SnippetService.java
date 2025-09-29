package com.ingsis.snippet_service.snippet;

import com.ingsis.snippet_service.snippetShare.SnippetShare;
import com.ingsis.snippet_service.snippetShare.SnippetShareRepo;
import com.ingsis.snippet_service.validationResult.ValidationResult;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SnippetService {

  private final SnippetRepo repository;
  private final SnippetShareRepo snippetShareRepo;

  // private final PrintScriptParser parser;

  // PrintScriptParser parser to add
  public SnippetService(SnippetRepo repository, SnippetShareRepo snippetRepo) {
    this.repository = repository;
    this.snippetShareRepo = snippetRepo;
    // this.parser = parser;
  }

  public Snippet createSnippet(Snippet snippet, String snippetOwnerId) {
    // ValidationResult result = parser.validate(snippet.getContent()); To DO
    ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
    snippet.setValidationResult(result);
    snippet.setSnippetOwnerId(snippetOwnerId);
    return repository.save(snippet);
  }

  public Snippet updateSnippet(UUID id, Snippet updatedSnippet, String snippetOwnerId) {
    Snippet existing =
        repository
            .findByIdAndSnippetOwnerId(id, snippetOwnerId)
            .orElseThrow(() -> new RuntimeException("Snippet not found"));

    existing.setName(updatedSnippet.getName());
    existing.setDescription(updatedSnippet.getDescription());
    existing.setLanguage(updatedSnippet.getLanguage());
    existing.setVersion(updatedSnippet.getVersion());
    existing.setContent(updatedSnippet.getContent());

    // ValidationResult result = parserClient.validate(existing);
    ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
    existing.setValidationResult(result);

    return repository.save(existing);
  }

  public List<Snippet> getAllSnippetsByOwner(String snippetOwnerId) {
    return repository.getAllSnippetsByOwner(snippetOwnerId);
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
    SnippetShare share = new SnippetShare();
    share.setSnippet(snippet);
    share.setSharedWithUserId(sharedWithUserId);
    share.setCanRead(true);

    snippet.getSnippetShare().add(share);
    return snippetShareRepo.save(share);
  }

  List<Snippet> getAccessibleSnippets(String userId) {
    return repository.findAllAccessibleByUserId(userId);
  }
}

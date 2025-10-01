package com.ingsis.snippet_service.snippet;

import com.ingsis.snippet_service.intermediate.UserClientService;
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
  private final UserClientService userClientService;


  // private final PrintScriptParser parser;

  // PrintScriptParser parser to add
  public SnippetService(SnippetRepo repository, SnippetShareRepo snippetRepo,  UserClientService userClientService) {
    this.repository = repository;
    this.snippetShareRepo = snippetRepo;
    this.userClientService = userClientService;
    // this.parser = parser;
  }

  public Snippet createSnippet(Snippet snippet, String snippetOwnerId) {
    // ValidationResult result = parser.validate(snippet.getContent()); To DO
    ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
    return repository.save(new Snippet(snippet.getName(),snippet.getDescription(),
            snippet.getLanguage(),snippet.getVersion(),
            snippet.getContent(),snippetOwnerId,result));
  }

  public Snippet updateSnippet(UUID id, Snippet updatedSnippet, String snippetOwnerId) {
    Snippet existing =
        repository
            .findByIdAndSnippetOwnerId(id, snippetOwnerId)
            .orElseThrow(() -> new RuntimeException("Snippet not found"));
    // ValidationResult result = parserClient.validate(existing);
    ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
    return repository.save(new Snippet(updatedSnippet.getName(),
            updatedSnippet.getDescription(),updatedSnippet.getLanguage(),
            updatedSnippet.getVersion(),updatedSnippet.getContent(),
            updatedSnippet.getSnippetOwnerId(),result));
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
    if(!userClientService.userExists(sharedWithUserId)){
        throw new RuntimeException("User not found");
    }
    SnippetShare share = new SnippetShare(new Snippet(snippet.getName(),
            snippet.getDescription(),snippet.getLanguage(),
            snippet.getVersion(),snippet.getContent(),snippetOwnerId,
            snippet.getValidationResult()), sharedWithUserId,true);
    snippet.getSnippetShare().add(share);
    return snippetShareRepo.save(share);
  }

  public Snippet downloadOriginalSnippet(String snippetOwnerId, UUID snippetId) {
     return repository.findByIdAndSnippetOwnerId(snippetId, snippetOwnerId).orElseThrow(() -> new RuntimeException("Snippet not found"));

  }
  public Snippet downloadFormattedSnippet(String snippetOwnerId, UUID snippetId) {
    return repository.findByIdAndSnippetOwnerId(snippetId, snippetOwnerId).orElseThrow(() -> new RuntimeException("Snippet not found"));
  }
}

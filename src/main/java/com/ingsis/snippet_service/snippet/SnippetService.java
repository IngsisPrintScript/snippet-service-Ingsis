package com.ingsis.snippet_service.snippet;

import com.ingsis.snippet_service.validationResult.ValidationResult;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SnippetService {

  private final SnippetRepo repository;

  // private final PrintScriptParser parser;

  // PrintScriptParser parser to add
  public SnippetService(SnippetRepo repository) {
    this.repository = repository;
    // this.parser = parser;
  }

  public Snippet createSnippet(Snippet snippet) {
    // ValidationResult result = parser.validate(snippet.getContent()); To DO
    ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
    if (!result.isValid()) {
      return snippet;
    }
    snippet.setValidationResult(result);
    return repository.save(snippet);
  }

  public Snippet updateSnippet(UUID id, Snippet updatedSnippet) {
    Snippet existing =
        repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));

    existing.setName(updatedSnippet.getName());
    existing.setDescription(updatedSnippet.getDescription());
    existing.setLanguage(updatedSnippet.getLanguage());
    existing.setVersion(updatedSnippet.getVersion());
    existing.setContent(updatedSnippet.getContent());

    // ValidationResult result = parserClient.validate(existing);
    ValidationResult result = new ValidationResult(true, "let name:String = \"Pepe\";");
    existing.setValidationResult(result);

    if (!result.isValid()) {
      return existing;
    }
    return repository.save(existing);
  }
}

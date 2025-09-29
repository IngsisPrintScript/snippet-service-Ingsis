package com.ingsis.snippet_service.snippet;

import com.ingsis.snippet_service.snippet.dto.RequestFileDTO;
import com.ingsis.snippet_service.snippet.dto.RequestSnippetDTO;
import com.ingsis.snippet_service.snippetDTO.Converter;
import com.ingsis.snippet_service.storageConfig.StorageService;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/snippets")
public class SnippetController {

  private final SnippetService snippetService;
  private final StorageService storageService;

  public SnippetController(SnippetService snippetService, StorageService storageService) {
    this.snippetService = snippetService;
    this.storageService = storageService;
  }

  // User Story 1
  @PostMapping
  public ResponseEntity<?> createSnippet(@RequestBody RequestSnippetDTO snippet) {
    Snippet saved = snippetService.createSnippet(new Converter().convertToSnippet(snippet));

    if (!saved.getValidationResult().isValid()) {
      return ResponseEntity.unprocessableEntity()
          .body(
              "Invalid Snippet: "
                  + saved.getValidationResult().getMessage()
                  + "in line:"
                  + saved.getValidationResult().getLine()
                  + ", column:"
                  + saved.getValidationResult().getColumn());
    }
    return ResponseEntity.ok(saved);
  }

  // User Story 1
  @PostMapping("/upload")
  public ResponseEntity<?> createSnippetFromFile(@ModelAttribute RequestFileDTO fileDTO)
      throws IOException {

    String blobName = UUID.randomUUID() + "-" + fileDTO.getFile().getOriginalFilename();
    String contentUrl = storageService.upload("snippets", blobName, fileDTO.getFile().getBytes());
    Snippet snippet = new Converter().convertToSnippet(fileDTO, contentUrl);
    Snippet saved = snippetService.createSnippet(snippet);

    if (!saved.getValidationResult().isValid()) {
      return ResponseEntity.unprocessableEntity()
          .body(
              "Invalid Snippet: "
                  + saved.getValidationResult().getMessage()
                  + " in line:"
                  + saved.getValidationResult().getLine()
                  + ", column:"
                  + saved.getValidationResult().getColumn());
    }

    return ResponseEntity.ok(saved);
  }

  // User Story 4
  @PutMapping("/{id}")
  public ResponseEntity<?> updateSnippet(
      @PathVariable UUID id, @RequestBody RequestSnippetDTO updatedSnippet) {
    Snippet result =
        snippetService.updateSnippet(id, new Converter().convertToSnippet(updatedSnippet));
    if (!result.getValidationResult().isValid()) {
      return ResponseEntity.unprocessableEntity()
          .body(
              "Invalid Snippet: "
                  + result.getValidationResult().getMessage()
                  + " in line: "
                  + result.getValidationResult().getLine()
                  + ", column: "
                  + result.getValidationResult().getColumn());
    }
    return ResponseEntity.ok(result);
  }

  // User Story 2
  @PutMapping("/{id}/upload")
  public ResponseEntity<?> updateSnippetFromFile(
      @PathVariable UUID id, @ModelAttribute RequestFileDTO fileDTO) {
    try {
      String blobName = UUID.randomUUID() + "-" + fileDTO.getFile().getOriginalFilename();
      String contentUrl = storageService.upload("snippets", blobName, fileDTO.getFile().getBytes());
      Snippet snippet = new Converter().convertToSnippet(fileDTO, contentUrl);
      Snippet result = snippetService.updateSnippet(id, snippet);
      if (!result.getValidationResult().isValid()) {
        return ResponseEntity.unprocessableEntity()
            .body(
                "Invalid Snippet: "
                    + result.getValidationResult().getMessage()
                    + " in line: "
                    + result.getValidationResult().getLine()
                    + ", column: "
                    + result.getValidationResult().getColumn());
      }
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
    }
  }
}

package com.ingsis.snippet_service.snippet;

import com.ingsis.snippet_service.snippet.dto.RequestFileDTO;
import com.ingsis.snippet_service.snippet.dto.RequestSnippetDTO;
import com.ingsis.snippet_service.snippetDTO.Converter;
import com.ingsis.snippet_service.storageConfig.StorageService;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
  public ResponseEntity<?> createSnippet(
      @RequestBody RequestSnippetDTO snippet, @AuthenticationPrincipal Jwt jwt) {
    Snippet saved =
        snippetService.createSnippet(
            new Converter().convertToSnippet(snippet), jwt.getClaimAsString("sub"));

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
  public ResponseEntity<?> createSnippetFromFile(
      @ModelAttribute RequestFileDTO fileDTO, @AuthenticationPrincipal Jwt jwt) throws IOException {

    String blobName = UUID.randomUUID() + "-" + fileDTO.getFile().getOriginalFilename();
    String contentUrl = storageService.upload("snippets", blobName, fileDTO.getFile().getBytes());
    Snippet snippet = new Converter().convertToSnippet(fileDTO, contentUrl);
    Snippet saved = snippetService.createSnippet(snippet, jwt.getClaimAsString("sub"));

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
      @PathVariable UUID id,
      @RequestBody RequestSnippetDTO updatedSnippet,
      @AuthenticationPrincipal Jwt jwt) {
    Snippet result =
        snippetService.updateSnippet(
            id, new Converter().convertToSnippet(updatedSnippet), jwt.getClaimAsString("sub"));
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
      @PathVariable UUID id,
      @ModelAttribute RequestFileDTO fileDTO,
      @AuthenticationPrincipal Jwt jwt) {
    try {
      String blobName = UUID.randomUUID() + "-" + fileDTO.getFile().getOriginalFilename();
      String contentUrl = storageService.upload("snippets", blobName, fileDTO.getFile().getBytes());
      Snippet snippet = new Converter().convertToSnippet(fileDTO, contentUrl);
      Snippet result = snippetService.updateSnippet(id, snippet, jwt.getClaimAsString("sub"));
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

  // User story 5
  @GetMapping()
  public ResponseEntity<?> getAllSnippets(@AuthenticationPrincipal Jwt jwt) {
    try {
      return ResponseEntity.ok(snippetService.getAllSnippetsByOwner(jwt.getClaimAsString("sub")));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error getting the snippets: " + e.getMessage());
    }
  }

  // User story 6
  @GetMapping("/{id}")
  public ResponseEntity<?> getSnippet(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(
        snippetService.findByIdAndSnippetOwnerId(id, jwt.getClaimAsString("sub")));
  }

  // User story 7
  @PostMapping("/{id}/share")
  public ResponseEntity<?> shareSnippet(
      @PathVariable UUID id,
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam String sharedWithUserId) {
    try {
      return ResponseEntity.ok(
          snippetService.shareSnippet(id, sharedWithUserId, jwt.getClaimAsString("sub")));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error getting the snippets: " + e.getMessage());
    }
  }
}

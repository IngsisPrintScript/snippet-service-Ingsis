package com.ingsis.snippet_service.snippet;

import com.ingsis.snippet_service.intermediate.TestSnippetsService;
import com.ingsis.snippet_service.intermediate.UserAuthorizationService;
import com.ingsis.snippet_service.snippet.dto.RequestFileDTO;
import com.ingsis.snippet_service.snippet.dto.RequestSnippetDTO;
import com.ingsis.snippet_service.snippet.dto.TestDTO;
import com.ingsis.snippet_service.snippet.dto.TestReceiveDTO;
import com.ingsis.snippet_service.snippetDTO.Converter;
import com.ingsis.snippet_service.storageConfig.StorageService;
import java.io.IOException;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/snippets")
public class SnippetController {

  private final SnippetService snippetService;
  private final StorageService storageService;
  private final UserAuthorizationService userAuthorizationService;
  private final TestSnippetsService testSnippetsService;

  public SnippetController(SnippetService snippetService, StorageService storageService, UserAuthorizationService userAuthorizationService, TestSnippetsService testSnippetsService) {
    this.snippetService = snippetService;
    this.storageService = storageService;
    this.userAuthorizationService = userAuthorizationService;
    this.testSnippetsService = testSnippetsService;
  }

  // User Story 1
  @PostMapping
  public ResponseEntity<?> createSnippet(
      @RequestBody RequestSnippetDTO snippet, @AuthenticationPrincipal Jwt jwt) {
    String contentUrl = storageService.upload("snippets",(UUID.randomUUID() + "-" + "code"), snippet.getContent().getBytes());
    Snippet saved =
        snippetService.createSnippet(
            new Converter().convertToSnippet(snippet, contentUrl), jwt.getClaimAsString("sub"));

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

  // User Story 3
  @PostMapping("/upload")
  public ResponseEntity<?> createSnippetFromFile(
      @ModelAttribute RequestFileDTO fileDTO, @AuthenticationPrincipal Jwt jwt) throws IOException {

    String blobName = UUID.randomUUID() + "-" + fileDTO.getFile().getOriginalFilename();
    String contentUrl = storageService.upload("snippets", blobName, fileDTO.getFile().getBytes());
    Snippet snippet = new Converter().convertFileToSnippet(fileDTO, contentUrl);
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
    String contentUrl = storageService.upload("snippets",(UUID.randomUUID() + "-" + "code"), updatedSnippet.getContent().getBytes());
    Snippet result =
        snippetService.updateSnippet(
            id, new Converter().convertToSnippet(updatedSnippet, contentUrl), jwt.getClaimAsString("sub"));
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
      Snippet snippet = new Converter().convertFileToSnippet(fileDTO, contentUrl);
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
  //User story 13
  @GetMapping("/{id}/download/original")
  public ResponseEntity<?> downloadOriginal(
          @AuthenticationPrincipal Jwt jwt,
          @PathVariable UUID id) {
    Snippet snippet = snippetService.downloadOriginalSnippet(jwt.getClaimAsString("sub"), id);
    ByteArrayResource resource = new ByteArrayResource(snippet.getContent().getBytes());

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + snippet.getName() + "_original.txt\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(resource);
  }

  //User story 13
  @GetMapping("/{id}/download/formatted")
  public ResponseEntity<?> downloadFormatted(
          @AuthenticationPrincipal Jwt jwt,
          @PathVariable UUID id) {
    Snippet snippet = snippetService.downloadFormattedSnippet(jwt.getClaimAsString("sub"), id);
    ByteArrayResource resource = new ByteArrayResource(snippet.getContent().getBytes());

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + snippet.getName() + "_formatted.txt\"")
            .contentType(MediaType.TEXT_PLAIN)
            .body(resource);
  }

  @PostMapping("/users")
  public ResponseEntity<?> createUser(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID roleId){
      return ResponseEntity.ok(userAuthorizationService.createUser(jwt.getClaimAsString("sub"), roleId));
  }

  //User story 8
  @PostMapping("/{id}/test")
  public ResponseEntity<?> createTest(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID id, @RequestBody TestReceiveDTO testDTO){
    return ResponseEntity.ok(testSnippetsService.createTest(jwt.getClaimAsString("sub"),new Converter().convertTestToTestDTO(id,testDTO)));
  }
}

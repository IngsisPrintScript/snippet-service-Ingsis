package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.ToMove.intermediate.*;
import com.ingsis.snippetManager.ToMove.snippet.dto.*;
import com.ingsis.snippetManager.ToMove.snippetDTO.Converter;
import com.ingsis.snippetManager.ToMove.storageConfig.StorageService;
import com.ingsis.snippetManager.ToMove.validationResult.ValidationResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/snippets")
public class SnippetController {

  private final SnippetService snippetService;
  private final StorageService storageService;
  private final UserAuthorizationService userAuthorizationService;
  private final TestSnippetsService testSnippetsService;
  private final FormatService formatService;
  private final LintingService lintingService;
  private final FormatJobService formatJobService;

  public SnippetController(
      SnippetService snippetService,
      StorageService storageService,
      UserAuthorizationService userAuthorizationService,
      TestSnippetsService testSnippetsService,
      FormatService formatService,
      LintingService lintingService,
      FormatJobService formatJobService) {
    this.snippetService = snippetService;
    this.storageService = storageService;
    this.userAuthorizationService = userAuthorizationService;
    this.testSnippetsService = testSnippetsService;
    this.formatService = formatService;
    this.lintingService = lintingService;
    this.formatJobService = formatJobService;
  }

  private boolean validRole(Jwt jwt, Roles role) {
    return userAuthorizationService.validRole(jwt.getClaim("sub"), role);
  }

  private boolean isValidLinting(Snippet snippet) {
    if (snippet.getId() == null) {
      return true;
    }
    return lintingService.validLinting(snippet.getLintingId(), snippet.getContentUrl());
  }

  // User Story 1
  @PostMapping("/create/file")
  public ResponseEntity<?> createSnippetFromFile(
      @ModelAttribute RequestFileDTO fileDTO, @AuthenticationPrincipal Jwt jwt) throws IOException {
    if (!validRole(jwt, Roles.DEVELOPER)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    String blobName = UUID.randomUUID() + "-" + fileDTO.file().getOriginalFilename();
    String contentUrl = storageService.upload("snippets", blobName, fileDTO.file().getBytes());
    Snippet snippet = new Converter().convertFileToSnippet(fileDTO, contentUrl);
    ValidationResult saved = snippetService.createSnippet(snippet, getOwnerId(jwt));

    if (!saved.isValid()) {
      String errorMsg =
          String.format(
              "Invalid Snippet: %s in line: %d, column: %d",
              saved.getMessage(), saved.getLine(), saved.getColumn());
      return ResponseEntity.unprocessableEntity().body(errorMsg);
    }
    return ResponseEntity.ok(saved);
  }

  // User Story 3
  @PostMapping("/create/text")
  public ResponseEntity<?> createSnippet(
      @RequestBody RequestSnippetDTO snippet, @AuthenticationPrincipal Jwt jwt) {
    if (!validRole(jwt, Roles.DEVELOPER)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    String contentUrl =
        storageService.upload(
            "snippets", (UUID.randomUUID() + "-" + "code"), snippet.content().getBytes());
    ValidationResult saved =
        snippetService.createSnippet(
            new Converter().convertToSnippet(snippet, contentUrl), getOwnerId(jwt));

    if (!saved.isValid()) {
      String errorMsg =
          String.format(
              "Invalid Snippet: %s in line: %d, column: %d",
              saved.getMessage(), saved.getLine(), saved.getColumn());
      return ResponseEntity.unprocessableEntity().body(errorMsg);
    }
    return ResponseEntity.ok(saved);
  }

  private static String getOwnerId(Jwt jwt) {
    return jwt.getClaimAsString("sub");
  }

  // User Story 2
  @PutMapping("/{id}/update/file")
  public ResponseEntity<?> updateSnippetFromFile(
      @PathVariable UUID id,
      @ModelAttribute RequestFileDTO fileDTO,
      @AuthenticationPrincipal Jwt jwt) {
    try {
      if (!validRole(jwt, Roles.DEVELOPER)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
      }
      String blobName = UUID.randomUUID() + "-" + getOriginalFilename(fileDTO);
      String contentUrl = storageService.upload("snippets", blobName, fileDTO.file().getBytes());
      Snippet snippet = new Converter().convertFileToSnippet(fileDTO, contentUrl);
      ValidationResult result = snippetService.updateSnippet(id, snippet, getOwnerId(jwt));
      if (!result.isValid()) {
        String errorMsg =
            String.format(
                "Invalid Snippet: %s in line: %d, column: %d",
                result.getMessage(), result.getLine(), result.getColumn());
        return ResponseEntity.unprocessableEntity().body(errorMsg);
      }
      return ResponseEntity.ok(result);

    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
    }
  }

  @Nullable
  private static String getOriginalFilename(RequestFileDTO fileDTO) {
    return fileDTO.file().getOriginalFilename();
  }

  // User Story 4
  @PutMapping("/{id}/update/text")
  public ResponseEntity<?> updateSnippet(
      @PathVariable UUID id,
      @RequestBody RequestSnippetDTO updatedSnippet,
      @AuthenticationPrincipal Jwt jwt) {
    if (!validRole(jwt, Roles.DEVELOPER)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    String contentUrl =
        storageService.upload(
            "snippets", (UUID.randomUUID() + "-" + "code"), updatedSnippet.content().getBytes());
    ValidationResult result =
        snippetService.updateSnippet(
            id, new Converter().convertToSnippet(updatedSnippet, contentUrl), getOwnerId(jwt));
    if (!result.isValid()) {
      String errorMsg =
          String.format(
              "Invalid Snippet: %s in line: %d, column: %d",
              result.getMessage(), result.getLine(), result.getColumn());
      return ResponseEntity.unprocessableEntity().body(errorMsg);
    }
    return ResponseEntity.ok(result);
  }

  // User story 5
  @GetMapping
  public ResponseEntity<?> getAllSnippets(
      @AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) FilterDTO filterDTO) {
    try {
      if (!validRole(jwt, Roles.SNIPPETS_OWNER)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
      }
      List<Snippet> snippets =
          filterDTO == null
              ? snippetService.getAllSnippetsByOwner(getOwnerId(jwt))
              : snippetService.getSnippetsBy(getOwnerId(jwt), filterDTO);
      List<SnippetValidLinting> validLinting = new ArrayList<>();
      for (Snippet snippet : snippets) {
        validLinting.add(new SnippetValidLinting(snippet, isValidLinting(snippet)));
      }
      return ResponseEntity.ok(validLinting);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error getting the snippets: " + e.getMessage());
    }
  }

  // User story 6
  @GetMapping("/{id}")
  public ResponseEntity<?> getSnippet(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    if (!validRole(jwt, Roles.SNIPPETS_ADMIN)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    Snippet snippet = snippetService.findByIdAndSnippetOwnerId(id, getOwnerId(jwt));
    List<String> lintingFailedRules = lintingService.evaluate(snippet.getContentUrl());
    Map<UUID, String> testName = testSnippetsService.getTestBySnippetId(snippet.getId());
    return ResponseEntity.ok(
        new SnippetInfoAndLintingFailedDTO(
            snippet.getName(),
            snippet.getDescription(),
            snippet.getLanguage(),
            snippet.getContentUrl(),
            lintingFailedRules,
            testName));
  }

  // User story 7 to improve
  @PostMapping("/{id}/share")
  public ResponseEntity<?> shareSnippet(
      @PathVariable UUID id,
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam String sharedWithUserId) {
    try {
      if (!validRole(jwt, Roles.SNIPPETS_ADMIN)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
      }
      return ResponseEntity.ok(snippetService.shareSnippet(id, sharedWithUserId, getOwnerId(jwt)));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Error getting the snippets: " + e.getMessage());
    }
  }

  // User story 8
  @PostMapping("/{id}/test")
  public ResponseEntity<?> createTest(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody TestReceiveDTO testDTO) {
    if (!validRole(jwt, Roles.DEVELOPER)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    return ResponseEntity.ok(
        testSnippetsService.createTest(
            getOwnerId(jwt), new Converter().convertTestToTestDTO(id, testDTO)));
  }

  //User story 9
  @GetMapping(value = "/{snippetId}/test/{testId}/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamTestOutput(
          @PathVariable UUID snippetId,
          @PathVariable UUID testId,
          @AuthenticationPrincipal Jwt jwt) {

    if (!validRole(jwt, Roles.INVESTIGATOR)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    SseEmitter emitter = new SseEmitter();
    CompletableFuture.runAsync(() -> {
      try {
        testSnippetsService.streamTestExecution(snippetId, testId, getOwnerId(jwt), emitter);
      } catch (Exception e) {
        try {
          emitter.send(SseEmitter.event().data("Error: " + e.getMessage()));
        } catch (IOException ignored) {}
        emitter.complete();
      }
    });

    return emitter;
  }


  // User story 12
  @PostMapping("/format")
  public ResponseEntity<String> formatAllSnippets(@AuthenticationPrincipal Jwt jwt) {
    List<UUID> snippetIds =
        snippetService.getAllSnippetsByOwner(getOwnerId(jwt)).stream()
            .map(Snippet::getId)
            .collect(Collectors.toList());
    String jobId = formatJobService.createJob(snippetIds);

    CompletableFuture.runAsync(
        () ->
            snippetIds.forEach(
                id -> {
                  try {
                    formatService.formatSnippet(id);
                    formatJobService.updateSnippetStatus(jobId, id, Status.SUCCESS);
                  } catch (Exception e) {
                    formatJobService.updateSnippetStatus(jobId, id, Status.FAILED);
                  }
                }));
    return ResponseEntity.ok("Job started: " + jobId);
  }

  // User story 13
  @GetMapping("/{id}/download/original")
  public ResponseEntity<?> downloadOriginal(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    if (!validRole(jwt, Roles.INVESTIGATOR)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    Snippet snippet = snippetService.downloadOriginalSnippet(getOwnerId(jwt), id);
    ByteArrayResource resource = new ByteArrayResource(snippet.getContentUrl().getBytes());

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + snippet.getName() + "_original.txt\"")
        .contentType(MediaType.TEXT_PLAIN)
        .body(resource);
  }

  // User story 13
  @GetMapping("/{id}/download/formatted")
  public ResponseEntity<?> downloadFormatted(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    if (!validRole(jwt, Roles.INVESTIGATOR)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    Snippet snippet = snippetService.downloadFormattedSnippet(getOwnerId(jwt), id);
    ByteArrayResource resource = new ByteArrayResource(snippet.getContentUrl().getBytes());

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + snippet.getName() + "_formatted.txt\"")
        .contentType(MediaType.TEXT_PLAIN)
        .body(resource);
  }

  @PostMapping("/format/rules")
  public ResponseEntity<?> setFormatRules(
          @AuthenticationPrincipal Jwt jwt,
          @RequestBody FormatDTO rulesDTO) {

    if (!validRole(jwt, Roles.DEVELOPER)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    boolean success = formatService.updateFormatRules(getOwnerId(jwt), rulesDTO);

    if (success) {
      return ResponseEntity.ok("Format rules updated successfully.");
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error updating format rules.");
    }
  }

  @PostMapping("/format/rules")
  public ResponseEntity<?> setLintingRules(
          @AuthenticationPrincipal Jwt jwt,
          @RequestBody LintingDTO rulesDTO) {

    if (!validRole(jwt, Roles.DEVELOPER)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    boolean success = lintingService.updateLintingRules(getOwnerId(jwt), rulesDTO);
    if (success) {
      return ResponseEntity.ok("linting rules updated successfully.");
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error updating format rules.");
    }
  }
}

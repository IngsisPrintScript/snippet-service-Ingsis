package com.ingsis.snippetManager.snippet.controllers.testing;

import com.ingsis.snippetManager.intermediate.testing.TestingService;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.testing.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/test")
public class SnippetTestingController {

    private final SnippetRepo snippetRepo;
    private final TestingService testingService;

    public SnippetTestingController(SnippetRepo snippetRepo, TestingService testingService) {
        this.snippetRepo = snippetRepo;
        this.testingService = testingService;
    }

    @PostMapping("/create")
    public ResponseEntity<GetTestDTO> createTests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateTestDTO createDTO
    ) {
        String userId = getOwnerId(jwt);
        return testingService.createTest(userId, createDTO);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateTests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateTestDTO updateDTO
    ) {
        String userId = getOwnerId(jwt);
        return testingService.updateTest(userId, updateDTO);
    }

    @DeleteMapping("/{testId}")
    public ResponseEntity<String> deleteParticularTest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID testId
    ) {
        String userId = getOwnerId(jwt);
        return testingService.deleteParticularTest(userId, testId);
    }

    @GetMapping("/{snippetId}")
    public ResponseEntity<List<GetTestDTO>> getTestBySnippetId(@AuthenticationPrincipal Jwt jwt,@RequestParam  UUID snippetId) {
        String userId = getOwnerId(jwt);
        return testingService.getTestsBySnippetIdAndTestOwner(userId, snippetId);
    }
    @PostMapping("/run/{snippetId}")
    public ResponseEntity<Void> runSingleTest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snippetId,
            @RequestBody TestToRunDTO testToRunDTO
    ) {
        String userId = getOwnerId(jwt);
        testingService.runParticularTest(userId, testToRunDTO,snippetId);
        return ResponseEntity.accepted().build();
    }


    @GetMapping("/snippets/test-status")
    public ResponseEntity<List<SnippetTestsStatusDTO>> getTestsStatuses(@AuthenticationPrincipal Jwt jwt) {
        String userId = getOwnerId(jwt);
        List<Snippet> snippets = snippetRepo.findAll();

        List<SnippetTestsStatusDTO> response = snippets.stream()
                .map(snippet -> new SnippetTestsStatusDTO(
                        snippet.getId(),
                        snippet.getName(),
                        snippet.getTestStatusList().stream()
                                .map(testStatus -> new TestValidateDTO(
                                        testStatus.getTestId(),
                                        testStatus.getTestStatus()
                                ))
                                .toList()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    private static String getOwnerId(Jwt jwt) {
        // Development mode: allow missing JWT and use a fallback owner id
        return jwt != null ? jwt.getClaimAsString("sub") : "dev-user";
    }

}

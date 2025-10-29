package com.ingsis.snippetManager.snippet.controllers.testing;

import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import com.ingsis.snippetManager.intermediate.testing.TestingService;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.dto.testing.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/test")
public class SnippetTestingController {

    private final TestingService testingService;
    private final UserPermissionService userService;

    public SnippetTestingController(TestingService testingService, UserPermissionService userService) {
        this.testingService = testingService;
        this.userService = userService;
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
    public ResponseEntity<List<GetTestDTO>> getTestBySnippetId(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID snippetId) {
        String userId = getOwnerId(jwt);
        if(userService.getUserSnippets(userId, AuthorizationActions.ALL).contains(snippetId)) {
            return testingService.getTestsBySnippetIdAndTestOwner(userId, snippetId);
        }
        if(userService.getUserSnippets(userId, AuthorizationActions.READ).contains(snippetId)){
            return testingService.getTestsBySnippetId(snippetId);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
        List<UUID> snippetsOwner = userService.getUserSnippets(userId,AuthorizationActions.ALL);
        List<Snippet> snippets = testingService.getAllSnippetByOwner(snippetsOwner);
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
        return jwt.getClaimAsString("sub");
    }

}

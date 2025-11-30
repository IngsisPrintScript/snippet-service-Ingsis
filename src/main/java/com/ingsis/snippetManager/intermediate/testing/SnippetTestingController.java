package com.ingsis.snippetManager.intermediate.testing;

import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.dto.testing.GetTestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.SnippetTestsStatusDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestRunResultDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestToRunDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestValidateDTO;
import com.ingsis.snippetManager.snippet.dto.testing.UpdateDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class SnippetTestingController {

    private final TestingService testingService;

    public SnippetTestingController(TestingService testingService) {
        this.testingService = testingService;
    }

    @PostMapping("/create")
    public ResponseEntity<GetTestDTO> createTests(@AuthenticationPrincipal Jwt jwt, @RequestBody TestDTO createDTO) {
        String userId = getOwnerId(jwt);
        if (testingService.validateTest(userId, createDTO.snippetId(), AuthorizationActions.ALL)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return testingService.createTest(createDTO, jwt.getTokenValue());
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateTests(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateDTO updateDTO) {
        String userId = getOwnerId(jwt);
        if (testingService.validateTest(userId, updateDTO.snippetId(), AuthorizationActions.ALL)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return testingService.updateTest(updateDTO, jwt.getTokenValue());
    }

    @DeleteMapping("/{testId}")
    public ResponseEntity<String> deleteParticularTest(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID testId) {
        String userId = getOwnerId(jwt);
        if (testingService.validateTest(userId, testingService.findSnippetById(testId, jwt.getTokenValue()),
                AuthorizationActions.ALL)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return testingService.deleteParticularTest(testId, jwt.getTokenValue());
    }

    @GetMapping("/{snippetId}")
    public ResponseEntity<List<GetTestDTO>> getTestBySnippetId(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID snippetId) {
        String userId = getOwnerId(jwt);
        if (testingService.validateTest(userId, snippetId, AuthorizationActions.ALL)
                || testingService.validateTest(userId, snippetId, AuthorizationActions.READ)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return testingService.getTestsBySnippetId(snippetId, jwt.getTokenValue());
    }

    @PostMapping("/run")
    public ResponseEntity<TestRunResultDTO> runSingleTest(@AuthenticationPrincipal Jwt jwt,
            @RequestBody TestToRunDTO testToRunDTO) {
        String userId = getOwnerId(jwt);
        if (testingService.validateTest(userId, testToRunDTO.snippetId(), AuthorizationActions.ALL)
                || testingService.validateTest(userId, testToRunDTO.snippetId(), AuthorizationActions.READ)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return testingService.runParticularTest(testToRunDTO, jwt.getTokenValue());
    }

    @GetMapping("/snippets/test-status")
    public ResponseEntity<List<SnippetTestsStatusDTO>> getTestsStatuses(@AuthenticationPrincipal Jwt jwt) {
        String userId = getOwnerId(jwt);
        List<UUID> snippetsOwner = testingService.getUserSnippets(userId, AuthorizationActions.ALL);
        List<Snippet> snippets = testingService.getAllSnippetByOwner(snippetsOwner);
        List<SnippetTestsStatusDTO> response = snippets.stream()
                .map(snippet -> new SnippetTestsStatusDTO(snippet.getId(), snippet.getName(),
                        snippet.getTestStatusList().stream().map(
                                testStatus -> new TestValidateDTO(testStatus.getTestId(), testStatus.getTestStatus()))
                                .toList()))
                .toList();

        return ResponseEntity.ok(response);
    }

    private static String getOwnerId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }
}

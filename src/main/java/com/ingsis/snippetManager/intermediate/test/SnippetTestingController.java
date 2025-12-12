package com.ingsis.snippetManager.intermediate.test;

import com.ingsis.snippetManager.intermediate.engine.dto.response.TestResponseDTO;
import com.ingsis.snippetManager.intermediate.test.model.TestSnippets;
import com.ingsis.snippetManager.snippet.dto.testing.GetTestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestToRunDTO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class SnippetTestingController {

    private final TestingService testSnippetService;

    public SnippetTestingController(TestingService testSnippetService) {
        this.testSnippetService = testSnippetService;
    }

    @PostMapping("/create")
    public ResponseEntity<GetTestDTO> testCreateSnippets(@AuthenticationPrincipal Jwt jwt,
            @RequestBody TestDTO testDTO) {
        try {
            TestSnippets created = testSnippetService.createTestSnippets(testDTO, jwt);
            GetTestDTO response = testSnippetService.convertToGetDTO(created);
            return ResponseEntity.ok(response);
        } catch (RuntimeException a) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/update")
    public ResponseEntity<GetTestDTO> testUpdateSnippets(@AuthenticationPrincipal Jwt jwt, @RequestBody UpdateDTO dto) {
        try {
            TestSnippets updated = testSnippetService.updateTest(dto, jwt);
            GetTestDTO response = testSnippetService.convertToGetDTO(updated);
            return ResponseEntity.ok(response);
        }catch (SecurityException a){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping()
    public ResponseEntity<List<GetTestDTO>> getSnippetTests(@AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID snippetId) {
        try {
            List<GetTestDTO> getTest = testSnippetService.getTestsBySnippetId(snippetId, jwt);
            return ResponseEntity.ok(getTest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{testId}")
    public ResponseEntity<UUID> getSnippetIdByTestId(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID testId) {
        return ResponseEntity.ok(testSnippetService.getTest(testId).getSnippetId());
    }

    @DeleteMapping()
    public ResponseEntity<String> deleteParticularTest(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID testId) {
        try {
            testSnippetService.deleteTest(testId);
            return ResponseEntity.ok("Test deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting test: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteTests(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID snippetId) {
        try {
            testSnippetService.deleteTestsBySnippet(snippetId, jwt);
            return ResponseEntity.ok("Test deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting test: " + e.getMessage());
        }
    }

    @PostMapping("/run")
    public ResponseEntity<TestResponseDTO> runTestCase(@AuthenticationPrincipal Jwt jwt,
            @RequestBody TestToRunDTO testToRunDTO) {
        return ResponseEntity.ok(testSnippetService.runTestCase(testToRunDTO, jwt));
    }
}

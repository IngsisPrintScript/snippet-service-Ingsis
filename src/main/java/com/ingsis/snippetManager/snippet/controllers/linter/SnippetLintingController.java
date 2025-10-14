package com.ingsis.snippetManager.snippet.controllers.linter;


import com.ingsis.snippetManager.intermediate.LintingService;
import com.ingsis.snippetManager.intermediate.UserAuthorizationService;
import com.ingsis.snippetManager.intermediate.userRoles.Roles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/lint")
public class SnippetLintingController {

    private final LintingService lintingService;
    private final UserAuthorizationService userAuthorizationService;
    public SnippetLintingController(
            LintingService lintingService,
            UserAuthorizationService userAuthorizationService) {
        this.lintingService = lintingService;
        this.userAuthorizationService = userAuthorizationService;
    }

    private boolean validRole(Jwt jwt, Roles role) {
        return !userAuthorizationService.validRole(jwt.getClaim("sub"), role);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> linter(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        try {
            if (!validRole(jwt, Roles.SNIPPETS_ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.OK).build();
        }catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting the snippet: " + e.getMessage());
        }
    }
}

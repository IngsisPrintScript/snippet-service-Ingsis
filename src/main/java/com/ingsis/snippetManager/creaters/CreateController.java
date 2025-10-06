package com.ingsis.snippetManager.creaters;

import com.ingsis.snippetManager.ToMove.intermediate.FormatService;
import com.ingsis.snippetManager.ToMove.intermediate.LintingService;
import com.ingsis.snippetManager.ToMove.intermediate.UserAuthorizationService;
import com.ingsis.snippetManager.ToMove.snippet.dto.FormatDTO;
import com.ingsis.snippetManager.ToMove.snippet.dto.LintingDTO;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/snippets")
public class CreateController {

  private final UserAuthorizationService userAuthorizationService;
  private final LintingService lintingService;
  private final FormatService formatService;

  public CreateController(
      UserAuthorizationService userAuthorizationService,
      LintingService lintingService,
      FormatService formatService) {
    this.userAuthorizationService = userAuthorizationService;
    this.lintingService = lintingService;
    this.formatService = formatService;
  }

  @PostMapping("/users")
  public ResponseEntity<?> createUserWIthRole(
      @AuthenticationPrincipal Jwt jwt, @RequestParam UUID roleId) {
    return ResponseEntity.ok(
        userAuthorizationService.createUser(jwt.getClaimAsString("sub"), roleId));
  }

  @PostMapping("/linting")
  public ResponseEntity<?> createLintingRules(
      @AuthenticationPrincipal Jwt jwt, @RequestBody LintingDTO lintingDTO) {
    return ResponseEntity.ok(lintingService.createLinting(jwt.getClaimAsString("sub"), lintingDTO));
  }

  @PostMapping("/format")
  public ResponseEntity<?> createFormatRules(
      @AuthenticationPrincipal Jwt jwt, @RequestBody FormatDTO formatDTO) {
    return ResponseEntity.ok(formatService.createFormat(jwt.getClaimAsString("sub"), formatDTO));
  }
}

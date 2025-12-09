package com.ingsis.snippetManager.intermediate.rules;

import com.ingsis.snippetManager.intermediate.engine.dto.request.FormatRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.request.SimpleRunSnippet;
import com.ingsis.snippetManager.intermediate.engine.dto.response.ValidationResult;
import com.ingsis.snippetManager.intermediate.rules.model.Rule;
import com.ingsis.snippetManager.intermediate.rules.model.RuleType;
import com.ingsis.snippetManager.intermediate.rules.model.UserRule;
import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    public ResponseEntity<Rule> createRule(@RequestParam String name, @RequestParam String value,
            @RequestParam RuleType type, @AuthenticationPrincipal Jwt jwt) {
        Rule rule = ruleService.createRule(name, value, jwt, type);
        return ResponseEntity.ok(rule);
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<Rule> updateRule(@PathVariable UUID ruleId, @RequestParam String newValue) {
        Rule updated = ruleService.updateGlobalRule(ruleId, newValue);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<String> deleteRule(@PathVariable UUID ruleId) {
        return ResponseEntity.ok(ruleService.deleteRule(ruleId));
    }

    @PostMapping("/{ruleId}/assign")
    public ResponseEntity<UserRule> assignRule(@PathVariable UUID ruleId, @RequestParam RuleType type,
            @AuthenticationPrincipal Jwt jwt) {
        UserRule assigned = ruleService.assignRuleToUser(jwt, ruleId, type);
        return ResponseEntity.ok(assigned);
    }

    @DeleteMapping("/{ruleId}/unassign")
    public ResponseEntity<String> removeRule(@PathVariable UUID ruleId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ruleService.removeRuleFromUser(jwt, ruleId));
    }

    @GetMapping
    public ResponseEntity<List<Rule>> getRules(@RequestParam RuleType type, @AuthenticationPrincipal Jwt jwt) {
        List<Rule> rules = ruleService.getRulesForUser(jwt, type);
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/{ruleId}/update")
    public ResponseEntity<Rule> updateUserRule(@PathVariable UUID ruleId, @RequestParam String newValue,
            @AuthenticationPrincipal Jwt jwt) {
        Rule updated = ruleService.updateUserRule(jwt, ruleId, newValue);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/format")
    public ResponseEntity<SnippetStatus> formatSnippet(@RequestBody FormatRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ruleService.formatSnippet(dto, jwt));
    }

    @PostMapping("/lint")
    public ResponseEntity<ValidationResult> analyzeSnippet(@RequestBody FormatRequestDTO dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ruleService.analyzeSnippet(dto, jwt));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validateSnippet(@RequestBody SimpleRunSnippet dto,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ruleService.validateSnippet(dto, jwt));
    }
}
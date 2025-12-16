package com.ingsis.snippetManager.intermediate.rules;

import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.intermediate.engine.EngineService;
import com.ingsis.snippetManager.intermediate.engine.dto.request.FormatRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.request.LintRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.request.SimpleRunSnippet;
import com.ingsis.snippetManager.intermediate.engine.dto.response.ValidationResult;
import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.intermediate.engine.supportedRules.FormatterSupportedRules;
import com.ingsis.snippetManager.intermediate.engine.supportedRules.LintSupportedRules;
import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import com.ingsis.snippetManager.intermediate.rules.model.Rule;
import com.ingsis.snippetManager.intermediate.rules.model.RuleType;
import com.ingsis.snippetManager.intermediate.rules.model.UserRule;
import com.ingsis.snippetManager.intermediate.rules.model.dto.UpdateRuleDTO;
import com.ingsis.snippetManager.intermediate.rules.repositories.RuleRepository;
import com.ingsis.snippetManager.intermediate.rules.repositories.UserRuleRepository;
import com.ingsis.snippetManager.redis.dto.request.FormatRequestEvent;
import com.ingsis.snippetManager.redis.dto.request.LintRequestEvent;
import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import com.ingsis.snippetManager.redis.requestProducer.FormatRequestProducer;
import com.ingsis.snippetManager.redis.requestProducer.LintRequestProducer;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.Converter;
import com.ingsis.snippetManager.snippet.formatted.SnippetFormatted;
import com.ingsis.snippetManager.snippet.formatted.SnippetFormattedInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleService.class);
    private final RuleRepository ruleRepository;
    private final UserRuleRepository userRuleRepository;
    private final LintRequestProducer lintRequestProducer;
    private final FormatRequestProducer formatRequestProducer;
    private final Converter converter;
    private final EngineService engineService;
    private final SnippetRepo snippetRepo;
    private final SnippetFormattedInterface snippetFormattedInterface;
    private final UserPermissionService userPermissionService;
    private final AssetService assetService;

    public RuleService(RuleRepository ruleRepository, UserRuleRepository userRuleRepository,
            LintRequestProducer lintRequestProducer, FormatRequestProducer formatRequestProducer, Converter converter,
            EngineService engineService, SnippetRepo snippetRepo, UserPermissionService userPermissionService,
            SnippetFormattedInterface snippetFormattedInterface, AssetService assetService) {
        this.ruleRepository = ruleRepository;
        this.userRuleRepository = userRuleRepository;
        this.lintRequestProducer = lintRequestProducer;
        this.formatRequestProducer = formatRequestProducer;
        this.converter = converter;
        this.engineService = engineService;
        this.snippetRepo = snippetRepo;
        this.userPermissionService = userPermissionService;
        this.snippetFormattedInterface = snippetFormattedInterface;
        this.assetService = assetService;
    }

    private String getToken(Jwt token) {
        return token.getTokenValue();
    }

    private static String getOwnerId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }

    public void initializeDefaultRules(Jwt ownerToken) {
        String ownerId = getOwnerId(ownerToken);

        boolean hasRules = !userRuleRepository.findRuleIdsByUserIdAndType(ownerId, RuleType.LINT).isEmpty()
                || !userRuleRepository.findRuleIdsByUserIdAndType(ownerId, RuleType.FORMATTING).isEmpty();

        if (hasRules)
            return;

        List<Rule> defaultRules = List.of(
                new Rule("mandatoryVariableOrLiteralInPrintln", "false", ownerId, RuleType.LINT),
                new Rule("mandatoryVariableOrLiteralInReadInput", "false", ownerId, RuleType.LINT),
                new Rule("identifierFormat", "null", ownerId, RuleType.LINT),
                new Rule("hasPostAscriptionSpace", "false", ownerId, RuleType.FORMATTING),
                new Rule("hasPreAscriptionSpace", "false", ownerId, RuleType.FORMATTING),
                new Rule("indentationInsideConditionals", "0", ownerId, RuleType.FORMATTING),
                new Rule("isAssignationSpaced", "false", ownerId, RuleType.FORMATTING),
                new Rule("printlnSeparationLines", "0", ownerId, RuleType.FORMATTING));

        ruleRepository.saveAll(defaultRules);

        for (Rule r : defaultRules) {
            userRuleRepository.save(new UserRule(ownerId, r.getId(), r.getType()));
        }
    }

    public Rule createRule(String name, String value, Jwt ownerToken, RuleType type) {
        String ownerId = getOwnerId(ownerToken);

        Rule rule = new Rule(name, value, ownerId, type);
        ruleRepository.save(rule);

        userRuleRepository.save(new UserRule(ownerId, rule.getId(), type));

        return rule;
    }

    public Rule updateGlobalRule(UUID ruleId, String value) {
        Rule rule = ruleRepository.findById(ruleId).orElseThrow(() -> new IllegalArgumentException("Rule not found"));
        rule.setValue(value);
        return ruleRepository.save(rule);
    }

    public String deleteRule(UUID ruleId) {
        ruleRepository.deleteById(ruleId);
        return userRuleRepository.deleteAllByRuleId(ruleId);
    }

    public UserRule assignRuleToUser(Jwt userId, UUID ruleId, RuleType type) {
        UserRule userRule = new UserRule(getOwnerId(userId), ruleId, type);
        return userRuleRepository.save(userRule);
    }

    public String removeRuleFromUser(Jwt jwt, UUID ruleId) {
        return userRuleRepository.deleteByUserIdAndRuleId(getOwnerId(jwt), ruleId);
    }

    public List<Rule> getRulesForUser(Jwt jwt, RuleType type) {
        List<UUID> ruleIds = userRuleRepository.findRuleIdsByUserIdAndType(getOwnerId(jwt), type);
        return ruleRepository.findAllById(ruleIds);
    }

    public void runLintRulesForUser(Jwt jwt) {
        List<UUID> snippets = userPermissionService.getUserSnippets(getOwnerId(jwt), AuthorizationActions.ALL,
                getToken(jwt));
        for (UUID snippet : snippets) {
            Snippet snippetToLint = snippetRepo.findById(snippet)
                    .orElseThrow(() -> new IllegalArgumentException("Snippet not found"));
            snippetToLint.setLintStatus(SnippetStatus.PENDING);
            snippetRepo.save(snippetToLint);
            List<Rule> rules = ruleRepository.findAllByOwnerIdAndType(getOwnerId(jwt), RuleType.LINT);
            LintSupportedRules lintRules = converter.convertToLintRules(rules);
            LintRequestEvent event = new LintRequestEvent(getOwnerId(jwt), snippet,
                    SupportedLanguage.valueOf(snippetToLint.getLanguage().toUpperCase()), lintRules,
                    snippetToLint.getVersion());
            lintRequestProducer.publish(event);
        }
    }

    public void runFormatRulesForUser(Jwt jwt) {
        List<UUID> snippets = userPermissionService.getUserSnippets(getOwnerId(jwt), AuthorizationActions.ALL,
                getToken(jwt));
        for (UUID snippet : snippets) {
            Snippet snippetToFormat = snippetRepo.findById(snippet)
                    .orElseThrow(() -> new IllegalArgumentException("Snippet not found"));
            snippetToFormat.setFormatStatus(SnippetStatus.PENDING);
            snippetRepo.save(snippetToFormat);
            List<Rule> rules = ruleRepository.findAllByOwnerIdAndType(getOwnerId(jwt), RuleType.FORMATTING);
            FormatterSupportedRules formatRules = converter.convertToFormatterRules(rules);
            UUID formatId = snippetFormattedInterface.findById(snippet).map(SnippetFormatted::getFormattedSnippetId)
                    .orElseGet(() -> {
                        UUID newFormatId = UUID.randomUUID();
                        snippetFormattedInterface.save(new SnippetFormatted(snippet, newFormatId));
                        return newFormatId;
                    });
            FormatRequestEvent event = new FormatRequestEvent(getOwnerId(jwt), snippet, formatId,
                    SupportedLanguage.valueOf(snippetToFormat.getLanguage().toUpperCase()),
                    snippetToFormat.getVersion(), formatRules);
            formatRequestProducer.publish(event);
        }
    }

    @Transactional
    public List<Rule> updateUserRule(Jwt jwt, List<UpdateRuleDTO> newValue) {
        String ownerId = getOwnerId(jwt);

        if (newValue.isEmpty())
            return List.of();

        List<Rule> updatedRules = new ArrayList<>();

        for (UpdateRuleDTO dto : newValue) {

            userRuleRepository.findByUserIdAndRuleId(ownerId, dto.ruleId())
                    .orElseThrow(() -> new IllegalArgumentException("User does not have rule " + dto.ruleId()));

            Rule rule = ruleRepository.findById(dto.ruleId())
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found"));

            rule.setValue(dto.value());
            updatedRules.add(rule);
        }
        ruleRepository.saveAll(updatedRules);
        if (updatedRules.isEmpty()) {
            return updatedRules;
        }

        RuleType type = updatedRules.getLast().getType();
        if (type == RuleType.FORMATTING) {
            runFormatRulesForUser(jwt);
        } else if (type == RuleType.LINT) {
            runLintRulesForUser(jwt);
        }

        return updatedRules;
    }

    public SnippetStatus formatSnippet(UUID snippetId, Jwt token) {

        Snippet snippet = snippetRepo.findById(snippetId).orElseThrow(() -> new RuntimeException("Snippet not found"));

        List<Rule> rules = ruleRepository.findAllByOwnerIdAndType(getOwnerId(token), RuleType.FORMATTING);

        FormatterSupportedRules formatterRules = converter.convertToFormatterRules(rules);

        UUID formatId = snippetFormattedInterface.findByOriginalSnippetId(snippetId)
                .map(SnippetFormatted::getFormattedSnippetId).orElse(UUID.randomUUID());

        assetService.saveOriginalSnippet(snippetId, formatId);

        FormatRequestDTO dto = new FormatRequestDTO(snippetId, formatId, snippet.getVersion(),
                SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), formatterRules);

        ResponseEntity<UUID> response = engineService.format(dto, getToken(token));

        if (!response.getStatusCode().is2xxSuccessful()) {
            return SnippetStatus.FAILED;
        }

        snippetFormattedInterface.save(new SnippetFormatted(snippetId, formatId));

        return SnippetStatus.PASSED;
    }
    public ValidationResult validateSnippet(SimpleRunSnippet dto, Jwt token) {
        ResponseEntity<ValidationResult> result = engineService.validate(dto, getToken(token));
        if (!result.getStatusCode().is2xxSuccessful() || result.getBody() == null) {
            throw new IllegalArgumentException("Snippet validation failed");
        }
        return result.getBody();
    }

    public ValidationResult analyzeSnippet(UUID snippetId, Jwt token) {
        Snippet snippet = snippetRepo.findById(snippetId).orElseThrow(() -> new RuntimeException("Test not found"));
        List<Rule> rules = ruleRepository.findAllByOwnerIdAndType(getOwnerId(token), RuleType.LINT);
        LintSupportedRules lintRules = converter.convertToLintRules(rules);
        LintRequestDTO dto = new LintRequestDTO(snippetId,
                SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), snippet.getVersion(), lintRules);
        ResponseEntity<ValidationResult> result = engineService.analyze(dto, getToken(token));
        if (!result.getStatusCode().is2xxSuccessful() || result.getBody() == null) {
            throw new IllegalArgumentException("Snippet validation failed");
        }
        return result.getBody();
    }

    public ValidationResult convertToSimpleSnippetRunAndValidate(UUID snippetId, Jwt jwt) {
        Snippet snippet = snippetRepo.findById(snippetId).orElseThrow(() -> new RuntimeException("Test not found"));
        return validateSnippet(new SimpleRunSnippet(snippetId,
                SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), snippet.getVersion()), jwt);
    }
}

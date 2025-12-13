package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.authentication.AuthenticationService;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.intermediate.engine.EngineService;
import com.ingsis.snippetManager.intermediate.engine.dto.request.RunSnippetRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.request.SimpleRunSnippet;
import com.ingsis.snippetManager.intermediate.engine.dto.response.RunSnippetResponseDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.response.ValidationResult;
import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import com.ingsis.snippetManager.intermediate.rules.RuleService;
import com.ingsis.snippetManager.intermediate.test.TestingService;
import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import com.ingsis.snippetManager.snippet.dto.DataDTO;
import com.ingsis.snippetManager.snippet.dto.PaginatedSnippets;
import com.ingsis.snippetManager.snippet.dto.filters.Order;
import com.ingsis.snippetManager.snippet.dto.filters.Property;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.ShareDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetFilterDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetResponseDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetWithLintData;
import com.ingsis.snippetManager.snippet.dto.testing.GetTestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestDTO;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class SnippetService {

    private final SnippetRepo repository;
    private final RuleService ruleService;
    private final AssetService assetService;
    private final UserPermissionService userPermissionService;
    private final TestingService testingService;
    private final AuthenticationService authenticationService;
    private final EngineService engineService;
    private static final Logger logger = LoggerFactory.getLogger(SnippetService.class);

    public SnippetService(SnippetRepo repository, AssetService assetService, RuleService ruleService,
            UserPermissionService userPermissionService, TestingService testingService,
            AuthenticationService authenticationService, EngineService engineService) {
        this.repository = repository;
        this.assetService = assetService;
        this.ruleService = ruleService;
        this.userPermissionService = userPermissionService;
        this.testingService = testingService;
        this.authenticationService = authenticationService;
        this.engineService = engineService;
    }

    private String getToken(Jwt token) {
        return token.getTokenValue();
    }

    private static String getOwnerId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }

    private ResponseEntity<String> createUser(Snippet snippet, Jwt jwt, AuthorizationActions actions) {
        return userPermissionService.createUser(getOwnerId(jwt), actions, snippet.getId(), getToken(jwt));
    }

    private @NotNull List<UUID> getAllUuids(String subject, Property principal, String token) {
        if (principal == Property.BOTH || principal == null) {
            Set<UUID> set = Stream
                    .concat(userPermissionService.getUserSnippets(subject, AuthorizationActions.ALL, token).stream(),
                            userPermissionService.getUserSnippets(subject, AuthorizationActions.READ, token).stream())
                    .collect(Collectors.toSet());
            return List.copyOf(set);
        }
        // se sustituye por un switch de permisos
        AuthorizationActions authorizationActions = principal == Property.OWNER
                ? AuthorizationActions.ALL
                : AuthorizationActions.READ;
        logger.info("getAllUuids {}", authorizationActions);
        return userPermissionService.getUserSnippets(subject, authorizationActions, token);
    }

    public SnippetResponseDTO createSnippet(Snippet snippet, Jwt jwt, AuthorizationActions actions, String content) {
        try {
            ResponseEntity<String> user = createUser(snippet, jwt, actions);
            if (!user.getStatusCode().is2xxSuccessful()) {
                throw new NoSuchElementException("Forbidden");
            }
            saveSnippetContent(snippet.getId(), content);
            ValidationResult result = ruleService.validateSnippet(new SimpleRunSnippet(snippet.getId(),
                    SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), snippet.getVersion()), jwt);

            if (!result.valid()) {
                deleteSnippetUserAuthorization(jwt, snippet.getId());
                assetService.deleteSnippet(snippet.getId());
                return new SnippetResponseDTO(snippet, jwt.getSubject(), content, "FAILED");
            }
            repository.save(snippet);
            return new SnippetResponseDTO(snippet, getOwnerId(jwt), content, "PENDING");
        } catch (Exception e) {
            deleteSnippetUserAuthorization(jwt, snippet.getId());
            assetService.deleteSnippet(snippet.getId());
            return new SnippetResponseDTO(snippet, getOwnerId(jwt), content, "FAILED");
        }
    }

    public SnippetResponseDTO updateSnippet(UUID id, Jwt jwt, Snippet updatedSnippet, String content) {
        String userId = getOwnerId(jwt);
        if (!validateSnippet(userId, id, AuthorizationActions.ALL, getToken(jwt))) {
            return new SnippetResponseDTO(null, userId, null, "forbidden");
        }
        Snippet snippet = repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));
        UUID random = UUID.randomUUID();
        assetService.saveSnippet(random, content == null ? "" : content);
        ValidationResult result = ruleService.validateSnippet(new SimpleRunSnippet(random,
                SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), snippet.getVersion()), jwt);
        if (!result.valid()) {
            return new SnippetResponseDTO(snippet, userId, assetService.getSnippet(snippet.getId()).getBody(),
                    "FAILED");
        }
        snippet.setName(updatedSnippet.getName());
        snippet.setDescription(updatedSnippet.getDescription());
        snippet.setLanguage(updatedSnippet.getLanguage());
        snippet.setVersion(updatedSnippet.getVersion());
        try {
            assetService.saveSnippet(snippet.getId(), content == null ? "" : content);
            repository.save(snippet);
        } catch (Exception e) {
            assetService.deleteSnippet(random);
            return new SnippetResponseDTO(snippet, userId, assetService.getSnippet(snippet.getId()).getBody(),
                    "FAILED");
        }
        testingService.runAllTestsForSnippet(snippet.getId(), jwt);
        return new SnippetResponseDTO(snippet, userId, content, "PENDING");
    }

    public PaginatedSnippets getFilteredSnippets(SnippetFilterDTO filterDTO, int page, int pageSize, Jwt jwt) {
        List<UUID> uuids = getAllUuids(getOwnerId(jwt), filterDTO != null ? filterDTO.property() : Property.BOTH,
                getToken(jwt));
        List<Snippet> snippets = repository.findAllById(uuids);
        if (filterDTO != null) {
            if (filterDTO.name() != null && !filterDTO.name().isEmpty()) {
                String nameFilter = filterDTO.name().toLowerCase();
                snippets = snippets.stream()
                        .filter(s -> s.getName() != null && s.getName().toLowerCase().contains(nameFilter)).toList();
            }
            if (filterDTO.language() != null && !filterDTO.language().isEmpty()) {
                String languageFilter = filterDTO.language().toLowerCase();
                snippets = snippets.stream()
                        .filter(s -> s.getLanguage() != null && s.getLanguage().equalsIgnoreCase(languageFilter))
                        .toList();
            }
        }
        if (filterDTO != null && filterDTO.sortBy() != null) {
            Comparator<Snippet> comparator = switch (filterDTO.sortBy()) {
                case LANGUAGE ->
                    Comparator.comparing(Snippet::getLanguage, Comparator.nullsLast(String::compareToIgnoreCase));
                case NAME -> Comparator.comparing(Snippet::getName, Comparator.nullsLast(String::compareToIgnoreCase));
            };
            if (filterDTO.order() == Order.DESC) {
                comparator = comparator.reversed();
            }
            snippets = snippets.stream().sorted(comparator).toList();
        }
        boolean mustValidate = filterDTO != null && (filterDTO.compliance() != null);
        List<SnippetWithLintData> withLint = new ArrayList<>();
        for (Snippet snippet : snippets) {
            SnippetStatus status = snippet.getLintStatus();
            if (mustValidate || status == null) {
                ValidationResult vr = ruleService.validateSnippet(
                        new SimpleRunSnippet(snippet.getId(),
                                SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), snippet.getVersion()),
                        jwt);
                status = vr.valid() ? SnippetStatus.PASSED : SnippetStatus.FAILED;
            }

            if (filterDTO != null && filterDTO.compliance() != null
                    && !SnippetStatus.valueOf(filterDTO.compliance()).equals(status)) {
                continue;
            }

            withLint.add(new SnippetWithLintData(snippet, status, findUserBySnippetId(snippet.getId(), jwt)));
        }
        if (filterDTO != null) {
            Comparator<SnippetWithLintData> cmp = Comparator.comparing(s -> s.valid().name());

            if (filterDTO.order() == Order.DESC) {
                cmp = cmp.reversed();
            }
            withLint = withLint.stream().sorted(cmp).toList();
        }
        int total = withLint.size();
        int from = Math.max(0, page * pageSize);
        int to = Math.min(from + pageSize, total);
        List<SnippetWithLintData> slice = from < total ? withLint.subList(from, to) : List.of();
        return new PaginatedSnippets(page, pageSize, total, slice);
    }

    public String downloadSnippetContent(UUID snippetId) {
        try {
            ResponseEntity<String> response = assetService.getSnippet(repository.findById(snippetId)
                    .orElseThrow(() -> new RuntimeException("Snippet not found")).getId());
            return response.getBody() != null ? response.getBody() : "";
        } catch (Exception e) {
            logger.error("Error downloading snippet content for {}: {}", snippetId, e.getMessage(), e);
            throw new NoSuchElementException(e);
        }
    }

    public String downloadFormattedSnippetContent(UUID snippetId) {
        try {
            ResponseEntity<String> response = assetService.getFormattedSnippet(repository.findById(snippetId)
                    .orElseThrow(() -> new RuntimeException("Snippet not found")).getId());
            return response.getBody() != null ? response.getBody() : "";
        } catch (Exception e) {
            logger.error("Error downloading snippet content for {}: {}", snippetId, e.getMessage(), e);
            throw new NoSuchElementException(e);
        }
    }

    public byte[] download(Jwt jwt, String version, UUID snippetId) {
        if (!validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL, getToken(jwt))
                || !validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.READ, getToken(jwt))) {
            throw new NoSuchElementException(HttpStatus.FORBIDDEN.toString());
        }
        String content;
        if (version.equals("original")) {
            content = downloadSnippetContent(snippetId);
        } else if (version.equals("formatted")) {
            content = downloadFormattedSnippetContent(snippetId);
        } else {
            throw new RuntimeException("Unsupported version");
        }
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public ResponseEntity<String> deleteSnippet(UUID snippetId, Jwt jwt) {
        if (!validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL, getToken(jwt))) {
            throw new NoSuchElementException(HttpStatus.FORBIDDEN.toString());
        }
        Snippet snippet = repository.findById(snippetId).orElseThrow(() -> new RuntimeException("Snippet not found"));
        List<GetTestDTO> test = testingService.getTestsBySnippetId(snippetId, jwt);
        try {
            deleteSnippetUserAuthorization(jwt, snippetId);
            deleteTest(jwt, snippetId);
            repository.delete(snippet);
            return assetService.deleteSnippet(snippetId);
        } catch (Exception e) {
            createUser(snippet, jwt, AuthorizationActions.ALL);
            List<GetTestDTO> list = test == null ? List.of() : test;
            for (GetTestDTO getTestDTO : list) {
                testingService.createTestSnippets(new TestDTO(snippetId, getTestDTO.name(), getTestDTO.inputs(),
                        getTestDTO.outputs(), getTestDTO.envs()), jwt);
            }
            repository.save(snippet);
            assetService.saveSnippet(snippetId, assetService.getSnippet(snippetId).getBody());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete snippet content");
        }
    }

    public boolean validateSnippet(String subject, UUID snippetId, AuthorizationActions authorizationActions,
            String token) {
        return userPermissionService.getUserSnippets(subject, authorizationActions, token).contains(snippetId);
    }

    public String deleteSnippetUserAuthorization(Jwt jwt, UUID snippetId) {
        if (!validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL, getToken(jwt))) {
            throw new NoSuchElementException(HttpStatus.FORBIDDEN.toString());
        }
        return userPermissionService.deleteSnippetUserAuthorization(snippetId, getToken(jwt)).getBody();
    }

    public ResponseEntity<String> deleteTest(Jwt jwt, UUID snippetId) {
        return ResponseEntity.ok(testingService.deleteTestsBySnippet(snippetId));
    }

    public DataDTO getSnippetById(UUID id, Jwt jwt) {
        if (!validateSnippet(getOwnerId(jwt), id, AuthorizationActions.ALL, getToken(jwt))
                && !validateSnippet(getOwnerId(jwt), id, AuthorizationActions.READ, getToken(jwt))) {
            throw new NoSuchElementException(HttpStatus.FORBIDDEN.toString());
        }
        Snippet snippet = repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));
        String content = assetService.getSnippet(snippet.getId()).getBody();
        return new DataDTO(snippet, getOwnerId(jwt), content);
    }

    public ResponseEntity<String> saveSnippetContent(UUID snippetId, String content) {
        return assetService.saveSnippet(snippetId, content == null ? "" : content);
    }

    public List<Snippet> getAllSnippetsByOwner(Jwt jwt, Property property) {
        logger.info("Get all snippets by owner {} with property {}", getOwnerId(jwt), property);
        List<UUID> uuids = getAllUuids(getOwnerId(jwt), property, getToken(jwt));
        logger.info("All uuids: {}", uuids);
        return repository.findAllById(uuids);
    }

    public ResponseEntity<String> shareSnippet(UUID snippetId, Jwt jwt, ShareDTO shareDTO) {
        if (!validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL, getToken(jwt))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not Authorized to share snippet");
        }
        if (!authenticationService.userExists(shareDTO.userId(), jwt)) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("User not exist");
        }
        return userPermissionService.createUser(shareDTO.userId(), shareDTO.action(), snippetId, getToken(jwt));
    }

    public String findUserBySnippetId(UUID snippetId, Jwt jwt) {
        String userId = userPermissionService.getUserIdBySnippetId(snippetId, jwt.getTokenValue());
        return authenticationService.getUserNameById(userId, jwt);
    }

    public RunSnippetResponseDTO execute(UUID snippetId, Jwt jwt, List<String> inputs, Map<String, String> envs) {
        Snippet snippet = repository.findById(snippetId).orElseThrow(() -> new RuntimeException("Snippet not found"));
        return engineService.execute(new RunSnippetRequestDTO(snippetId,
                SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), inputs, snippet.getVersion(), envs),
                getToken(jwt)).getBody();
    }
}

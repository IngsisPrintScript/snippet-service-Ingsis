package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.authSecurityConfig.AuthenticationService;
import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.intermediate.lint.LintingService;
import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import com.ingsis.snippetManager.intermediate.testing.TestingService;
import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.dto.filters.Order;
import com.ingsis.snippetManager.snippet.dto.filters.Property;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.SnippetValidLintingDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetFilterDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class SnippetService {

    private final SnippetRepo repository;
    private final LintingService lintingService;
    private final AssetService assetService;
    private final UserPermissionService userPermissionService;
    private final TestingService testingService;
    private final AuthenticationService authenticationService;
    private static final Logger logger = LoggerFactory.getLogger(SnippetService.class);

    // private final PrintScriptParser parser;

    // PrintScriptParser parser to add
    public SnippetService(SnippetRepo repository, AssetService assetService, LintingService lintingService,
            UserPermissionService userPermissionService, TestingService testingService,
            AuthenticationService authenticationService) {
        this.repository = repository;
        this.assetService = assetService;
        this.lintingService = lintingService;
        this.userPermissionService = userPermissionService;
        this.testingService = testingService;
        this.authenticationService = authenticationService;
        // this.parser = parser;
    }

    public ValidationResult createSnippet(Snippet snippet) {

        // ValidationResult result = parser.validate(snippet.getContent()); To DO
        ValidationResult result = new ValidationResult(true, "Success");
        if (result.isValid()) {
            repository.save(snippet);
        }
        return result;
    }

    public ValidationResult updateSnippet(UUID id, String subject, Snippet updatedSnippet, String content) {
        logger.info("Updating snippet with id: " + id);
        Snippet snippet = repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));
        logger.info("find repository");
        // ValidationResult result = parserClient.validate(updatedSnippet.content());
        ValidationResult result = new ValidationResult(true, "Success");
        if (result.isValid()) {
            Snippet updateSnippet = new Snippet(UUID.randomUUID(), updatedSnippet.getName(),
                    updatedSnippet.getDescription(), updatedSnippet.getLanguage(), updatedSnippet.getVersion());
            logger.info("Snippet created {}", snippet.getId());
            try {
                assetService.saveSnippet(snippet.getId(), content == null ? "" : content);
                logger.info("Snippet updated {}", snippet.getId());
                repository.save(snippet);
                logger.info("Snippet updated");
            } catch (Exception e) {
                assetService.deleteSnippet(snippet.getId());
                logger.info("Snippet deleted");
                return new ValidationResult(true, e.getMessage());
            }
            runAllTestsForSnippet(subject, snippet.getId());
        }
        return result;
    }

    public List<Snippet> getSnippetsBy(String subject, SnippetFilterDTO filter, String token) {
        Sort sort = Sort.unsorted();
        logger.info("snippets unsorted");

        if (filter.sortBy() != null && filter.order() != null) {
            Sort.Direction direction = (filter.order() == Order.DESC) ? Sort.Direction.DESC : Sort.Direction.ASC;
            logger.info("Sort by direction {}", direction);

            String sortField = switch (filter.sortBy()) {
                case LANGUAGE -> "language";
                case NAME -> "name";
                case VALID -> "";
            };
            if (!sortField.isEmpty()) {
                logger.info("Sort by {}", sortField);
                sort = Sort.by(direction, sortField);
            }
        }

        boolean noFilters = (filter.name() == null || filter.name().isEmpty())
                && (filter.language() == null || filter.language().isEmpty()) && filter.valid() == null
                && filter.property() == null;
        logger.info("noFilters {}", noFilters);
        if (noFilters) {
            return getAllSnippetsByOwner(subject, Property.OWNER, token);
        }

        String nameFilter = (filter.name() != null && !filter.name().isEmpty()) ? filter.name().toLowerCase() : null;
        logger.info("nameFilter : {}", nameFilter);
        String languageFilter = (filter.language() != null && !filter.language().isEmpty())
                ? filter.language().toLowerCase()
                : null;
        logger.info("languageFilter : {}", languageFilter);

        List<UUID> uuids = getAllUuids(subject, filter.property(), token);
        logger.info("uuids {}", uuids);

        // Traer todos los snippets por UUID desde la DB
        List<Snippet> snippets = repository.findAllById(uuids);

        // Filtrado en memoria
        if (nameFilter != null) {
            snippets = snippets.stream()
                    .filter(s -> s.getName() != null && s.getName().toLowerCase().contains(nameFilter))
                    .collect(Collectors.toList());
        }

        if (languageFilter != null) {
            snippets = snippets.stream()
                    .filter(s -> s.getLanguage() != null && s.getLanguage().toLowerCase().equals(languageFilter))
                    .collect(Collectors.toList());
        }
        if (!sort.isUnsorted()) {
            Comparator<Snippet> comparator = switch (filter.sortBy()) {
                case LANGUAGE ->
                    Comparator.comparing(Snippet::getLanguage, Comparator.nullsLast(String::compareToIgnoreCase));
                case NAME -> Comparator.comparing(Snippet::getName, Comparator.nullsLast(String::compareToIgnoreCase));
                default -> null;
            };
            if (comparator != null) {
                if (filter.order() == Order.DESC) {
                    comparator = comparator.reversed();
                }
                snippets = snippets.stream().sorted(comparator).toList();
            }
        }

        return snippets;
    }

    public List<SnippetValidLintingDTO> filterValidSnippets(List<Snippet> snippets, SnippetFilterDTO filterDTO,
            String snippetOwnerId) {
        List<SnippetValidLintingDTO> validatedSnippets = new ArrayList<>();
        for (Snippet snippet : snippets) {
            logger.info("Validating snippet {}", snippet.getName());
            SnippetLintStatus linting = lintingService.validLinting(snippet.getId(), snippetOwnerId);
            logger.info("linting status {}", linting);
            if (filterDTO.valid() != null) {
                if (filterDTO.valid() == linting) {
                    validatedSnippets.add(new SnippetValidLintingDTO(snippet, linting));
                }
            } else {
                validatedSnippets.add(new SnippetValidLintingDTO(snippet, linting));
            }
        }
        return validatedSnippets;
    }

    public String downloadSnippetContent(UUID snippetId) {
        try {
            Snippet snippet = repository.findById(snippetId).orElseThrow(() -> new RuntimeException("Snippet not found"));
            ResponseEntity<String> response = assetService.getSnippet(snippetId);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("AssetService returned non-2xx status {} for snippet {}", response.getStatusCode(), snippetId);
                return "";
            }
            
            String content = response.getBody();
            return content != null ? content : "";
        } catch (Exception e) {
            logger.error("Error downloading snippet content for {}: {}", snippetId, e.getMessage(), e);
            return "";
        }
    }

    public ResponseEntity<String> deleteSnippet(UUID snippetId) {
        Snippet snippet = repository.findById(snippetId).orElseThrow(() -> new RuntimeException("Snippet not found"));
        repository.delete(snippet);
        try {
            return assetService.deleteSnippet(snippetId);
        } catch (Exception e) {
            logger.warn("Failed to delete snippet content from storage for snippet {}: {}", snippetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete snippet content");
        }
    }

    private @NotNull List<UUID> getAllUuids(String subject, Property principal, String token) {
        if (principal == Property.BOTH || principal == null) {
            Set<UUID> set = Stream
                    .concat(userPermissionService.getUserSnippets(subject, AuthorizationActions.ALL, token).stream(),
                            userPermissionService.getUserSnippets(subject, AuthorizationActions.READ, token).stream())
                    .collect(Collectors.toSet());
            return List.copyOf(set);
        }
        AuthorizationActions authorizationActions = principal == Property.OWNER
                ? AuthorizationActions.ALL
                : AuthorizationActions.READ;
        logger.info("getAllUuids {}", authorizationActions);
        return userPermissionService.getUserSnippets(subject, authorizationActions, token);
    }

    public boolean validateSnippet(String subject, UUID snippetId, AuthorizationActions authorizationActions,
            String token) {
        return !userPermissionService.getUserSnippets(subject, authorizationActions, token).contains(snippetId);
    }

    public ResponseEntity<String> createUser(String userId, AuthorizationActions authorizationActions, UUID snippetId,
            String token) {
        logger.info("Add permission {} to user {} for the snippet {}", authorizationActions, userId, snippetId);
        return userPermissionService.createUser(userId, authorizationActions, snippetId, token);
    }

    public ResponseEntity<String> deleteSnippetUserAuthorization(UUID id, String token) {
        return userPermissionService.deleteSnippetUserAuthorization(id, token);
    }

    public ResponseEntity<String> deleteTest(UUID id, String jwt) {
        return testingService.deleteTestsBySnippet(id, jwt);
    }

    public Snippet getSnippetById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));
    }

    public ResponseEntity<String> saveSnippetContent(UUID snippetId, String content) {
        logger.info("content {}", content);
        return assetService.saveSnippet(snippetId, content == null ? "" : content);
    }

    public List<Snippet> getAllSnippetsByOwner(String subject, Property property, String token) {
        logger.info("Get all snippets by owner {} with property {}", subject, property);
        List<UUID> uuids = getAllUuids(subject, property, token);
        logger.info("All uuids: {}", uuids);
        return repository.findAllById(uuids);
    }

    public List<Snippet> getSnippetByName(String name) {
        return repository.findByName(name);
    }

    public void runAllTestsForSnippet(String subject, UUID snippetId) {
        testingService.runAllTestsForSnippet(snippetId);
    }

    public String findUserBySnippetId(UUID snippetId, Jwt jwt) {
        String userId = userPermissionService.getUserIdBySnippetId(snippetId, jwt.getTokenValue());
        return authenticationService.getUserNameById(userId, jwt);
    }
}

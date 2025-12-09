package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.authSecurityConfig.AuthenticationService;
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
import com.ingsis.snippetManager.intermediate.testing.TestingService;
import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import com.ingsis.snippetManager.snippet.dto.DataDTO;
import com.ingsis.snippetManager.snippet.dto.filters.Order;
import com.ingsis.snippetManager.snippet.dto.filters.Property;
import com.ingsis.snippetManager.snippet.dto.lintingDTO.SnippetValidLintingDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.ShareDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetFilterDTO;
import com.ingsis.snippetManager.snippet.dto.testing.GetTestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestDTO;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
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

    public String createSnippet(Snippet snippet, Jwt jwt, AuthorizationActions actions, String content) {
        ResponseEntity<String> user = createUser(snippet, jwt, actions);
        if (!user.getStatusCode().is2xxSuccessful()) {
            throw new NoSuchElementException(HttpStatus.FORBIDDEN.toString());
        }
        ValidationResult result = ruleService.validateSnippet(new SimpleRunSnippet(snippet.getId(),
                SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), snippet.getVersion()), jwt);
        if (!result.valid()) {
            return result.message();
        }
        repository.save(snippet);
        return saveSnippetContent(snippet.getId(), content).getBody();
    }

    public String updateSnippet(UUID id, Jwt jwt, Snippet updatedSnippet, String content) {
        if (!validateSnippet(getOwnerId(jwt), id, AuthorizationActions.ALL, getToken(jwt))) {
            throw new NoSuchElementException(HttpStatus.FORBIDDEN.toString());
        }
        logger.info("Updating snippet with id: " + id);
        Snippet snippet = repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));
        ValidationResult result = ruleService.validateSnippet(new SimpleRunSnippet(snippet.getId(),
                SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), snippet.getVersion()), jwt);
        if (result.valid()) {
            snippet.setName(updatedSnippet.getName());
            snippet.setDescription(updatedSnippet.getDescription());
            snippet.setLanguage(updatedSnippet.getLanguage());
            snippet.setVersion(updatedSnippet.getVersion());
            logger.info("Snippet created {}", snippet.getId());
            try {
                assetService.saveSnippet(snippet.getId(), content == null ? "" : content);
                logger.info("Snippet updated {}", snippet.getId());
                repository.save(snippet);
                logger.info("Snippet updated");
            } catch (Exception e) {
                assetService.saveSnippet(snippet.getId(), assetService.getSnippet(snippet.getId()).getBody());
                logger.info("Snippet deleted");
                return e.getMessage();
            }
            testingService.runAllTestsForSnippet(snippet.getId());
        }
        return result.message();
    }

    public List<Snippet> getSnippetsWithFilter(SnippetFilterDTO filter, Jwt jwt) {
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
            return getAllSnippetsByOwner(jwt, Property.OWNER);
        }

        String nameFilter = (filter.name() != null && !filter.name().isEmpty()) ? filter.name().toLowerCase() : null;
        logger.info("nameFilter : {}", nameFilter);
        String languageFilter = (filter.language() != null && !filter.language().isEmpty())
                ? filter.language().toLowerCase()
                : null;
        logger.info("languageFilter : {}", languageFilter);

        List<UUID> uuids = getAllUuids(getOwnerId(jwt), filter.property(), getToken(jwt));
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
            Jwt jwt) {
        List<SnippetValidLintingDTO> validatedSnippets = new ArrayList<>();
        for (Snippet snippet : snippets) {
            logger.info("Validating snippet {}", snippet.getName());
            ValidationResult linting = ruleService.validateSnippet(new SimpleRunSnippet(snippet.getId(),
                    SupportedLanguage.valueOf(snippet.getLanguage().toUpperCase()), snippet.getVersion()), jwt);
            logger.info("linting status {}", linting);
            SnippetStatus status = linting.valid() ? SnippetStatus.PASSED : SnippetStatus.FAILED;
            if (filterDTO.valid() != null) {
                if (filterDTO.valid() == status) {
                    validatedSnippets.add(new SnippetValidLintingDTO(snippet, status));
                }
            } else {
                validatedSnippets.add(new SnippetValidLintingDTO(snippet, status));
            }
        }
        return validatedSnippets;
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
        List<GetTestDTO> test = testingService.getTestsBySnippetId(snippetId, getToken(jwt)).getBody();
        try {
            deleteSnippetUserAuthorization(jwt, snippetId);
            deleteTest(jwt, snippetId);
            repository.delete(snippet);
            return assetService.deleteSnippet(snippetId);
        } catch (Exception e) {
            createUser(snippet, jwt, AuthorizationActions.ALL);
            List<GetTestDTO> list = test == null ? List.of() : test;
            for (GetTestDTO getTestDTO : list) {
                testingService.createTest(
                        new TestDTO(snippetId, getTestDTO.name(), getTestDTO.inputs(), getTestDTO.outputs()),
                        getToken(jwt));
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
        return testingService.deleteTestsBySnippet(snippetId, getToken(jwt));
    }

    public DataDTO getSnippetById(UUID id, Jwt jwt) {
        if (!validateSnippet(getOwnerId(jwt), id, AuthorizationActions.ALL, getToken(jwt))) {
            throw new NoSuchElementException(HttpStatus.FORBIDDEN.toString());
        } else if (!validateSnippet(getOwnerId(jwt), id, AuthorizationActions.READ, getToken(jwt))) {
            throw new NoSuchElementException(HttpStatus.FORBIDDEN.toString());
        }
        Snippet snippet = repository.findById(id).orElseThrow(() -> new RuntimeException("Snippet not found"));
        return new DataDTO(snippet, getOwnerId(jwt), assetService.getSnippet(snippet.getId()).getBody());
    }

    public ResponseEntity<String> saveSnippetContent(UUID snippetId, String content) {
        logger.info("content {}", content);
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
        return userPermissionService.createUser(shareDTO.userId(), shareDTO.action(), snippetId, getToken(jwt));
    }

    // TO DO
    public void runAllTestsForSnippet(String subject, UUID snippetId) {
        ;
    }

    public String findUserBySnippetId(UUID snippetId, Jwt jwt) {
        String userId = userPermissionService.getUserIdBySnippetId(snippetId, jwt.getTokenValue());
        return authenticationService.getUserNameById(userId, jwt);
    }

    public RunSnippetResponseDTO execute(UUID snippetId, Jwt jwt, List<String> inputs) {
        Snippet snippet = repository.findById(snippetId).orElseThrow(() -> new RuntimeException("Snippet not found"));
        return engineService.execute(new RunSnippetRequestDTO(snippetId,
                SupportedLanguage.valueOf(snippet.getLanguage()), inputs, snippet.getVersion()), getToken(jwt))
                .getBody();
    }
}

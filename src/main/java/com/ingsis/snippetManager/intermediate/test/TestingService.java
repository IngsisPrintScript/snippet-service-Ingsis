package com.ingsis.snippetManager.intermediate.test;

import com.ingsis.snippetManager.intermediate.azureStorageConfig.AssetService;
import com.ingsis.snippetManager.intermediate.engine.EngineService;
import com.ingsis.snippetManager.intermediate.engine.dto.request.TestRequestDTO;
import com.ingsis.snippetManager.intermediate.engine.dto.response.TestResponseDTO;
import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;
import com.ingsis.snippetManager.intermediate.permissions.UserPermissionService;
import com.ingsis.snippetManager.intermediate.test.model.TestCaseExpectedOutput;
import com.ingsis.snippetManager.intermediate.test.model.TestCasesInput;
import com.ingsis.snippetManager.intermediate.test.model.TestSnippets;
import com.ingsis.snippetManager.intermediate.test.repos.TestRepo;
import com.ingsis.snippetManager.redis.dto.request.TestRequestEvent;
import com.ingsis.snippetManager.redis.requestProducer.TestRequestProducer;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.SnippetRepo;
import com.ingsis.snippetManager.snippet.dto.testing.GetTestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestDTO;
import com.ingsis.snippetManager.snippet.dto.testing.TestToRunDTO;
import com.ingsis.snippetManager.snippet.dto.testing.UpdateDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class TestingService {

    private final AssetService assetService;
    private final TestRepo testRepo;
    private final SnippetRepo snippetRepo;
    private final UserPermissionService userPermissionService;
    private final TestRequestProducer testRequestProducer;
    private final EngineService engineService;

    private static final Logger logger = LoggerFactory.getLogger(TestingService.class);

    public TestingService(SnippetRepo snippetRepo, AssetService assetService, TestRepo testRepo,
            UserPermissionService userPermissionService, TestRequestProducer testRequestProducer,
            EngineService engineService) {
        this.snippetRepo = snippetRepo;
        this.assetService = assetService;
        this.testRepo = testRepo;
        this.userPermissionService = userPermissionService;
        this.testRequestProducer = testRequestProducer;
        this.engineService = engineService;
    }

    private String getToken(Jwt token) {
        return token.getTokenValue();
    }

    private static String getOwnerId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }

    private boolean validateSnippet(String subject, UUID snippetId, AuthorizationActions authorizationActions,
            String token) {
        return userPermissionService.getUserSnippets(subject, authorizationActions, token).contains(snippetId);
    }

    @Transactional
    public TestSnippets createTestSnippets(TestDTO testDTO) {
        TestSnippets testSnippets = new TestSnippets(UUID.randomUUID(), testDTO.name(), testDTO.snippetId());
        logger.info("Creating test snippets with id {}", testSnippets.getId());

        for (String input : testDTO.input()) {
            testSnippets.getInputs().add(new TestCasesInput(UUID.randomUUID(), input, testSnippets));
        }
        for (String output : testDTO.output()) {
            testSnippets.getExpectedOutputs().add(new TestCaseExpectedOutput(UUID.randomUUID(), output, testSnippets));
        }

        return testRepo.saveAndFlush(testSnippets);
    }

    @Transactional
    public TestSnippets updateTest(UpdateDTO dto) {
        TestSnippets existing = testRepo.findById(dto.testId())
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        existing.getInputs().clear();
        existing.getExpectedOutputs().clear();

        for (String input : dto.inputs()) {
            existing.getInputs().add(new TestCasesInput(UUID.randomUUID(), input, existing));
        }
        for (String output : dto.outputs()) {
            existing.getExpectedOutputs().add(new TestCaseExpectedOutput(UUID.randomUUID(), output, existing));
        }

        return testRepo.save(existing);
    }

    @Transactional
    public void deleteTest(UUID testId) {
        TestSnippets testToDelete = testRepo.findById(testId)
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));

        testRepo.delete(testToDelete);
    }

    @Transactional
    public String deleteTestsBySnippet(UUID snippetId) {
        List<TestSnippets> testsToDelete = testRepo.findAllBySnippetId(snippetId);
        testRepo.deleteAll(testsToDelete);
        return "Successful";
    }

    public List<GetTestDTO> getTestsBySnippetId(UUID snippetId) {
        List<TestSnippets> tests = testRepo.findAllBySnippetId(snippetId);

        return tests.stream().map(this::convertToGetDTO).toList();
    }

    public GetTestDTO convertToGetDTO(TestSnippets updated) {
        List<String> inputs = updated.getInputs().stream().map(TestCasesInput::getInputUrl).toList();
        List<String> outputs = updated.getExpectedOutputs().stream().map(TestCaseExpectedOutput::getOutput).toList();

        return new GetTestDTO(updated.getId(), updated.getSnippetId(), updated.getName(), inputs, outputs);
    }

    @Transactional
    public TestResponseDTO runTestCase(TestToRunDTO testToRunDTO, Jwt jwt) {
        if (!validateSnippet(getOwnerId(jwt), testToRunDTO.snippetId(), AuthorizationActions.ALL, getOwnerId(jwt))) {
            throw new RuntimeException("Snippet validation failed");
        }
        TestSnippets testCase = testRepo.findById(testToRunDTO.testCaseId())
                .orElseThrow(() -> new RuntimeException("TestCase not found"));
        Snippet snippet = snippetRepo.findById(testToRunDTO.snippetId())
                .orElseThrow(() -> new RuntimeException("Snippet not found"));
        String snippetContent = assetService.getSnippet(testCase.getSnippetId()).getBody();
        if (snippetContent == null || snippetContent.isBlank()) {
            throw new RuntimeException("Snippet content is empty");
        }

        List<String> inputs = testCase.getInputs().stream().map(TestCasesInput::getInputUrl).toList();

        List<String> expectedOutputs = testCase.getExpectedOutputs().stream().map(TestCaseExpectedOutput::getOutput)
                .toList();
        TestRequestDTO request = new TestRequestDTO(testCase.getSnippetId(), inputs, expectedOutputs,
                SupportedLanguage.valueOf(snippet.getLanguage()), snippet.getVersion());
        ResponseEntity<TestResponseDTO> engineResponse = engineService.test(request, getToken(jwt));

        if (!engineResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Engine failed: " + engineResponse.getStatusCode());
        }

        return engineResponse.getBody();
    }

    public TestSnippets getTest(UUID testId) {
        return testRepo.findById(testId).orElseThrow(() -> new RuntimeException("Test not found"));
    }

    @Transactional
    public TestSnippets createTestSnippets(TestDTO testDTO, Jwt jwt) {
        if (!validateSnippet(getOwnerId(jwt), testDTO.snippetId(), AuthorizationActions.ALL, getOwnerId(jwt))) {
            throw new RuntimeException("Snippet validation failed");
        }
        return createTestSnippets(testDTO);
    }

    @Transactional
    public TestSnippets updateTest(UpdateDTO dto, Jwt jwt) {
        TestSnippets test = getTest(dto.testId());
        if (!validateSnippet(getOwnerId(jwt), dto.snippetId(), AuthorizationActions.ALL, getOwnerId(jwt))) {
            throw new RuntimeException("Snippet validation failed");
        }
        return updateTest(dto);
    }

    public List<GetTestDTO> getTestsBySnippetId(UUID snippetId, Jwt jwt) {
        if (!validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL, getOwnerId(jwt))) {
            throw new RuntimeException("Snippet validation failed");
        }
        return getTestsBySnippetId(snippetId);
    }

    @Transactional
    public void deleteTestsBySnippet(UUID snippetId, Jwt jwt) {
        if (!validateSnippet(getOwnerId(jwt), snippetId, AuthorizationActions.ALL, getOwnerId(jwt))) {
            throw new RuntimeException("Snippet validation failed");
        }
        deleteTestsBySnippet(snippetId);
    }

    @Transactional
    public void runAllTestsForSnippet(UUID snippetId, Jwt jwt) {
        List<TestSnippets> testCases = testRepo.findAllBySnippetId(snippetId);
        Snippet snippet = snippetRepo.findById(snippetId).orElseThrow(() -> new RuntimeException("Snippet not found"));
        if (testCases.isEmpty()) {
            throw new RuntimeException("No tests found for snippet " + snippetId);
        }
        for (TestSnippets test : testCases) {

            List<String> inputs = test.getInputs().stream().map(TestCasesInput::getInputUrl).toList();

            List<String> expected = test.getExpectedOutputs().stream().map(TestCaseExpectedOutput::getOutput).toList();

            TestRequestEvent event = new TestRequestEvent(getOwnerId(jwt), test.getId(), snippetId,
                    SupportedLanguage.valueOf(snippet.getLanguage()), snippet.getVersion(), inputs, expected);

            logger.info("Publishing test execution request for testId {} and snippetId {}", test.getId(), snippetId);

            testRequestProducer.publish(event);
        }
    }
}

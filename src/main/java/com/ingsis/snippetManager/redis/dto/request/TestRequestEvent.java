package com.ingsis.snippetManager.redis.dto.request;

import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import java.util.List;
import java.util.UUID;

public record TestRequestEvent(String ownerId, UUID testId, UUID snippetId, SupportedLanguage language, String version,
        List<String> inputs, List<String> expectedOutputs) {
}

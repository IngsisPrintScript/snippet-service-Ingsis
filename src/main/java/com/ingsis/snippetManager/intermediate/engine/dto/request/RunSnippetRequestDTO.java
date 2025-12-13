package com.ingsis.snippetManager.intermediate.engine.dto.request;

import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RunSnippetRequestDTO(UUID snippetId, SupportedLanguage language, List<String> inputs, String version,
        Map<String, String> envs) {
}

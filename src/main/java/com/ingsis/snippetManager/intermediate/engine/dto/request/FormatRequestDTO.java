package com.ingsis.snippetManager.intermediate.engine.dto.request;

import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.intermediate.engine.supportedRules.FormatterSupportedRules;
import java.util.UUID;

public record FormatRequestDTO(UUID snippetId,UUID formatId, String version, SupportedLanguage language,
        FormatterSupportedRules formatterSupportedRules) {
}
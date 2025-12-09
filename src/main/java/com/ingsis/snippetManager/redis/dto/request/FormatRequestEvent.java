package com.ingsis.snippetManager.redis.dto.request;

import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.intermediate.engine.supportedRules.FormatterSupportedRules;
import java.util.UUID;

public record FormatRequestEvent(String ownerId, UUID snippetId, SupportedLanguage language, String version,
        FormatterSupportedRules rules) {
}

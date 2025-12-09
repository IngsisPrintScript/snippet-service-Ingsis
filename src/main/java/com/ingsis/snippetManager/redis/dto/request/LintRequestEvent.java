package com.ingsis.snippetManager.redis.dto.request;

import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.intermediate.engine.supportedRules.LintSupportedRules;
import java.util.UUID;

public record LintRequestEvent(String ownerId, UUID snippetId, SupportedLanguage language,
        LintSupportedRules supportedRules, String version) {
}

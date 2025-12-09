package com.ingsis.snippetManager.intermediate.engine.dto.request;

import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import com.ingsis.snippetManager.intermediate.engine.supportedRules.LintSupportedRules;
import java.util.UUID;

public record LintRequestDTO(UUID snippetId, SupportedLanguage language, String version, LintSupportedRules rules) {
}

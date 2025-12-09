package com.ingsis.snippetManager.intermediate.engine.dto.request;

import com.ingsis.snippetManager.intermediate.engine.supportedLanguage.SupportedLanguage;
import java.util.UUID;

public record SimpleRunSnippet(UUID snippetId, SupportedLanguage language, String version) {
}

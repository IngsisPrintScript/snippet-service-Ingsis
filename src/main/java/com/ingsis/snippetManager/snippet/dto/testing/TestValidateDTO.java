package com.ingsis.snippetManager.snippet.dto.testing;

import com.ingsis.snippetManager.redis.testing.dto.SnippetTestStatus;
import java.util.UUID;

public record TestValidateDTO(UUID testId, SnippetTestStatus testStatus) {
}

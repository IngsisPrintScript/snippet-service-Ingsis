package com.ingsis.snippetManager.snippet.dto.testing;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import java.util.UUID;

public record TestValidateDTO(UUID testId, SnippetStatus testStatus) {
}

package com.ingsis.snippetManager.snippet.dto.testing;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import java.util.List;

public record TestRunResultDTO(SnippetStatus status, String message, List<String> outputs, List<String> inputs) {
}

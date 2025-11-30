package com.ingsis.snippetManager.snippet.dto.testing;

import com.ingsis.snippetManager.redis.testing.dto.SnippetTestStatus;
import java.util.List;

public record TestRunResultDTO(SnippetTestStatus status, String message, List<String> outputs, List<String> inputs) {
}

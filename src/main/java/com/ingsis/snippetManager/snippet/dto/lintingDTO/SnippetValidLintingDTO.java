package com.ingsis.snippetManager.snippet.dto.lintingDTO;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import com.ingsis.snippetManager.snippet.Snippet;

public record SnippetValidLintingDTO(Snippet snippet, SnippetStatus valid) {
}

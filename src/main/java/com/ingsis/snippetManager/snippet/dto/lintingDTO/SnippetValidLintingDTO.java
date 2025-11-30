package com.ingsis.snippetManager.snippet.dto.lintingDTO;

import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.Snippet;

public record SnippetValidLintingDTO(Snippet snippet, SnippetLintStatus valid) {
}

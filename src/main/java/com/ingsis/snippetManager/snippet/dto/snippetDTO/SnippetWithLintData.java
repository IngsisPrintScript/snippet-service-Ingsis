package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.Snippet;

public record SnippetWithLintData(Snippet snippet, SnippetLintStatus valid, String user) {
}

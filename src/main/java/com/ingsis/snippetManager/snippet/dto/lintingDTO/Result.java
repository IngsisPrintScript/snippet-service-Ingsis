package com.ingsis.snippetManager.snippet.dto.lintingDTO;

import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;

public record Result(SnippetLintStatus evaluated, String nameRule) {
}

package com.ingsis.snippetManager.snippet.dto.lintingDTO;

import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;

import java.util.UUID;

public record GetSnippetLintingStatusDTO(UUID snippetId, SnippetLintStatus status) {
}

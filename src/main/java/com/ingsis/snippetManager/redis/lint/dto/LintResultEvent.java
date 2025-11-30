package com.ingsis.snippetManager.redis.lint.dto;

import java.util.UUID;

public record LintResultEvent(String userId, UUID snippetId, SnippetLintStatus status) {
}

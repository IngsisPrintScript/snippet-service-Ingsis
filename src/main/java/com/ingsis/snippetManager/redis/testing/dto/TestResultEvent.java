package com.ingsis.snippetManager.redis.testing.dto;

import java.util.UUID;

public record TestResultEvent(String userId,UUID testId, UUID snippetId, SnippetTestStatus status) {
}

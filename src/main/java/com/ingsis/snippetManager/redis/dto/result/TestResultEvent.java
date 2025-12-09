package com.ingsis.snippetManager.redis.dto.result;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import java.util.UUID;

public record TestResultEvent(String userId, UUID testId, UUID snippetId, SnippetStatus status) {
}

package com.ingsis.snippetManager.redis.dto.result;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import java.util.UUID;

public record LintResultEvent(String userId, UUID snippetId, SnippetStatus status) {
}

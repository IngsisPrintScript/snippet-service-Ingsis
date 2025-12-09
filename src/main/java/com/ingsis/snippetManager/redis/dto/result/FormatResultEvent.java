package com.ingsis.snippetManager.redis.dto.result;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import java.util.UUID;

public record FormatResultEvent(String userId, UUID snippetId, SnippetStatus status) {
}

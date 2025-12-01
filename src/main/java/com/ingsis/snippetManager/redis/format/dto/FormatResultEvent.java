package com.ingsis.snippetManager.redis.format.dto;

import java.util.UUID;

public record FormatResultEvent(String userId, UUID snippetId, SnippetFormatStatus status) {
}

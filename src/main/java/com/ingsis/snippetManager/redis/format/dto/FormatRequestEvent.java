package com.ingsis.snippetManager.redis.format.dto;

import java.util.UUID;

public record FormatRequestEvent(String ownerId, UUID snippetId, String language, String content) {
}
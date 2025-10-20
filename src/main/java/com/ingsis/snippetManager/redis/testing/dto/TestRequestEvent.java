package com.ingsis.snippetManager.redis.testing.dto;

import java.util.UUID;

public record TestRequestEvent(String ownerId, UUID snippetId, String language, String content) {
}
package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import java.util.UUID;

public record SnippetListItemDTO(UUID id, String name, String language, String version, String author,
        SnippetStatus status, String content) {
}
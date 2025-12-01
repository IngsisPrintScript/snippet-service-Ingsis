package com.ingsis.snippetManager.snippet.dto.format;

import com.ingsis.snippetManager.redis.format.dto.SnippetFormatStatus;
import java.util.UUID;

public record GetSnippetFormatStatusDTO(UUID snippetId, SnippetFormatStatus status) {
}

package com.ingsis.snippetManager.snippet.dto.format;

import com.ingsis.snippetManager.redis.format.dto.SnippetFormatStatus;
import com.ingsis.snippetManager.snippet.Snippet;

public record SnippetValidFormatDTO(Snippet snippet, SnippetFormatStatus valid) {
}

package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import com.ingsis.snippetManager.snippet.Snippet;

public record SnippetWithLintData(Snippet snippet, SnippetStatus valid, String user) {
}

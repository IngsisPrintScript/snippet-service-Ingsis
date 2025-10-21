package com.ingsis.snippetManager.snippet.dto.testing;

import com.ingsis.snippetManager.redis.testing.dto.SnippetTestStatus;

import java.util.UUID;

public record ParticularTestToRun(UUID testId, String content) {
}

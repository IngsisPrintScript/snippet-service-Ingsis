package com.ingsis.snippetManager.redis.testing.dto;

import com.ingsis.snippetManager.snippet.dto.testing.TestToRunDTO;

import java.util.List;
import java.util.UUID;

public record TestReturnDTO(UUID testId, SnippetTestStatus status, List<String> outputs) {
}

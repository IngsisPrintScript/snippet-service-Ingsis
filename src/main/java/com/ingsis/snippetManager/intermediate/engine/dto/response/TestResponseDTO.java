package com.ingsis.snippetManager.intermediate.engine.dto.response;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import java.util.List;

public record TestResponseDTO(List<String> outputs, List<String> errors, SnippetStatus status) {
}

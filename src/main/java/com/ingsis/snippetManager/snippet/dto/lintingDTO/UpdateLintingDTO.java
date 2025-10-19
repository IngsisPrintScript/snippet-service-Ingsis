package com.ingsis.snippetManager.snippet.dto.lintingDTO;

import java.util.UUID;

public record UpdateLintingDTO(UUID lintId, String value, boolean active) {
}

package com.ingsis.snippetManager.snippet.dto.lintingDTO;

import java.util.UUID;

public record UpdateDTO(UUID Id, String value, boolean active) {
}

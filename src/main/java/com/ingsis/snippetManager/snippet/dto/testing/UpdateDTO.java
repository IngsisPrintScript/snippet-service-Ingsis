package com.ingsis.snippetManager.snippet.dto.testing;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record UpdateDTO(@NotBlank UUID testId, @NotBlank UUID snippetId, String name, List<String> inputs,
        List<String> outputs) {
}

package com.ingsis.snippetManager.snippet.dto.testing;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TestDTO(@NotBlank UUID snippetId, String name, List<String> inputs, List<String> expectedOutputs,
        Map<String, String> envs) {
}

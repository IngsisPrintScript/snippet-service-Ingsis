package com.ingsis.snippetManager.intermediate.test.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record TestDTO(@NotBlank UUID snippetId, String name, List<String> input, List<String> output) {
}

package com.ingsis.snippetManager.intermediate.test.model.dto;

import java.util.List;

public record TestRunResultDTO(TestStatus status, String message, List<String> outputs, List<String> inputs) {
}

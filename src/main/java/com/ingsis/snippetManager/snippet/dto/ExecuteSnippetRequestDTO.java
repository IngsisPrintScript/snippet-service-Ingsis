package com.ingsis.snippetManager.snippet.dto;

import java.util.List;
import java.util.Map;

public record ExecuteSnippetRequestDTO(List<String> inputs, Map<String, String> envs) {
}

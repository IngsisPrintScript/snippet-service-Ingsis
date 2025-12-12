package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.intermediate.engine.dto.response.RunSnippetResponseDTO;

import java.util.List;
import java.util.Map;

public record ExecuteSnippetRequestDTO(List<String> inputs, Map<String, String> envs) {
}

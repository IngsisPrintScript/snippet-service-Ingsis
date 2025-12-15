package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.snippet.Snippet;

import java.util.UUID;

public record SnippetResponseDTO(
        UUID id,
        String name,
        String language,
        String version,
        String user,
        String content,
        String status
) {}

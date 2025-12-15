package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.snippet.Snippet;

import java.util.UUID;

public record SnippetDetailDTO(
        UUID id,
        String name,
        String description,
        String language,
        String version,
        String owner,
        String content
) {}
package com.ingsis.snippetManager.snippet.dto.snippetDTO;

public record RequestSnippetDTO(
        String name, String description, String language, String version, String content) {
}

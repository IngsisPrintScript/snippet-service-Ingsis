package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.snippet.Snippet;

public record SnippetResponseDTO(Snippet snippet, String user, String content, String valid) {
}

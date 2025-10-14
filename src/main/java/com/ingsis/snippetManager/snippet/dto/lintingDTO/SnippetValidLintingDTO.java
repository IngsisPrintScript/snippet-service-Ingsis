package com.ingsis.snippetManager.snippet.dto.lintingDTO;

import com.ingsis.snippetManager.snippet.Snippet;

public record SnippetValidLintingDTO(Snippet snippet, boolean valid) {}

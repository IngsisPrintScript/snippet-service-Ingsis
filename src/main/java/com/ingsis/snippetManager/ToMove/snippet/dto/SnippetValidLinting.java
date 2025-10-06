package com.ingsis.snippetManager.ToMove.snippet.dto;

import com.ingsis.snippetManager.snippet.Snippet;

public record SnippetValidLinting(Snippet snippet, boolean valid) {}

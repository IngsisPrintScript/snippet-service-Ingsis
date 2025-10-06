package com.ingsis.snippetManager.ToMove.snippet.dto;

public record RequestSnippetDTO(
    String name, String description, String language, String version, String content) {}

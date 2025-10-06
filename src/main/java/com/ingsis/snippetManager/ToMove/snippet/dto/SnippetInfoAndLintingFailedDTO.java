package com.ingsis.snippetManager.ToMove.snippet.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SnippetInfoAndLintingFailedDTO(
    String name,
    String description,
    String language,
    String contentUrl,
    List<String> failedLintingRules,
    Map<UUID, String> testsName) {}

package com.ingsis.snippetManager.snippet.dto.testing;

import java.util.UUID;

public record TestToRunDTO(UUID testCaseId, String content, String language) { }
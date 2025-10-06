package com.ingsis.snippetManager.ToMove.snippet.dto;

import java.util.UUID;

public record TestDTO(String name, UUID snippetId, String input, String output) {}

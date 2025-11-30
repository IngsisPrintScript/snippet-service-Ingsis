package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.intermediate.permissions.AuthorizationActions;

import java.util.UUID;

public record ShareDTO(String userId, AuthorizationActions action) {
}

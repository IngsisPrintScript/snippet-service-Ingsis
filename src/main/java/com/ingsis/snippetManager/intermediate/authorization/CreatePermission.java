package com.ingsis.snippetManager.intermediate.authorization;

import com.ingsis.snippetManager.intermediate.UserClientService;

import java.util.UUID;

public record CreatePermission(String userId, UUID snippetId, AuthorizationActions action){
}

package com.ingsis.snippetManager.intermediate.permissions;

import java.util.UUID;

public record CreatePermission(String userId, UUID snippetId, AuthorizationActions actions){}

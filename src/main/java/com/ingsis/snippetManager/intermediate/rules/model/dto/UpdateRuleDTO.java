package com.ingsis.snippetManager.intermediate.rules.model.dto;

import java.util.UUID;

public record UpdateRuleDTO(UUID ruleId, String value) {
}
package com.ingsis.snippetManager.intermediate.rules.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.util.UUID;

@Entity
@IdClass(UserRuleId.class)
public class UserRule {

    @Id
    private String userId;

    @Id
    private UUID ruleId;

    @Enumerated(EnumType.STRING)
    private RuleType type;

    public UserRule() {
    }

    public UserRule(String userId, UUID ruleId, RuleType type) {
        this.userId = userId;
        this.ruleId = ruleId;
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public RuleType getType() {
        return type;
    }
}

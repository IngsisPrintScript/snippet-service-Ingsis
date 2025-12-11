package com.ingsis.snippetManager.intermediate.rules.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserRuleId implements Serializable {

    private String userId;
    private UUID ruleId;

    public UserRuleId() {
    }

    public UserRuleId(String userId, UUID ruleSetId) {
        this.userId = userId;
        this.ruleId = ruleSetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UserRuleId that))
            return false;
        return userId.equals(that.userId) && ruleId.equals(that.ruleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, ruleId);
    }
}

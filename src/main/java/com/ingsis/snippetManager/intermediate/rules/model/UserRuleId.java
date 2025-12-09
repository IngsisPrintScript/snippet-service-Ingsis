package com.ingsis.snippetManager.intermediate.rules.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserRuleId implements Serializable {

    private String userId;
    private UUID ruleSetId;

    public UserRuleId() {
    }

    public UserRuleId(String userId, UUID ruleSetId) {
        this.userId = userId;
        this.ruleSetId = ruleSetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UserRuleId that))
            return false;
        return userId.equals(that.userId) && ruleSetId.equals(that.ruleSetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, ruleSetId);
    }
}

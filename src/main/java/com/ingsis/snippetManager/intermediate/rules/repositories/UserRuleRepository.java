package com.ingsis.snippetManager.intermediate.rules.repositories;

import com.ingsis.snippetManager.intermediate.rules.model.RuleType;
import com.ingsis.snippetManager.intermediate.rules.model.UserRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRuleRepository extends JpaRepository<UserRule, UUID> {

    @Modifying
    @Query("DELETE FROM UserRule ur WHERE ur.ruleId = :ruleId")
    String deleteAllByRuleId(UUID ruleId);

    @Modifying
    @Query("DELETE FROM UserRule ur WHERE ur.userId = :userId AND ur.ruleId = :ruleId")
    String deleteByUserIdAndRuleId(String userId, UUID ruleId);

    @Query("SELECT ur.ruleId FROM UserRule ur WHERE ur.userId = :userId AND ur.type = :type")
    List<UUID> findRuleIdsByUserIdAndType(String userId, RuleType type);

    Optional<UserRule> findByUserIdAndRuleId(String userId, UUID ruleId);
}

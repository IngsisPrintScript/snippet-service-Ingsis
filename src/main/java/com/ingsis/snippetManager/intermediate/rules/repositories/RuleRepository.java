package com.ingsis.snippetManager.intermediate.rules.repositories;

import com.ingsis.snippetManager.intermediate.rules.model.Rule;
import com.ingsis.snippetManager.intermediate.rules.model.RuleType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findAllByOwnerIdAndType(String ownerId, RuleType type);
}

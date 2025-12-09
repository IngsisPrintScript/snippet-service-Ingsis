package com.ingsis.snippetManager.intermediate.test.repos;

import java.util.UUID;

import com.ingsis.snippetManager.intermediate.test.model.TestCaseExpectedOutput;
import com.ingsis.snippetManager.intermediate.test.model.TestSnippets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestCaseExpectedOutputRepository extends JpaRepository<TestCaseExpectedOutput, UUID> {

    public void deleteAllByTestSnippet(TestSnippets testSnippet);
}

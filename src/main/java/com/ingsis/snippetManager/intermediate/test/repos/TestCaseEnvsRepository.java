package com.ingsis.snippetManager.intermediate.test.repos;

import com.ingsis.snippetManager.intermediate.test.model.TestCasesInput;
import com.ingsis.snippetManager.intermediate.test.model.TestSnippets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TestCaseEnvsRepository extends JpaRepository<TestCasesInput, UUID> {

    public void deleteAllByTestSnippet(TestSnippets testSnippet);

}

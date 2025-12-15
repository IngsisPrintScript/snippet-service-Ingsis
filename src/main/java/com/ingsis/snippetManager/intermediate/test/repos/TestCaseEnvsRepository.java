package com.ingsis.snippetManager.intermediate.test.repos;

import com.ingsis.snippetManager.intermediate.test.model.TestCasesInput;
import com.ingsis.snippetManager.intermediate.test.model.TestSnippets;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestCaseEnvsRepository extends JpaRepository<TestCasesInput, UUID> {

    public void deleteAllByTestSnippet(TestSnippets testSnippet);

}

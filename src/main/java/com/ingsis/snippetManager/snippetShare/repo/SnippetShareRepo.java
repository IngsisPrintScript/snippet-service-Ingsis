package com.ingsis.snippetManager.snippetShare.repo;

import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippetShare.SnippetShare;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnippetShareRepo extends JpaRepository<SnippetShare, UUID> {
    List<SnippetShare> findAllBySharedWithUserId(String userId);

    List<SnippetShare> findAllBySnippet(Snippet snippet);
}

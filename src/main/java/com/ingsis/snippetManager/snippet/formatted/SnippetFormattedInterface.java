package com.ingsis.snippetManager.snippet.formatted;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnippetFormattedInterface extends JpaRepository<SnippetFormatted, UUID> {
    Optional<SnippetFormatted> findByOriginalSnippetId(UUID originalSnippetId);
}

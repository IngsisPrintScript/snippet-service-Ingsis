package com.ingsis.snippetManager.intermediate.rules.repositories;

import com.ingsis.snippetManager.intermediate.rules.model.formatted.SnippetFormatted;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnippetFormattedInterface extends JpaRepository<SnippetFormatted, UUID> {
}

package com.ingsis.snippetManager.intermediate.rules.repositories;

import com.ingsis.snippetManager.intermediate.rules.model.Rule;
import com.ingsis.snippetManager.intermediate.rules.model.formatted.SnippetFormatted;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SnippetFormattedInterface extends JpaRepository<SnippetFormatted, UUID> {
}

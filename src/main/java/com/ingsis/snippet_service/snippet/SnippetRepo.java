package com.ingsis.snippet_service.snippet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SnippetRepo extends JpaRepository<Snippet, UUID> {
}

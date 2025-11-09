package com.ingsis.snippetManager.snippet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SnippetRepo extends JpaRepository<Snippet, UUID> {

    @Query("""
    SELECT DISTINCT s
    FROM Snippet s
    WHERE (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
      AND (:language IS NULL OR LOWER(s.language) = LOWER(:language))
      AND (:ids IS NULL OR s.id IN :ids)
    """)
    List<Snippet> findFilteredSnippets(
            @Param("name") String name,
            @Param("language") String language,
            @Param("ids") List<UUID> ids,
            Sort sort
    );
}

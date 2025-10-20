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

    @Query(
            """
                      SELECT s FROM Snippet s
                      LEFT JOIN SnippetShare ss ON ss.snippet = s
                      WHERE s.snippetOwnerId = :userId OR ss.sharedWithUserId = :userId
                    """)
    List<Snippet> findAllAccessibleByUserId(@Param("userId") String userId);

    @Query("""
                SELECT DISTINCT s
                FROM Snippet s
                LEFT JOIN s.snippetShare sh
                WHERE (
                    (:relation = 'OWNER' AND s.snippetOwnerId = :userId)
                    OR (:relation = 'SHARED' AND sh.sharedWithUserId = :userId)
                    OR (:relation = 'BOTH' AND (s.snippetOwnerId = :userId OR sh.sharedWithUserId = :userId))
                    OR :relation IS NULL
                )
                AND (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
                AND (:language IS NULL OR LOWER(s.language) = LOWER(:language))
            
            """)
    List<Snippet> findFilteredSnippets(
            @Param("userId") String userId,
            @Param("relation") String relation,
            @Param("name") String name,
            @Param("language") String language,
            Sort sort
    );

    @Query("""
                SELECT DISTINCT s\s
                FROM Snippet s
                LEFT JOIN s.snippetShare sh
                WHERE s.snippetOwnerId = :userId
                   OR sh.sharedWithUserId = :userId
            """)
    List<Snippet> findAllAccessibleByUserId(@Param("userId") String userId, Sort sort);

    Optional<Snippet> findByIdAndSnippetOwnerId(
            @Param("id") UUID id, @Param("ownerId") String ownerId);

    Snippet findSnippetByIdAndSnippetOwnerId(UUID id, String snippetOwnerId);
}

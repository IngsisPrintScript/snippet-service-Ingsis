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

//    @Query(
//            """
//                      SELECT s FROM Snippet s
//                      LEFT JOIN SnippetShare ss ON ss.snippet = s
//                      WHERE s.ownersId = :userId OR ss.sharedWithUserId = :userId
//                    """)
//    List<Snippet> findAllAccessibleByUserId(@Param("userId") String userId);

    @Query("""
                SELECT DISTINCT s
                FROM Snippet s
                WHERE(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
                AND (:language IS NULL OR LOWER(s.language) = LOWER(:language))
            
            """)
    List<Snippet> findFilteredSnippets(
            @Param("name") String name,
            @Param("language") String language,
            Sort sort
    );
}

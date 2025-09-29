package com.ingsis.snippet_service.snippet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SnippetRepo extends JpaRepository<Snippet, UUID> {

  @Query("SELECT s FROM Snippet s WHERE s.snippetOwnerId = :ownerId")
  List<Snippet> getAllSnippetsByOwner(@Param("ownerId") String ownerId);

  @Query(
      """
        SELECT s FROM Snippet s
        LEFT JOIN SnippetShare ss ON ss.snippet = s
        WHERE s.snippetOwnerId = :userId OR ss.sharedWithUserId = :userId
      """)
  List<Snippet> findAllAccessibleByUserId(@Param("userId") String userId);

  Optional<Snippet> findByIdAndSnippetOwnerId(
      @Param("id") UUID id, @Param("ownerId") String ownerId);
}

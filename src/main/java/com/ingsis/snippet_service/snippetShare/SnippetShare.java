package com.ingsis.snippet_service.snippetShare;

import com.ingsis.snippet_service.snippet.Snippet;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
public class SnippetShare {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne private Snippet snippet;

  @NotBlank private String sharedWithUserId;

  private boolean canRead = true;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Snippet getSnippet() {
    return snippet;
  }

  public void setSnippet(Snippet snippet) {
    this.snippet = snippet;
  }

  public String getSharedWithUserId() {
    return sharedWithUserId;
  }

  public void setSharedWithUserId(String sharedWithUserId) {
    this.sharedWithUserId = sharedWithUserId;
  }

  public boolean isCanRead() {
    return canRead;
  }

  public void setCanRead(boolean canRead) {
    this.canRead = canRead;
  }
}

package com.ingsis.snippet_service.snippet;

import com.ingsis.snippet_service.snippetShare.SnippetShare;
import com.ingsis.snippet_service.validationResult.ValidationResult;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Snippet {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id = UUID.randomUUID();

  @NotBlank private String snippetOwnerId;

  @NotBlank private String name;

  @NotBlank private String description;

  @NotBlank private String language;

  @NotBlank private String version;

  @OneToMany(mappedBy = "snippet", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SnippetShare> snippetShare = List.of();

  @Column(columnDefinition = "TEXT")
  private String contentUrl;

  @Embedded private ValidationResult validationResult;

  public Snippet() {}

  public Snippet(String name, String description, String language, String version, String content) {
    this.name = name;
    this.description = description;
    this.language = language;
    this.version = version;
    this.contentUrl = content;
  }
  public Snippet(String name, String description, String language, String version,
                 String content, String ownerId,ValidationResult validationResult) {
    this.name = name;
    this.description = description;
    this.language = language;
    this.version = version;
    this.contentUrl = content;
    this.snippetOwnerId = ownerId;
    this.validationResult = validationResult;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getLanguage() {
    return language;
  }

  public String getVersion() {
    return version;
  }

  public String getContent() {
    return contentUrl;
  }

  public ValidationResult getValidationResult() {
    return validationResult;
  }

  public String getSnippetOwnerId() {
    return snippetOwnerId;
  }

  public List<SnippetShare> getSnippetShare() {
    return snippetShare;
  }
}

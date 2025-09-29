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
  private List<SnippetShare> snippetShare = new ArrayList<>();

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

  public void setValidationResult(ValidationResult validationResult) {
    this.validationResult = validationResult;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setContent(String content) {
    this.contentUrl = content;
  }

  public void setSnippetOwnerId(String snippetOwnerId) {
    this.snippetOwnerId = snippetOwnerId;
  }

  public void setSnippetShare(List<SnippetShare> snippetShare) {
    this.snippetShare = snippetShare;
  }
}

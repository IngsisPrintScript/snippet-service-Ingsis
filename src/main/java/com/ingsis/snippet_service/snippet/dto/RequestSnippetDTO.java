package com.ingsis.snippet_service.snippet.dto;

public class RequestSnippetDTO {

  final String name;
  final String description;
  final String language;
  final String version;
  final String contentUrl;

  public RequestSnippetDTO(
      String name, String description, String language, String version, String content) {
    this.name = name;
    this.description = description;
    this.language = language;
    this.version = version;
    this.contentUrl = content;
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
}

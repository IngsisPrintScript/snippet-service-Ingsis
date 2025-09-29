package com.ingsis.snippet_service.snippetDTO;

import com.ingsis.snippet_service.snippet.Snippet;
import com.ingsis.snippet_service.snippet.dto.RequestFileDTO;
import com.ingsis.snippet_service.snippet.dto.RequestSnippetDTO;

public class Converter {

  public Snippet convertToSnippet(RequestSnippetDTO snippetDTO) {
    return new Snippet(
        snippetDTO.getName(),
        snippetDTO.getDescription(),
        snippetDTO.getLanguage(),
        snippetDTO.getVersion(),
        snippetDTO.getContent());
  }

  public Snippet convertToSnippet(RequestFileDTO fileDTO, String contentUrl) {
    return new Snippet(
        fileDTO.getName(),
        fileDTO.getDescription(),
        fileDTO.getLanguage(),
        fileDTO.getVersion(),
        contentUrl);
  }
}

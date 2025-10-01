package com.ingsis.snippet_service.snippetDTO;

import com.ingsis.snippet_service.snippet.Snippet;
import com.ingsis.snippet_service.snippet.dto.RequestFileDTO;
import com.ingsis.snippet_service.snippet.dto.RequestSnippetDTO;
import com.ingsis.snippet_service.snippet.dto.TestDTO;
import com.ingsis.snippet_service.snippet.dto.TestReceiveDTO;

import java.util.UUID;

public class Converter {

  public Snippet convertToSnippet(RequestSnippetDTO snippetDTO, String contentUrl) {
    return new Snippet(
            snippetDTO.getName(),
            snippetDTO.getDescription(),
            snippetDTO.getLanguage(),
            snippetDTO.getVersion(),
            contentUrl);
  }

  public Snippet convertFileToSnippet(RequestFileDTO fileDTO, String contentUrl) {
    return new Snippet(
            fileDTO.getName(),
            fileDTO.getDescription(),
            fileDTO.getLanguage(),
            fileDTO.getVersion(),
            contentUrl);
  }

  public TestDTO convertTestToTestDTO(UUID id, TestReceiveDTO testDTO) {
    return new TestDTO(testDTO.getName(),id,testDTO.getInput(),testDTO.getOutput());
  }
}

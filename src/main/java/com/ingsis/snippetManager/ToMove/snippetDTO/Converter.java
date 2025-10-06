package com.ingsis.snippetManager.ToMove.snippetDTO;

import com.ingsis.snippetManager.ToMove.snippet.dto.RequestFileDTO;
import com.ingsis.snippetManager.ToMove.snippet.dto.RequestSnippetDTO;
import com.ingsis.snippetManager.ToMove.snippet.dto.TestDTO;
import com.ingsis.snippetManager.ToMove.snippet.dto.TestReceiveDTO;
import com.ingsis.snippetManager.snippet.Snippet;
import java.util.UUID;

public class Converter {

  public Snippet convertToSnippet(RequestSnippetDTO snippetDTO, String contentUrl) {
    return new Snippet(
        snippetDTO.name(),
        snippetDTO.description(),
        snippetDTO.language(),
        snippetDTO.version(),
        contentUrl);
  }

  public Snippet convertFileToSnippet(RequestFileDTO fileDTO, String contentUrl) {
    return new Snippet(
        fileDTO.name(), fileDTO.description(), fileDTO.language(), fileDTO.version(), contentUrl);
  }

  public TestDTO convertTestToTestDTO(UUID id, TestReceiveDTO testDTO) {
    return new TestDTO(testDTO.name(), id, testDTO.input(), testDTO.output());
  }
}

package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestFileDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestSnippetDTO;

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

//  public TestDTO convertTestToTestDTO(UUID id, TestReceiveDTO testDTO) {
//    return new TestDTO(testDTO.name(), id, testDTO.input(), testDTO.output());
//  }
}

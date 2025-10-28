package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestFileDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestSnippetDTO;

import java.io.IOException;
import java.util.UUID;

public class Converter {

    public Snippet convertToSnippet(RequestSnippetDTO snippetDTO, String contentUrl, String ownerId) {
        return new Snippet(
                snippetDTO.name(),
                snippetDTO.description(),
                snippetDTO.language(),
                snippetDTO.version(),
                contentUrl,
                ownerId);
    }

    public Snippet convertFileToSnippet(RequestFileDTO fileDTO) {
        return new Snippet(
                fileDTO.name(), fileDTO.description(), fileDTO.language(), fileDTO.version());
    }
}

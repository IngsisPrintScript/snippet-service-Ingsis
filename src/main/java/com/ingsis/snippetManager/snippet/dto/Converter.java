package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestFileDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestSnippetDTO;
import java.util.UUID;

public class Converter {

    public Snippet convertToSnippet(RequestSnippetDTO snippetDTO) {
        return new Snippet(UUID.randomUUID(), snippetDTO.name(), snippetDTO.description(), snippetDTO.language(),
                snippetDTO.version());
    }

    public Snippet convertFileToSnippet(RequestFileDTO fileDTO) {
        return new Snippet(UUID.randomUUID(), fileDTO.name(), fileDTO.description(), fileDTO.language(),
                fileDTO.version());
    }
}

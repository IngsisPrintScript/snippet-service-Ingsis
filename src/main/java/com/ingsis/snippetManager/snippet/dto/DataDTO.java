package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.snippet.Snippet;

public record DataDTO(Snippet snippet,String owner, String content) {
}

package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.snippet.dto.snippetDTO.SnippetWithLintData;
import java.util.List;

public record PaginatedSnippets(int page, int page_size, int count, List<SnippetWithLintData> snippets) {
}

package com.ingsis.snippetManager.snippet.dto;

import java.util.List;

public record PaginatedSnippets(int page, int page_size, int count, List<SnippetListItemDTO> snippets) {
}

package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.dto.filters.Order;
import com.ingsis.snippetManager.snippet.dto.filters.Property;
import com.ingsis.snippetManager.snippet.dto.filters.SortBy;

public record SnippetFilterDTO(String name, String language, SnippetLintStatus valid, SortBy sortBy, Order order,
        Property property) {
}

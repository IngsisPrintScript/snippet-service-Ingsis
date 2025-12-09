package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import com.ingsis.snippetManager.snippet.dto.filters.Order;
import com.ingsis.snippetManager.snippet.dto.filters.Property;
import com.ingsis.snippetManager.snippet.dto.filters.SortBy;

public record SnippetFilterDTO(String name, String language, SnippetStatus valid, SortBy sortBy, Order order,
        Property property) {
}

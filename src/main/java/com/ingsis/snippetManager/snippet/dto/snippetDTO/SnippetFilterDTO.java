package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.snippet.dto.filters.Order;
import com.ingsis.snippetManager.snippet.dto.filters.Property;
import com.ingsis.snippetManager.snippet.dto.filters.SortBy;

public record SnippetFilterDTO(String name, String language, String compliance, SortBy sortBy, Order order,
        Property property) {
}

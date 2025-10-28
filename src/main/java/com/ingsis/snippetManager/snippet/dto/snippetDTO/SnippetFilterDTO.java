package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippet.controllers.filters.Order;
import com.ingsis.snippetManager.snippet.controllers.filters.Property;
import com.ingsis.snippetManager.snippet.controllers.filters.SortBy;
import org.springframework.web.bind.annotation.RequestParam;

public record SnippetFilterDTO(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) SnippetLintStatus valid,
        @RequestParam(required = false) SortBy sortBy,
        @RequestParam(required = false) Order order,
        @RequestParam(required = false) Property property) {
}
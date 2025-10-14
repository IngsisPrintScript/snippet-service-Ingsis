package com.ingsis.snippetManager.snippet.dto.snippetDTO;

import com.ingsis.snippetManager.snippet.controllers.filters.Order;
import com.ingsis.snippetManager.snippet.controllers.filters.Relation;
import com.ingsis.snippetManager.snippet.controllers.filters.SortBy;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

public record SnippetFilterDTO(
        @RequestParam UUID lintingRuleId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String language,
        @RequestParam(required = false) Boolean valid,
        @RequestParam(required = false) SortBy sortBy,
        @RequestParam(required = false) Order order,
        @RequestParam(required = false) Relation property) {}
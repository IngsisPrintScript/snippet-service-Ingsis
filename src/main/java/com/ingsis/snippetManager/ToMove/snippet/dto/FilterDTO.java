package com.ingsis.snippetManager.ToMove.snippet.dto;

import com.ingsis.snippetManager.ToMove.userStory5Filters.Order;
import com.ingsis.snippetManager.ToMove.userStory5Filters.Relation;
import com.ingsis.snippetManager.ToMove.userStory5Filters.SortBy;
import org.springframework.web.bind.annotation.RequestParam;

public record FilterDTO(
    @RequestParam(required = false) String name,
    @RequestParam(required = false) String language,
    @RequestParam(required = false) boolean valid,
    @RequestParam(required = false) SortBy sortBy,
    @RequestParam(required = false) Order order,
    @RequestParam(required = false) Relation property) {}

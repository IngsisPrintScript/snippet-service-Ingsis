package com.ingsis.snippetManager.ToMove.snippet.dto;

import org.springframework.web.multipart.MultipartFile;

public record RequestFileDTO(
    String name, String description, String language, String version, MultipartFile file) {}

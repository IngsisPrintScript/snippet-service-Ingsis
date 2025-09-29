package com.ingsis.snippet_service.snippet.dto;

import org.springframework.web.multipart.MultipartFile;

public class RequestFileDTO {

    final String name;
    final String description;
    final String language;
    final String version;
    final MultipartFile file;

    public RequestFileDTO(String name, String description, String language, String version, MultipartFile file) {
        this.name = name;
        this.description = description;
        this.language = language;
        this.version = version;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLanguage() {
        return language;
    }

    public String getVersion() {
        return version;
    }

    public MultipartFile getFile() {
        return file;
    }
}

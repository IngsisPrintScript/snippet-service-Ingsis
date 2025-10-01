package com.ingsis.snippet_service.snippet.dto;

import java.util.UUID;

public class TestDTO {

    final String name;
    final UUID snippetId;
    final String input;
    final String output;

    public TestDTO(String name, UUID snippetId, String input, String output) {
        this.name = name;
        this.snippetId = snippetId;
        this.input = input;
        this.output = output;
    }

    public String getName() {
        return name;
    }

    public UUID getSnippetId() {
        return snippetId;
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }
}

package com.ingsis.snippetManager.intermediate.test.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class TestCasesInput {
    @Id
    private UUID id;

    private String input;

    @ManyToOne
    @JoinColumn(name = "test_snippet_id")
    private TestSnippets testSnippet;

    public TestCasesInput() {
    }

    public TestCasesInput(UUID id, String value, TestSnippets testSnippet) {
        this.id = id;
        this.input = value;
        this.testSnippet = testSnippet;
    }

    public UUID getId() {
        return id;
    }

    public String getInputUrl() {
        return input;
    }

    public void setInputUrl(String value) {
        this.input = value;
    }

    public TestSnippets getTestSnippet() {
        return testSnippet;
    }

    public void setTestSnippet(TestSnippets testSnippet) {
        this.testSnippet = testSnippet;
    }
}

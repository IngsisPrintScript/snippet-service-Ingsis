package com.ingsis.snippetManager.intermediate.test.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class TestCaseExpectedOutput {
    @Id
    private UUID id;

    private String output;

    @ManyToOne
    @JoinColumn(name = "test_snippet_id")
    private TestSnippets testSnippet;

    public TestCaseExpectedOutput() {
    }

    public TestCaseExpectedOutput(UUID id, String outputUrl, TestSnippets testSnippet) {
        this.id = id;
        this.output = outputUrl;
        this.testSnippet = testSnippet;
    }

    public UUID getId() {
        return id;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public TestSnippets getTestSnippet() {
        return testSnippet;
    }

    public void setTestSnippet(TestSnippets testSnippet) {
        this.testSnippet = testSnippet;
    }
}

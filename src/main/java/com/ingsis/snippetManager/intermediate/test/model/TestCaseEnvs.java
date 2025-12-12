package com.ingsis.snippetManager.intermediate.test.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.UUID;

@Entity
public class TestCaseEnvs {

    @Id
    private UUID id;

    private String key;

    private String value;

    @ManyToOne
    @JoinColumn(name = "test_snippet_id")
    private TestSnippets testSnippet;

    public TestCaseEnvs(UUID id, String key, String value, TestSnippets testCaseEnvs){
        this.id = id;
        this.key = key;
        this.value = value;
        this.testSnippet = testCaseEnvs;
    }

    public TestCaseEnvs() {

    }

    public UUID getId() {
        return id;
    }

    public TestSnippets getTestSnippet() {
        return testSnippet;
    }

    public void setTestSnippet(TestSnippets testSnippet) {
        this.testSnippet = testSnippet;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

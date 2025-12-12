package com.ingsis.snippetManager.intermediate.test.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class TestSnippets {

    @Id
    private UUID id;

    @NotBlank
    private String name;

    private UUID snippetId;

    @OneToMany(mappedBy = "testSnippet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TestCasesInput> inputs = new ArrayList<>();

    @OneToMany(mappedBy = "testSnippet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TestCaseExpectedOutput> expectedOutputs = new ArrayList<>();

    @OneToMany(mappedBy = "testSnippet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TestCaseEnvs> envs = new ArrayList<>();

    public TestSnippets() {
    }

    public TestSnippets(UUID id, String name, UUID snippetId) {
        this.id = id;
        this.name = name;
        this.snippetId = snippetId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getSnippetId() {
        return snippetId;
    }

    public List<TestCasesInput> getInputs() {
        return inputs;
    }

    public List<TestCaseExpectedOutput> getExpectedOutputs() {
        return expectedOutputs;
    }

    public void setInputs(List<TestCasesInput> inputs) {
        this.inputs = inputs;
    }

    public void setExpectedOutputs(List<TestCaseExpectedOutput> expectedOutputs) {
        this.expectedOutputs = expectedOutputs;
    }

    public List<TestCaseEnvs> getEnvs() {
        return envs;
    }

    public void setEnvs(List<TestCaseEnvs> envs) {
        this.envs = envs;
    }
}

package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.redis.format.dto.SnippetFormatStatus;
import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.*;

@Entity
public class Snippet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    private String language;

    @NotBlank
    private String version;

    @ElementCollection
    @CollectionTable(name = "snippet_tests", joinColumns = @JoinColumn(name = "snippet_id"))
    @Column(name = "test_id")
    private List<UUID> testId = new ArrayList<>();

    @OneToMany(mappedBy = "snippet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestStatus> testStatusList = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private SnippetLintStatus lintStatus = SnippetLintStatus.NOT_LINTED;

    @Enumerated(EnumType.STRING)
    private SnippetFormatStatus formatStatus = SnippetFormatStatus.NOT_LINTED;

    private String contentUrl;

    private String snippetOwnerId;


    public Snippet() {
    }

    public Snippet(String name, String description, String language, String version) {
        this.name = name;
        this.description = description;
        this.language = language;
        this.version = version;
    }

    public Snippet(UUID snippetId, String name, String description, String language, String version) {
        this.id = snippetId;
        this.name = name;
        this.description = description;
        this.language = language;
        this.version = version;
    }

    public UUID getId() {
        return id;
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

    public List<UUID> getTestId() {
        return testId;
    }

    public SnippetLintStatus getLintStatus() {
        return lintStatus;
    }
    public SnippetFormatStatus getFormatStatus() {
        return formatStatus;
    }
    public void setFormatStatus(SnippetFormatStatus formatStatus) {
        this.formatStatus = formatStatus;
    }

    public void setLintStatus(SnippetLintStatus lintStatus) {
        this.lintStatus = lintStatus;
    }


    public List<TestStatus> getTestStatusList() {
        return testStatusList;
    }

    public void setTestStatusList(List<TestStatus> testStatusList) {
        this.testStatusList = testStatusList;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setVersion(String version) {
        this.version = version;
    }
/*
    public void setSnippetOwnerId(String snippetOwnerId) {
        this.snippetOwnerId = snippetOwnerId;
    }*/

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getSnippetOwnerId() {
        return snippetOwnerId;
    }

    public Snippet(String name, String description, String language, String version, String contentUrl, String snippetOwnerId) {
        this.name = name;
        this.description = description;
        this.language = language;
        this.version = version;
        this.contentUrl = contentUrl;
        this.snippetOwnerId = snippetOwnerId;
    }
}

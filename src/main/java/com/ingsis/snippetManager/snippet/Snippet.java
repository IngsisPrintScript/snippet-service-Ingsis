package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.redis.dto.status.SnippetStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Snippet {

    @Id
    private UUID id;

    @NotBlank
    @Column(name = "name", columnDefinition = "TEXT")
    private String name;
    private String description;
    @NotBlank
    private String language;
    private String version;

    @ElementCollection
    @CollectionTable(name = "snippet_tests", joinColumns = @JoinColumn(name = "snippet_id"))
    @Column(name = "test_id")
    private List<UUID> testId = new ArrayList<>();

    @OneToMany(mappedBy = "snippet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestStatus> testStatusList = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private SnippetStatus lintStatus = SnippetStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private SnippetStatus formatStatus = SnippetStatus.TO_DO;

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

    public SnippetStatus getLintStatus() {
        return lintStatus;
    }

    public SnippetStatus getFormatStatus() {
        return formatStatus;
    }

    public void setFormatStatus(SnippetStatus formatStatus) {
        this.formatStatus = formatStatus;
    }

    public void setLintStatus(SnippetStatus lintStatus) {
        this.lintStatus = lintStatus;
    }
    public List<TestStatus> getTestStatusList() {
        return testStatusList;
    }

    public void setTestStatusList(List<TestStatus> testStatusList) {
        this.testStatusList = testStatusList;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setTestId(List<UUID> testId) {
        this.testId = testId;
    }

    public void addOrUpdateTestStatus(UUID testId, SnippetStatus status) {

        TestStatus ts = testStatusList.stream().filter(t -> t.getTestId().equals(testId)).findFirst().orElseGet(() -> {
            TestStatus newTs = new TestStatus();
            newTs.setTestId(testId);
            newTs.setSnippet(this);
            testStatusList.add(newTs);
            return newTs;
        });

        ts.setTestStatus(status);
    }
}

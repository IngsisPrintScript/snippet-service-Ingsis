package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.redis.format.dto.SnippetFormatStatus;
import com.ingsis.snippetManager.redis.lint.dto.SnippetLintStatus;
import com.ingsis.snippetManager.snippetShare.SnippetShare;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Snippet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String snippetOwnerId;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    private String language;

    @NotBlank
    private String version;

    @OneToMany(mappedBy = "snippet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SnippetShare> snippetShare = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String contentUrl;

    @ElementCollection
    @CollectionTable(name = "snippet_tests", joinColumns = @JoinColumn(name = "snippet_id"))
    @Column(name = "test_id")
    private List<UUID> testId = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private SnippetLintStatus lintStatus = SnippetLintStatus.NOT_LINTED;

    @Enumerated(EnumType.STRING)
    private SnippetFormatStatus formatStatus = SnippetFormatStatus.NOT_LINTED;

    public Snippet() {
    }

    public Snippet(String name, String description, String language, String version, String content) {
        this.name = name;
        this.description = description;
        this.language = language;
        this.version = version;
        this.contentUrl = content;
    }

    public Snippet(
            String name,
            String description,
            String language,
            String version,
            String content,
            String ownerId) {
        this.name = name;
        this.description = description;
        this.language = language;
        this.version = version;
        this.contentUrl = content;
        this.snippetOwnerId = ownerId;
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

    public String getContentUrl() {
        return contentUrl;
    }

    public String getSnippetOwnerId() {
        return snippetOwnerId;
    }

    public List<SnippetShare> getSnippetShare() {
        return snippetShare;
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
    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

}

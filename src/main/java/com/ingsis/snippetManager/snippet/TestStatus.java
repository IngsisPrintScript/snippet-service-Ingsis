package com.ingsis.snippetManager.snippet;

import com.ingsis.snippetManager.redis.testing.dto.SnippetTestStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class TestStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SnippetTestStatus testStatus;
    private UUID testId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snippet_id", nullable = false)
    private Snippet snippet;

    public UUID getId() {
        return id;
    }

    public SnippetTestStatus getTestStatus() {
        return testStatus;
    }

    public UUID getTestId() {
        return testId;
    }

    public void setTestId(UUID testId) {
        this.testId = testId;
    }

    public Snippet getSnippet() {
        return snippet;
    }

    public void setTestStatus(SnippetTestStatus testStatus) {
        this.testStatus = testStatus;
    }
}

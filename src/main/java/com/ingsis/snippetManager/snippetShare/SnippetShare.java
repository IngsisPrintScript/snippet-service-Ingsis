package com.ingsis.snippetManager.snippetShare;

import com.ingsis.snippetManager.snippet.Snippet;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Entity
public class SnippetShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Snippet snippet;

    @NotBlank
    private String sharedWithUserId;

    @NotBlank
    private boolean canRead = true;

    public SnippetShare() {
    }

    public SnippetShare(Snippet snippet, String sharedWithUserId, boolean canRead) {
        this.snippet = snippet;
        this.sharedWithUserId = sharedWithUserId;
        this.canRead = canRead;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Snippet getSnippet() {
        return snippet;
    }

    public String getSharedWithUserId() {
        return sharedWithUserId;
    }

    public boolean isCanRead() {
        return canRead;
    }
}

package com.ingsis.snippetManager.snippetShare;

import com.ingsis.snippetManager.snippet.Snippet;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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

    public SnippetShare() {
    }

    public SnippetShare(Snippet snippet, String sharedWithUserId) {
        this.snippet = snippet;
        this.sharedWithUserId = sharedWithUserId;
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
}

package com.ingsis.snippetManager.intermediate.rules.model.formatted;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class SnippetFormatted {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID originalSnippetId;
    private UUID formattedSnippetId;

    public SnippetFormatted(UUID originalSnippetId, UUID formattedSnippetId) {
        this.originalSnippetId = originalSnippetId;
        this.formattedSnippetId = formattedSnippetId;
    }

    public SnippetFormatted() {

    }

    public UUID getOriginalSnippetId() {
        return originalSnippetId;
    }

    public UUID getFormattedSnippetId() {
        return formattedSnippetId;
    }
}

package com.ingsis.snippetManager.snippet.dto.format;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Entity
public class FormatRuleSet {

    @Id
    private UUID id;

    @NotBlank
    private String name;

    @Column(nullable = false)
    private String ownerId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rulesJson;

    public FormatRuleSet() {
    }

    public FormatRuleSet(UUID id, String name, String ownerId, String rulesJson) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.rulesJson = rulesJson;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getRulesJson() {
        return rulesJson;
    }
}

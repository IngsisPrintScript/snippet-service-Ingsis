package com.ingsis.snippetManager.intermediate.rules.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Rule {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String value;

    @Column(nullable = false)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleType type;

    public Rule() {
    }

    public Rule(String name, String value, String ownerId, RuleType type) {
        this.name = name;
        this.value = value;
        this.ownerId = ownerId;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getValue() {
        return value;
    }
    public String getOwnerId() {
        return ownerId;
    }
    public RuleType getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setType(RuleType type) {
        this.type = type;
    }
}

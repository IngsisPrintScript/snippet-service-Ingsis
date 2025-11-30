package com.ingsis.snippetManager.snippet;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class ValidationResult {

    private UUID snippetId;
    private boolean valid;
    private String message;
    private int line;
    private int column;

    public ValidationResult() {
    }

    public ValidationResult(UUID snippetId, boolean valid, String message, int line, int column) {
        this.valid = valid;
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public ValidationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public UUID getSnippetId() {
        return snippetId;
    }
}

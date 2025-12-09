package com.ingsis.snippetManager.snippet.dto;

import com.ingsis.snippetManager.intermediate.engine.supportedRules.FormatterSupportedRules;
import com.ingsis.snippetManager.intermediate.engine.supportedRules.LintSupportedRules;
import com.ingsis.snippetManager.intermediate.rules.model.Rule;
import com.ingsis.snippetManager.intermediate.rules.model.RuleType;
import com.ingsis.snippetManager.snippet.Snippet;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestFileDTO;
import com.ingsis.snippetManager.snippet.dto.snippetDTO.RequestSnippetDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class Converter {

    public Snippet convertToSnippet(RequestSnippetDTO snippetDTO) {
        return new Snippet(UUID.randomUUID(), snippetDTO.name(), snippetDTO.description(), snippetDTO.language(),
                snippetDTO.version());
    }

    public Snippet convertFileToSnippet(RequestFileDTO fileDTO) {
        return new Snippet(UUID.randomUUID(), fileDTO.name(), fileDTO.description(), fileDTO.language(),
                fileDTO.version());
    }
    public FormatterSupportedRules convertToFormatterRules(List<Rule> rules) {
        boolean hasPostAscriptionSpace = false;
        boolean hasPreAscriptionSpace = false;
        int indentationInsideConditionals = 0;
        boolean isAssignationSpaced = false;
        int printlnSeparationLines = 0;

        List<Rule> formatRules = rules.stream().filter(r -> r.getType() == RuleType.LINT).toList();

        for (Rule rule : formatRules) {
            switch (rule.getName()) {
                case "hasPostAscriptionSpace" -> hasPostAscriptionSpace = Boolean.parseBoolean(rule.getValue());
                case "hasPreAscriptionSpace" -> hasPreAscriptionSpace = Boolean.parseBoolean(rule.getValue());
                case "indentationInsideConditionals" ->
                    indentationInsideConditionals = Integer.parseInt(rule.getValue());
                case "isAssignationSpaced" -> isAssignationSpaced = Boolean.parseBoolean(rule.getValue());
                case "printlnSeparationLines" -> printlnSeparationLines = Integer.parseInt(rule.getValue());
            }
        }

        return new FormatterSupportedRules(hasPostAscriptionSpace, hasPreAscriptionSpace, indentationInsideConditionals,
                isAssignationSpaced, printlnSeparationLines);
    }
    public LintSupportedRules convertToLintRules(List<Rule> rules) {
        boolean mandatoryVariableOrLiteralInPrintln = false;
        boolean mandatoryVariableOrLiteralInReadInput = false;
        String identifierFormat = null;
        List<Rule> lintRules = rules.stream().filter(r -> r.getType() == RuleType.LINT).toList();
        for (Rule rule : lintRules) {
            switch (rule.getName()) {
                case "mandatoryVariableOrLiteralInPrintln" ->
                    mandatoryVariableOrLiteralInPrintln = Boolean.parseBoolean(rule.getValue());
                case "mandatoryVariableOrLiteralInReadInput" ->
                    mandatoryVariableOrLiteralInReadInput = Boolean.parseBoolean(rule.getValue());
                case "identifierFormat" -> identifierFormat = rule.getValue();
            }
        }

        return new LintSupportedRules(mandatoryVariableOrLiteralInPrintln, mandatoryVariableOrLiteralInReadInput,
                identifierFormat);
    }
}

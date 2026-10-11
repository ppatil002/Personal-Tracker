package com.example.pp.core.llm.prompts;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility engine for rendering base Jinja-style prompt templates.
 * Replaces placeholders like {{ variable }} or { variable } with values from a context map,
 * handling default fallback values for missing data and providing helper functions to combine prompts.
 */
public class PromptTemplateEngine {

    // Regex matching both Jinja style {{ var_name }} and standard { var_name }
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{?\\s*([a-zA-Z0-9_]+)\\s*\\}?\\}");
    private static final String DEFAULT_MISSING_VALUE = "[MISSING]";

    private PromptTemplateEngine() {
        // Utility class
    }

    /**
     * Renders a raw template string using values provided in the context map.
     *
     * @param template Base prompt template string containing placeholders.
     * @param context  Map containing variable names and their replacement values.
     * @return Rendered prompt text ready to send to an LLM.
     */
    public static String render(String template, Map<String, ?> context) {
        if (template == null) return "";
        if (context == null || context.isEmpty()) return template;

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);

            String valStr = (value != null) ? value.toString().trim() : "";
            String replacementStr = (!valStr.isEmpty()) ? valStr : DEFAULT_MISSING_VALUE;

            // Escape backslashes and dollar signs for safe Matcher replacement
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacementStr));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Combines system instructions and user context prompt into a single formatted prompt.
     *
     * @param systemInstruction High-level system prompt / persona definition.
     * @param userPrompt        Formatted prompt containing specific task inputs.
     * @return Unified prompt string.
     */
    public static String combinePrompts(String systemInstruction, String userPrompt) {
        if (systemInstruction == null || systemInstruction.trim().isEmpty()) {
            return userPrompt != null ? userPrompt : "";
        }
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return systemInstruction;
        }
        return systemInstruction.trim() + "\n\n" + userPrompt.trim();
    }

    /**
     * Appends a JSON output constraint instruction to any rendered prompt string.
     *
     * @param prompt     Rendered prompt.
     * @param jsonSchema Expected JSON format description or schema string.
     * @return Prompt with appended raw JSON instruction.
     */
    public static String appendJsonSchema(String prompt, String jsonSchema) {
        if (prompt == null) prompt = "";
        if (jsonSchema == null || jsonSchema.trim().isEmpty()) return prompt;

        return prompt.trim() + "\n\n" +
                "CRITICAL INSTRUCTION: Output ONLY a valid raw JSON object matching the schema below. " +
                "Do NOT include Markdown fences (no ```json), extra text, or explanations.\n\n" +
                "JSON SCHEMA:\n" + jsonSchema.trim();
    }
}

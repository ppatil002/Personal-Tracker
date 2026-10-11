package com.example.pp.core.llm.prompts;

/**
 * Pure Prompt Library for Vocabulary feature.
 * Contains raw base Jinja prompt templates used across vocabulary LLM operations.
 */
public class VocabularyPrompts {

    public static final String VOCABULARY_ENRICHMENT_PROMPT =
            "You are an expert lexicographer and English dictionary assistant.\n" +
            "Complete and enrich the linguistic information for the word provided below.\n\n" +
            "INPUT DETAILS:\n" +
            "- Word: {{ word }}\n" +
            "- Part of Speech (POS): {{ pos }}\n" +
            "- Meaning: {{ meaning }}\n" +
            "- Usage / Example Sentence: {{ usage }}\n\n" +
            "RULES:\n" +
            "1. If any input field is marked as [MISSING] or empty, fill it in accurately and concisely.\n" +
            "2. Keep valid provided information, but refine it if incomplete or grammatically incorrect.\n" +
            "3. 'partOfSpeech' must be capitalized (Noun, Verb, Adjective, Adverb, Phrase, Idiom, etc.).\n" +
            "4. 'meaning' must be a concise, clear definition.\n" +
            "5. 'example' (usage) must be a natural, complete sentence demonstrating correct word usage.\n" +
            "6. Output ONLY a raw JSON object with NO markdown, NO code fences, and NO extra text explanation.\n\n" +
            "EXPECTED JSON SCHEMA:\n" +
            "{\n" +
            "  \"word\": \"<word>\",\n" +
            "  \"partOfSpeech\": \"<partOfSpeech>\",\n" +
            "  \"meaning\": \"<meaning>\",\n" +
            "  \"example\": \"<example sentence>\"\n" +
            "}\n";

    public static final String VOCABULARY_LOOKUP_PROMPT =
            "You are an expert lexicographer.\n" +
            "Provide the part of speech, concise definition/meaning, and an example sentence for the word: \"{{ word }}\".\n\n" +
            "OUTPUT ONLY a raw JSON object in this format:\n" +
            "{\n" +
            "  \"word\": \"{{ word }}\",\n" +
            "  \"partOfSpeech\": \"<part of speech>\",\n" +
            "  \"meaning\": \"<meaning>\",\n" +
            "  \"example\": \"<example sentence>\"\n" +
            "}\n";

    private VocabularyPrompts() {
        // Pure prompt library
    }
}

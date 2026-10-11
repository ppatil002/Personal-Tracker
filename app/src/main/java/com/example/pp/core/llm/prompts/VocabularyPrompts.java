package com.example.pp.core.llm.prompts;

/**
 * Pure Prompt Library for Vocabulary feature.
 * Contains raw base Jinja prompt templates used across vocabulary LLM operations.
 */
public class VocabularyPrompts {

    public static final String VOCABULARY_ENRICHMENT_PROMPT =
            "You are an expert lexicographer and English dictionary assistant.\n" +
            "Complete and enrich the linguistic information for the word provided below based on the rules and example.\n\n" +

            "EXAMPLE:\n" +
            "Input:\n" +
            "- Word: serendipity\n" +
            "- Part of Speech (POS): [MISSING]\n" +
            "- Meaning: [MISSING]\n" +
            "- Usage / Example Sentence: [MISSING]\n\n" +
            "Output:\n" +
            "{\n" +
            "  \"word\": \"serendipity\",\n" +
            "  \"partOfSpeech\": \"Noun\",\n" +
            "  \"meaning\": \"The occurrence of events by chance in a happy or beneficial way.\",\n" +
            "  \"example\": \"Finding my old childhood photo album in the thrift store felt like pure serendipity.\"\n" +
            "}\n\n" +

            "ACTUAL TASK:\n" +
            "INPUT DETAILS:\n" +
            "- Word: {{ word }}\n" +
            "- Part of Speech (POS): {{ pos }}\n" +
            "- Meaning: {{ meaning }}\n" +
            "- Usage / Example Sentence: {{ usage }}\n\n" +
            "RULES:\n" +
            "1. If any input field is marked as [MISSING] or empty, fill it in accurately and concisely.\n" +
            "2. If any input field is already present please donot change that filed.\n" +
            "3. Keep valid provided information, but refine it if incomplete or grammatically incorrect.\n" +
            "4. 'partOfSpeech' must be capitalized and only of of these 4 available options (Noun, Verb, Adjective, Adverb).\n" +
            "5. 'meaning' must be a concise, clear definition. For a single word there could be multiple meaning so please make sure that you return best and easy beginner friendly meaning separated by semi colon as a delimiter for different meaning \n" +
            "6. 'example' must be a natural, complete sentence demonstrating correct word usage. Make sure to have the example related with the meaning.\n" +
            "7. Output ONLY a raw JSON object matching the schema below with NO markdown, NO code fences, and NO extra text.\n\n" +
            "EXPECTED JSON SCHEMA:\n" +
            "{\n" +
            "  \"word\": \"<word>\",\n" +
            "  \"partOfSpeech\": \"<partOfSpeech>\",\n" +
            "  \"meaning\": \"<meaning>\",\n" +
            "  \"example\": \"<example sentence>\"\n" +
            "}\n";
}

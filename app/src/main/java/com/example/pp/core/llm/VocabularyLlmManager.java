package com.example.pp.core.llm;

import static com.example.pp.core.constants.AppConstants.GEMINI_MODEL_VOCABULARY;

import com.example.pp.core.llm.prompts.PromptTemplateEngine;
import com.example.pp.core.llm.prompts.VocabularyPrompts;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.remote.gemini.GeminiApiClient;
import com.example.pp.data.remote.llm.LlmClient;
import com.example.pp.domain.model.VocabularyLlmResponse;
import com.example.pp.utils.LlmUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Feature Manager encapsulating all LLM business logic and data manipulation for Vocabulary.
 * Uses raw templates from VocabularyPrompts, renders them via PromptTemplateEngine, calls LlmClient,
 * and parses structured JSON using LlmUtils.
 */
public class VocabularyLlmManager {

    private static VocabularyLlmManager instance;
    private final LlmClient llmClient;
    private final Gson gson;

    public VocabularyLlmManager() {
        this(new GeminiApiClient());
    }

    public VocabularyLlmManager(LlmClient llmClient) {
        this.llmClient = llmClient != null ? llmClient : new GeminiApiClient();
        this.gson = new Gson();
    }

    public static synchronized VocabularyLlmManager getInstance() {
        if (instance == null) {
            instance = new VocabularyLlmManager();
        }
        return instance;
    }

    /**
     * Checks if a Vocabulary item has missing or empty core fields (POS, meaning, example).
     */
    public boolean hasMissingFields(Vocabulary vocabulary) {
        if (vocabulary == null) return false;
        boolean missingPos = vocabulary.partOfSpeech == null || vocabulary.partOfSpeech.trim().isEmpty();
        boolean missingMeaning = vocabulary.meaning == null || vocabulary.meaning.trim().isEmpty();
        boolean missingExample = vocabulary.example == null || vocabulary.example.trim().isEmpty();
        return missingPos || missingMeaning || missingExample;
    }

    /**
     * Processes word details and returns a parsed VocabularyLlmResponse domain.
     */
    public VocabularyLlmResponse processWordIngestion(String word, String pos, String meaning, String usage) throws IOException {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("word", word);
            context.put("pos", pos);
            context.put("meaning", meaning);
            context.put("usage", usage);

            String prompt = PromptTemplateEngine.render(VocabularyPrompts.VOCABULARY_ENRICHMENT_PROMPT, context);
            String rawResponse = llmClient.sendPromptSync(prompt, GEMINI_MODEL_VOCABULARY);
            return LlmUtils.parseStructuredResponse(rawResponse, VocabularyLlmResponse.class, gson);
        } catch (Exception e) {
            throw new IOException("Vocabulary LLM processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up full linguistic information for a given word string.
     */
    public VocabularyLlmResponse lookupWord(String word) throws IOException {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("word", word);

            String prompt = PromptTemplateEngine.render(VocabularyPrompts.VOCABULARY_LOOKUP_PROMPT, context);
            String rawResponse = llmClient.sendPromptSync(prompt, GEMINI_MODEL_VOCABULARY);
            return LlmUtils.parseStructuredResponse(rawResponse, VocabularyLlmResponse.class, gson);
        } catch (Exception e) {
            throw new IOException("Vocabulary LLM lookup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Enriches an existing Vocabulary entity in-place with missing linguistic information.
     */
    public Vocabulary autoCompleteVocabulary(Vocabulary vocabulary) throws IOException {
        if (vocabulary == null || vocabulary.word == null || vocabulary.word.trim().isEmpty()) {
            return vocabulary;
        }

        if (!hasMissingFields(vocabulary)) {
            return vocabulary;
        }

        VocabularyLlmResponse response = processWordIngestion(
                vocabulary.word,
                vocabulary.partOfSpeech,
                vocabulary.meaning,
                vocabulary.example
        );

        if (response != null) {
            if ((vocabulary.partOfSpeech == null || vocabulary.partOfSpeech.trim().isEmpty())
                    && response.getPartOfSpeech() != null && !response.getPartOfSpeech().trim().isEmpty()) {
                vocabulary.partOfSpeech = response.getPartOfSpeech().trim();
            }

            if ((vocabulary.meaning == null || vocabulary.meaning.trim().isEmpty())
                    && response.getMeaning() != null && !response.getMeaning().trim().isEmpty()) {
                vocabulary.meaning = response.getMeaning().trim();
            }

            if ((vocabulary.example == null || vocabulary.example.trim().isEmpty())
                    && response.getExample() != null && !response.getExample().trim().isEmpty()) {
                vocabulary.example = response.getExample().trim();
            }
        }

        return vocabulary;
    }

    public static String sanitizeJsonResponse(String rawResponse) {
        return LlmUtils.sanitizeJsonResponse(rawResponse);
    }
}

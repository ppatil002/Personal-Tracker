package com.example.pp.core.llm;

import static com.example.pp.core.constants.AppConstants.GEMINI_MODEL_VOCABULARY;

import android.util.Log;

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

    private static final String TAG = "VocabularyLLM";
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
     * Enriches an existing Vocabulary entity in-place with missing linguistic information.
     */
    public Vocabulary autoCompleteVocabulary(Vocabulary vocabulary) throws IOException {
        if (vocabulary == null || vocabulary.word == null || vocabulary.word.trim().isEmpty()) {
            return vocabulary;
        }

        if (!hasMissingFields(vocabulary)) {
            return vocabulary;
        }

        VocabularyLlmResponse response = enrichWordFromLLM(vocabulary);

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

    /**
     * Enriches word details using LLM and returns a parsed VocabularyLlmResponse domain.
     */
    public VocabularyLlmResponse enrichWordFromLLM(Vocabulary vocabulary) throws IOException {
        try {
            Map<String, Object> context = new HashMap<>();
            context.put("word", vocabulary.getWord());
            context.put("pos", vocabulary.getPartOfSpeech());
            context.put("meaning", vocabulary.getMeaning());
            context.put("usage", vocabulary.getExample());

            String prompt = PromptTemplateEngine.render(VocabularyPrompts.VOCABULARY_ENRICHMENT_PROMPT, context);
            String rawResponse = llmClient.sendPromptSync(prompt, GEMINI_MODEL_VOCABULARY);
            VocabularyLlmResponse response = LlmUtils.parseStructuredResponse(rawResponse, VocabularyLlmResponse.class, gson);
            Log.d(TAG, "Parsed response for word: '" + vocabulary.getWord() + "' - pos: '" + response.getPartOfSpeech() + "', meaning: '" + response.getMeaning() + "', example: '" + response.getExample() + "'");
            return response;
        } catch (Exception e) {
            Log.e(TAG, "enrichWordFromLLM FAILED for word: '" + vocabulary.getWord() + "'", e);
            throw new IOException("Vocabulary LLM processing failed: " + e.getMessage(), e);
        }
    }

}

package com.example.pp.data.remote.llm;

/**
 * Generic remote client interface for LLM calls (Gemini, OpenAI, Claude, etc.).
 * Allows plug-and-play switching of LLM providers.
 */
public interface LlmClient {

    /**
     * Synchronously generates response content for the given prompt and model.
     */
    String sendPromptSync(String prompt, String model) throws Exception;

    /**
     * Asynchronously generates response content for the given prompt and model.
     */
    void sendPromptAsync(String prompt, String model, LlmCallback callback);

    interface LlmCallback {
        void onSuccess(String rawJsonResponse);
        void onError(Exception e);
    }
}

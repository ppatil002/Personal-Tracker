package com.example.pp.data.remote.gemini;

import android.util.Log;

import com.example.pp.BuildConfig;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.remote.llm.LlmClient;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

import java.io.IOException;
import java.util.concurrent.Executor;

import lombok.Getter;

@Getter
public class GeminiApiClient implements LlmClient {

    private static final String TAG = "GeminiApiClient";
    private static volatile Client client;
    private final String apiKey;
    private final Executor networkExecutor;

    public GeminiApiClient() {
        this(resolveGeminiApiKey());
    }

    private static String resolveGeminiApiKey() {
        try {
            return (String) BuildConfig.class.getField("GEMINI_API_KEY").get(null);
        } catch (Exception e) {
            Log.e(TAG, "GeminiApiClient - Failed to resolve API key", e);
            return "";
        }
    }

    public GeminiApiClient(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
        this.networkExecutor = AppExecutors.getInstance().networkIO();
        initializeClient();
    }

    private synchronized void initializeClient() {
        if (client == null) {
            try {
                // Correct builder instantiation for the v0.3.0+ SDK
                client = Client.builder()
                        .apiKey(apiKey)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Gemini client", e);
            }
        }
    }

    @Override
    public String sendPromptSync(String prompt, String model) throws Exception {
        if (client == null) {
            throw new IOException("Gemini client not initialized. API key may be missing.");
        }

        try {
            // Correct configuration class name
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .build();

            Log.d(TAG, "GeminiApiClient - Initializing model: " + model);
            Log.d(TAG, "GeminiApiClient - Prompt To LLM ::   " + prompt);
            long startTime = System.currentTimeMillis();

            // client.models.generateContent is synchronous and returns response directly
            GenerateContentResponse response = client.models.generateContent(
                    model,
                    prompt,
                    config
            );
            
            long endTime = System.currentTimeMillis();
            Log.i(TAG, "GeminiApiClient - API call completed in " + (endTime - startTime) + "ms");

            if (response != null && response.text() != null && !response.text().isEmpty()) {
                return response.text();
            }
            throw new IOException("Gemini API returned empty response content.");
        } catch (Exception e) {
            Log.e(TAG, "GeminiApiClient - Unexpected exception during API call", e);
            throw new IOException("Gemini API Error: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPromptAsync(String prompt, String model, LlmCallback callback) {
        if (client == null) {
            if (callback != null) {
                callback.onError(new IOException("Gemini client not initialized. API key may be missing."));
            }
            return;
        }

        networkExecutor.execute(() -> {
            try {
                String result = sendPromptSync(prompt, model);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e instanceof IOException ? (IOException) e : new IOException(e));
                }
            }
        });
    }

    public static void shutdown() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore shutdown errors
            }
            client = null;
        }
    }
}

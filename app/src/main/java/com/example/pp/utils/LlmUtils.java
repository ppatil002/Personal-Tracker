package com.example.pp.utils;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;

/**
 * Common reusable utility methods for LLM operations across all features
 * (response cleaning, JSON parsing, prompt validation).
 */
public class LlmUtils {

    private static final String TAG = "LlmUtils";

    private LlmUtils() {
        // Utility class
    }

    /**
     * Sanitizes raw LLM output string by removing markdown code fences (e.g., ```json ... ``` or ``` ... ```).
     *
     * @param rawResponse Raw string returned from LLM execution.
     * @return Clean JSON string ready for parsing.
     */
    public static String sanitizeJsonResponse(String rawResponse) {
        if (rawResponse == null) return "";
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    /**
     * Sanitizes raw LLM string and deserializes it into the requested class using the provided Gson instance.
     *
     * @param rawResponse raw response text from LLM.
     * @param targetClass Target class to parse into.
     * @param gson        Gson serializer/deserializer instance.
     * @param <T>         Target type.
     * @return Parsed domain object instance.
     * @throws IOException If parsing fails or output is empty.
     */
    public static <T> T parseStructuredResponse(String rawResponse, Class<T> targetClass, Gson gson) throws IOException {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            throw new IOException("Empty or null raw response string received from LLM.");
        }

        String jsonText = sanitizeJsonResponse(rawResponse);
        try {
            return gson.fromJson(jsonText, targetClass);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse structured JSON response: " + rawResponse, e);
            throw new IOException("Failed to parse LLM response into " + targetClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}

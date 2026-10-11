package com.example.pp.data.repository.vocabulary.callback;

import java.util.List;

/**
 * Callback for batch word completion with real-time progress updates.
 * Used by manual UI operations (AI button) where user needs to see progress.
 * Reports progress after each word is processed (success or failure).
 */
public interface BatchWordCompletionProgressCallback {
    /**
     * Called after each word is processed.
     * @param current Current word index (1-based)
     * @param total Total words to process
     * @param wordName Name of the word just processed
     * @param successCount Number of successfully completed words
     * @param failureCount Number of failed words
     * @param failedWords List of words that failed to complete
     */
    void onProgress(int current, int total, String wordName,
                   int successCount, int failureCount, List<String> failedWords);

    /**
     * Called when all words have been processed.
     * @param successCount Total successful completions
     * @param failureCount Total failures
     * @param failedWords List of all failed words
     */
    void onComplete(int successCount, int failureCount, List<String> failedWords);

    /**
     * Called when there are no words to process.
     */
    void onEmpty();

    /**`
     * Called if a critical error occurs during batch processing.
     * @param throwable The error that occurred
     */
    void onError(Throwable throwable);
}

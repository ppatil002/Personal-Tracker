package com.example.pp.data.repository.vocabulary.callback;

/**
 * Callback for batch word completion without progress updates.
 * Used by periodic workers/schedulers that run in the background.
 * Reports final result only when all words are processed.
 */
public interface BatchWordCompletionCallback {
    /**
     * Called when batch completion is finished.
     * @param enrichedCount Number of words successfully completed
     */
    void onSuccess(int enrichedCount);

    /**
     * Called if an error occurs during batch processing.
     * @param throwable The error that occurred
     */
    void onError(Throwable throwable);
}

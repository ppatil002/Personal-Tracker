package com.example.pp.data.repository.vocabulary.callback;

/**
 * Callback for checking if a word already exists in the vocabulary.
 * Used before adding a new word to prevent duplicates.
 */
public interface WordExistenceCheckCallback {
    /**
     * Called with the result of the word existence check.
     * @param status Status code indicating the result (e.g., ADDED, ALREADY_EXISTS, etc.)
     * @param word The word that was checked
     */
    void onResult(int status, String word);
}

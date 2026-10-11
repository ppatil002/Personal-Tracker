package com.example.pp.domain.model;

/**
 * Represents the analytics state for vocabulary UI.
 * Contains statistics about loaded vs total vocabulary words.
 */
public class VocabularyAnalyticsState {
    public final int loadedWords;
    public final int totalWords;

    public VocabularyAnalyticsState(int loadedWords, int totalWords) {
        this.loadedWords = loadedWords;
        this.totalWords = totalWords;
    }
}

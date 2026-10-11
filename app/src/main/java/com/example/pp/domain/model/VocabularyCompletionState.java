package com.example.pp.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the completion state for filling missing vocabulary information.
 * Tracks progress of completing incomplete vocabulary entries.
 */
public class VocabularyCompletionState {
    public final boolean isCompleting;
    public final int currentWord;
    public final int totalWords;
    public final String currentWordName;
    public final int successCount;
    public final int failureCount;
    public final List<String> failedWords;

    public VocabularyCompletionState(boolean isCompleting, int currentWord, int totalWords,
                                    String currentWordName, int successCount, int failureCount,
                                    List<String> failedWords) {
        this.isCompleting = isCompleting;
        this.currentWord = currentWord;
        this.totalWords = totalWords;
        this.currentWordName = currentWordName;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.failedWords = failedWords != null ? failedWords : new ArrayList<>();
    }

    public static VocabularyCompletionState idle() {
        return new VocabularyCompletionState(false, 0, 0, null, 0, 0, new ArrayList<>());
    }

    public static VocabularyCompletionState started(int totalWords) {
        return new VocabularyCompletionState(true, 0, totalWords, "Starting...", 0, 0, new ArrayList<>());
    }

    public static VocabularyCompletionState processing(int currentWord, int totalWords, String wordName,
                                                     int successCount, int failureCount, List<String> failedWords) {
        return new VocabularyCompletionState(true, currentWord, totalWords, wordName, successCount, failureCount, failedWords);
    }

    public static VocabularyCompletionState completed(int successCount, int failureCount, List<String> failedWords) {
        return new VocabularyCompletionState(false, 0, 0, null, successCount, failureCount, failedWords);
    }
}

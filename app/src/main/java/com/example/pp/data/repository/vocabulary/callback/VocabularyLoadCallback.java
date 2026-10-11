package com.example.pp.data.repository.vocabulary.callback;

import com.example.pp.data.local.entity.Vocabulary;

import java.util.List;

/**
 * Callback for loading all vocabulary items.
 * Used when complete vocabulary list is needed.
 */
public interface VocabularyLoadCallback {
    /**
     * Called when vocabulary items are loaded.
     * @param vocabList List of vocabulary items
     */
    void onLoaded(List<Vocabulary> vocabList);
}

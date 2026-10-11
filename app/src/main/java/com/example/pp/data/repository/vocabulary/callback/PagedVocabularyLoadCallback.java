package com.example.pp.data.repository.vocabulary.callback;

import com.example.pp.data.local.entity.Vocabulary;

import java.util.List;

/**
 * Callback for loading paginated/chunked vocabulary items.
 * Used for pagination and scrolling where data is loaded in chunks.
 */
public interface PagedVocabularyLoadCallback {
    /**
     * Called when a chunk of vocabulary items is loaded.
     * @param vocabList List of vocabulary items in this chunk
     * @param totalWords Total number of words in the database
     */
    void onLoaded(List<Vocabulary> vocabList, int totalWords);
}

package com.example.pp.domain.model;

import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.local.entity.VocabularyRevisionLog;

import lombok.Getter;
import lombok.Setter;

/**
 * Container holding the result of a vocabulary review computation.
 */
@Getter
@Setter
public class VocabularyReviewResult {
    private final Vocabulary vocabulary;
    private final VocabularyRevisionLog revisionLog;

    public VocabularyReviewResult(Vocabulary vocabulary, VocabularyRevisionLog revisionLog) {
        this.vocabulary = vocabulary;
        this.revisionLog = revisionLog;
    }
}

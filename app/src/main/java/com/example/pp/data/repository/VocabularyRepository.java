package com.example.pp.data.repository;

import android.content.Context;

import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.ml.HalfLifeEngine;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.VocabularyDao;
import com.example.pp.data.local.entity.Vocabulary;

import java.util.List;

public class VocabularyRepository {
    private final Context context;
    private final VocabularyDao vocabularyDao;
    private final AppExecutors executors;

    public VocabularyRepository(Context context) {
        this.context = context.getApplicationContext();
        this.vocabularyDao = AppDatabase.getInstance(this.context).vocabularyDao();
        this.executors = AppExecutors.getInstance();
    }

    public void addVocabulary(String word, String meaning, String example, String partOfSpeech, long timestamp) {
        executors.diskIO().execute(() -> {
            Vocabulary vocab = new Vocabulary(word, meaning, example, partOfSpeech, timestamp);
            HalfLifeEngine.getInstance(context).processIngestion(vocab);
            vocabularyDao.insert(vocab);
        });
    }

    public void updateVocabulary(Vocabulary vocabulary) {
        executors.diskIO().execute(() -> {
            vocabularyDao.insert(vocabulary);
        });
    }

    public void deleteVocabulary(Vocabulary vocabulary) {
        executors.diskIO().execute(() -> {
            vocabularyDao.delete(vocabulary);
        });
    }

    public interface WordCheckCallback {
        void onResult(int status, String word);
    }

    public void addWordIfNotExist(String word, WordCheckCallback callback) {
        executors.diskIO().execute(() -> {
            Vocabulary existing = vocabularyDao.getVocabByWord(word);
            if (existing == null) {
                Vocabulary vocab = new Vocabulary(word, "", "", "", System.currentTimeMillis());
                HalfLifeEngine.getInstance(context).processIngestion(vocab);
                vocabularyDao.insert(vocab);
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_ADDED, word));
            } else {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_ALREADY_EXISTS, word));
            }
        });
    }

    public void addMeaningToLastWord(String rawMeaning, WordCheckCallback callback) {
        executors.diskIO().execute(() -> {
            Vocabulary last = vocabularyDao.getLastAddedVocab();
            if (last == null) {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_NO_LAST_WORD, ""));
                return;
            }

            if (last.meaning != null && !last.meaning.trim().isEmpty()) {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_MEANING_SKIPPED, last.word));
                return;
            }

            String meaning = rawMeaning.trim();
            // Remove ending full stop
            if (meaning.endsWith(".")) {
                meaning = meaning.substring(0, meaning.length() - 1).trim();
            }
            // Remove quotes
            meaning = meaning.replaceAll("^['\"]|['\"]$", "").trim();

            last.meaning = meaning;
            vocabularyDao.insert(last);
            executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_MEANING_ADDED, last.word));
        });
    }

    public void addPartOfSpeechToLastWord(String rawPos, WordCheckCallback callback) {
        executors.diskIO().execute(() -> {
            Vocabulary last = vocabularyDao.getLastAddedVocab();
            if (last == null) {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_NO_LAST_WORD, ""));
                return;
            }

            if (last.partOfSpeech != null && !last.partOfSpeech.trim().isEmpty()) {
                executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_POS_SKIPPED, last.word));
                return;
            }

            String pos = rawPos.trim().substring(0, 1).toUpperCase() + rawPos.trim().substring(1).toLowerCase();
            last.partOfSpeech = pos;
            vocabularyDao.insert(last);
            executors.mainThread().execute(() -> callback.onResult(AppConstants.VOCABULARY_STATUS_POS_ADDED, last.word));
        });
    }

    /**
     * Processes a user review for a word by updating its half-life and inserting back into the DB via VocabularyRepository.
     */
    public void processReview(long vocabularyId, int userRating, boolean hintUsed, long responseTimeMs) {
        executors.diskIO().execute(() -> {
            Vocabulary vocab = HalfLifeEngine.getInstance(context).processReview(vocabularyId, userRating, hintUsed, responseTimeMs);
            if (vocab != null) {
                vocabularyDao.insert(vocab);
            }
        });
    }

    /**
     * Rebuilds/recalculates half-life and due dates for all words in vocabulary and saves them to DB.
     */
    public void rebuildAllHalfLife(Runnable onComplete) {
        executors.diskIO().execute(() -> {
            List<Vocabulary> updatedList = HalfLifeEngine.getInstance(context).rebuildAllHalfLife();
            if (updatedList != null && !updatedList.isEmpty()) {
                vocabularyDao.insertBatch(updatedList);
            }
            if (onComplete != null) {
                executors.mainThread().execute(onComplete);
            }
        });
    }

    public interface LoadCallback {
        void onLoaded(List<Vocabulary> vocabList);
    }

    public interface ChunkLoadCallback {
        void onLoaded(List<Vocabulary> vocabList, int totalWords);
    }

    public void getVocabChunk(int limit, int offset, ChunkLoadCallback callback) {
        executors.diskIO().execute(() -> {
            List<Vocabulary> list = vocabularyDao.getLatestVocab(limit, offset);
            int totalWords = vocabularyDao.getCount();
            executors.mainThread().execute(() -> callback.onLoaded(list, totalWords));
        });
    }
}

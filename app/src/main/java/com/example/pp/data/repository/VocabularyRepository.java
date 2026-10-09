package com.example.pp.data.repository;

import android.content.Context;

import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.ml.HalfLifeEngine;
import com.example.pp.core.preferences.HalfLifeSharedPreference;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.VocabularyDao;
import com.example.pp.data.local.dao.VocabularyRevisionLogDao;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.local.entity.VocabularyRevisionLog;
import com.example.pp.domain.model.VocabularyReviewResult;

import java.util.List;

public class VocabularyRepository {
    private final Context context;
    private final VocabularyDao vocabularyDao;
    private final VocabularyRevisionLogDao vocabularyRevisionLogDao;
    private final HalfLifeSharedPreference halfLifePrefs;
    private final HalfLifeEngine halfLifeEngine;
    private final AppExecutors executors;

    public VocabularyRepository(Context context) {
        this.context = context.getApplicationContext();
        this.vocabularyDao = AppDatabase.getInstance(this.context).vocabularyDao();
        this.vocabularyRevisionLogDao = AppDatabase.getInstance(this.context).vocabularyRevisionLogDao();
        this.halfLifePrefs = new HalfLifeSharedPreference(this.context);
        this.halfLifeEngine = HalfLifeEngine.getInstance();
        this.executors = AppExecutors.getInstance();
    }

    public void addVocabulary(String word, String meaning, String example, String partOfSpeech, long timestamp) {
        executors.diskIO().execute(() -> {
            Vocabulary vocab = new Vocabulary(word, meaning, example, partOfSpeech, timestamp);
            double[] weights = halfLifePrefs.getWeights();
            halfLifeEngine.processIngestion(vocab, weights);
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
                double[] weights = halfLifePrefs.getWeights();
                halfLifeEngine.processIngestion(vocab, weights);
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
            Vocabulary vocab = vocabularyDao.getVocabById(vocabularyId);
            if (vocab == null) return;

            double[] weights = halfLifePrefs.getWeights();
            VocabularyReviewResult result = halfLifeEngine.processReview(vocab, userRating, hintUsed, weights);
            if (result != null) {
                vocabularyDao.insert(result.getVocabulary());
                if (result.getRevisionLog() != null) {
                    vocabularyRevisionLogDao.insert(result.getRevisionLog());
                }
            }
        });
    }

    /**
     * Rebuilds/recalculates half-life and due dates for all words in vocabulary and saves them to DB.
     */
    public void rebuildAllHalfLife(Runnable onComplete) {
        executors.diskIO().execute(() -> {
            List<Vocabulary> allVocab = vocabularyDao.getAllVocabList();
            if (allVocab != null && !allVocab.isEmpty()) {
                double[] weights = halfLifePrefs.getWeights();
                for (Vocabulary vocab : allVocab) {
                    List<VocabularyRevisionLog> logs = vocabularyRevisionLogDao.getLogsForVocabulary(vocab.id);
                    halfLifeEngine.recalculateWordHalfLife(vocab, weights, logs);
                }
                vocabularyDao.insertBatch(allVocab);
            }
            if (onComplete != null) {
                executors.mainThread().execute(onComplete);
            }
        });
    }

    /**
     * Fetches overdue vocabulary items for notification scheduling.
     * Falls back to least recently reviewed items if no items are currently overdue.
     */
    public Vocabulary getNextWordToRevise() {
        long now = System.currentTimeMillis();
        List<Vocabulary> dueWords = vocabularyDao.getOverdueVocabulary(now, 1);
        if (dueWords == null || dueWords.isEmpty()) {
            dueWords = vocabularyDao.getOldestReviewedVocabulary(1);
        }
        if (dueWords != null && !dueWords.isEmpty()) {
            return dueWords.get(0);
        }
        return null;
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

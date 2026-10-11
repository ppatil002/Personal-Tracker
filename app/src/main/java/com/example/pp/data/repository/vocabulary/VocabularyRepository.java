package com.example.pp.data.repository.vocabulary;

import android.content.Context;
import android.util.Log;

import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.llm.VocabularyLlmManager;
import com.example.pp.core.ml.HalfLifeEngine;
import com.example.pp.core.preferences.HalfLifeSharedPreference;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.VocabularyDao;
import com.example.pp.data.local.dao.VocabularyRevisionLogDao;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.local.entity.VocabularyRevisionLog;
import com.example.pp.data.repository.vocabulary.callback.BatchWordCompletionCallback;
import com.example.pp.data.repository.vocabulary.callback.BatchWordCompletionProgressCallback;
import com.example.pp.data.repository.vocabulary.callback.PagedVocabularyLoadCallback;
import com.example.pp.data.repository.vocabulary.callback.VocabularyLoadCallback;
import com.example.pp.data.repository.vocabulary.callback.WordExistenceCheckCallback;
import com.example.pp.domain.model.VocabularyReviewResult;

import java.util.ArrayList;
import java.util.List;

public class VocabularyRepository {
    private static final String TAG = "VocabularyRepository";
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

    public void addWordIfNotExist(String word, WordExistenceCheckCallback callback) {
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

    public void addMeaningToLastWord(String rawMeaning, WordExistenceCheckCallback callback) {
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

    public void addPartOfSpeechToLastWord(String rawPos, WordExistenceCheckCallback callback) {
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

    // ==================== Load Operations ====================

    public void loadAllVocabulary(VocabularyLoadCallback callback) {
        executors.diskIO().execute(() -> {
            List<Vocabulary> list = vocabularyDao.getAllVocabList();
            executors.mainThread().execute(() -> callback.onLoaded(list));
        });
    }

    public void loadVocabularyChunk(int limit, int offset, PagedVocabularyLoadCallback callback) {
        executors.diskIO().execute(() -> {
            List<Vocabulary> list = vocabularyDao.getLatestVocab(limit, offset);
            int totalWords = vocabularyDao.getCount();
            executors.mainThread().execute(() -> callback.onLoaded(list, totalWords));
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

    // ==================== Batch Completion Operations ====================

    /**
     * Completes incomplete vocabulary entries without progress updates.
     * Used by periodic workers/schedulers that run in the background.
     *
     * @param limit Maximum number of words to complete
     * @param callback Callback for final result only
     */
    public void completeIncompleteVocabulariesBatch(int limit, BatchWordCompletionCallback callback) {
        Log.i(TAG, "completeIncompleteVocabulariesBatch START - limit: " + limit);
        executors.diskIO().execute(() -> {
            try {
                Log.i(TAG, "Fetching incomplete vocabularies from database");
                List<Vocabulary> incompleteList = vocabularyDao.getIncompleteVocabularies(limit);
                Log.i(TAG, "Found " + (incompleteList != null ? incompleteList.size() : 0) + " incomplete vocabularies");

                if (incompleteList == null || incompleteList.isEmpty()) {
                    Log.i(TAG, "No incomplete vocabularies found, returning success with 0 completed");
                    if (callback != null) {
                        executors.mainThread().execute(() -> callback.onSuccess(0));
                    }
                    return;
                }

                VocabularyLlmManager llmManager = VocabularyLlmManager.getInstance();
                int completedCount = 0;
                int totalWords = incompleteList.size();

                Log.i(TAG, "Starting completion for " + totalWords + " words");
                for (int i = 0; i < incompleteList.size(); i++) {
                    Vocabulary vocab = incompleteList.get(i);
                    Log.i(TAG, "Processing word " + (i + 1) + "/" + totalWords + ": '" + vocab.word + "'");
                    try {
                        if (llmManager.hasMissingFields(vocab)) {
                            Log.i(TAG, "Word '" + vocab.word + "' has missing fields, calling LLM");
                            llmManager.autoCompleteVocabulary(vocab);
                            vocabularyDao.insert(vocab);
                            completedCount++;
                            Log.i(TAG, "Successfully completed word '" + vocab.word + "' (" + completedCount + "/" + totalWords + ")");
                        } else {
                            Log.i(TAG, "Word '" + vocab.word + "' has no missing fields, skipping");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error completing word '" + vocab.word + "' (" + (i + 1) + "/" + totalWords + ")", e);
                    }
                }

                Log.i(TAG, "Batch completion completed. Completed " + completedCount + "/" + totalWords + " words");
                final int finalCount = completedCount;
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onSuccess(finalCount));
                }
            } catch (Exception e) {
                Log.e(TAG, "Batch completion failed with exception", e);
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * Completes incomplete vocabulary entries with real-time progress updates.
     * Used by manual UI operations (AI button) where user needs to see progress.
     *
     * @param limit Maximum number of words to complete
     * @param callback Callback for progress updates and final result
     */
    public void completeIncompleteVocabulariesWithProgress(int limit, BatchWordCompletionProgressCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                List<Vocabulary> incompleteList = vocabularyDao.getIncompleteVocabularies(limit);

                if (incompleteList == null || incompleteList.isEmpty()) {
                    if (callback != null) {
                        executors.mainThread().execute(() -> callback.onEmpty());
                    }
                    return;
                }

                VocabularyLlmManager llmManager = VocabularyLlmManager.getInstance();
                int completedCount = 0;
                int failureCount = 0;
                List<String> failedWords = new ArrayList<>();
                int totalWords = incompleteList.size();
                Log.i(TAG, "Starting completion for " + totalWords + " words");
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onProgress(0, totalWords, "Starting...", 0, 0, new ArrayList<>()));
                }

                for (int i = 0; i < incompleteList.size(); i++) {
                    Vocabulary vocab = incompleteList.get(i);
                    try {
                        if (llmManager.hasMissingFields(vocab)) {
                            llmManager.autoCompleteVocabulary(vocab);
                            vocabularyDao.insert(vocab);
                            completedCount++;
                        }

                        // Report progress after each word
                        final int currentProgress = i + 1;
                        final int currentSuccess = completedCount;
                        final int currentFailure = failureCount;
                        final List<String> currentFailed = new ArrayList<>(failedWords);
                        if (callback != null) {
                            executors.mainThread().execute(() -> callback.onProgress(currentProgress, totalWords, vocab.word, currentSuccess, currentFailure, currentFailed));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error completing word '" + vocab.word + "' (" + (i + 1) + "/" + totalWords + ")", e);
                        failureCount++;
                        failedWords.add(vocab.word);

                        // Report progress even on failure
                        final int currentProgress = i + 1;
                        final int currentSuccess = completedCount;
                        final int currentFailure = failureCount;
                        final List<String> currentFailed = new ArrayList<>(failedWords);
                        if (callback != null) {
                            executors.mainThread().execute(() -> callback.onProgress(currentProgress, totalWords, vocab.word, currentSuccess, currentFailure, currentFailed));
                        }
                    }
                }

                Log.d(TAG, "Batch completion completed. Completed " + completedCount + "/" + totalWords + " words, Failed: " + failureCount);
                final int finalSuccessCount = completedCount;
                final int finalFailureCount = failureCount;
                final List<String> finalFailedWords = new ArrayList<>(failedWords);
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onComplete(finalSuccessCount, finalFailureCount, finalFailedWords));
                }
            } catch (Exception e) {
                Log.e(TAG, "Batch completion failed with exception", e);
                if (callback != null) {
                    executors.mainThread().execute(() -> callback.onError(e));
                }
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

    public void resetAndRebuildAllVocabularyStats() {
        executors.diskIO().execute(() -> {
            vocabularyDao.resetAllVocabularyStats();

            List<Vocabulary> allVocab = vocabularyDao.getAllVocabList();
            if (allVocab != null && !allVocab.isEmpty()) {
                double[] weights = halfLifePrefs.getWeights();
                for (Vocabulary vocab : allVocab) {
                    List<VocabularyRevisionLog> logs = vocabularyRevisionLogDao.getLogsForVocabulary(vocab.id);
                    halfLifeEngine.recalculateWordHalfLife(vocab, weights, logs);
                }
                vocabularyDao.insertBatch(allVocab);
            }
        });
    }
}

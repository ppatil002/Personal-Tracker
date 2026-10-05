package com.example.pp.core.ml;

import android.content.Context;

import com.example.pp.core.preferences.HalfLifeSharedPreference;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.local.entity.VocabularyRevisionLog;
import com.example.pp.utils.HalfLifeUtils;

import java.util.List;

public class HalfLifeEngine {

    private static final double LEARNING_RATE_ALPHA = 0.01;

    private static volatile HalfLifeEngine instance;
    private final Context context;
    private final AppDatabase db;
    private final HalfLifeSharedPreference halfLifePrefs;

    private HalfLifeEngine(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
        this.halfLifePrefs = new HalfLifeSharedPreference(this.context);
    }

    public static HalfLifeEngine getInstance(Context context) {
        if (instance == null) {
            synchronized (HalfLifeEngine.class) {
                if (instance == null) {
                    instance = new HalfLifeEngine(context);
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves current global weights [w0, w1, w2] from HalfLifeSharedPreference.
     */
    public double[] getWeights() {
        return halfLifePrefs.getWeights();
    }

    /**
     * Persists updated global weights [w0, w1, w2] via HalfLifeSharedPreference.
     */
    public void saveWeights(double w0, double w1, double w2) {
        halfLifePrefs.saveWeights(w0, w1, w2);
    }

    /**
     * Seeds initial weights in registry DB and SharedPreferences.
     */
    public HalfLifeSharedPreference.HalfLifeWeights seedWeights() {
        return halfLifePrefs.seedWeights();
    }

    /**
     * Phase 1: Ingestion Step (Birth of a Word)
     * Initializes baseline half-life and due timestamp for a vocabulary entity.
     * Sets nextRevisionTimestamp to 'now' so it immediately enters the top of the review queue.
     * Note: Purely calculates half-life fields without inserting into DB (insertion is handled by VocabularyRepository).
     */
    public Vocabulary processIngestion(Vocabulary vocab) {
        if (vocab == null) return null;

        long now = System.currentTimeMillis();
        if (vocab.createdTimestamp <= 0) {
            vocab.createdTimestamp = now;
        }

        vocab.lastReviewedTimestamp = now;
        vocab.correctAttempts = 0;
        vocab.incorrectAttempts = 0;
        vocab.hintsUsedCount = 0;

        double[] weights = getWeights();
        vocab.halfLife = HalfLifeUtils.computeHalfLife(0, 0, weights[0], weights[1], weights[2]);
        // Set due timestamp to 'now' so brand new words jump to the top of the review queue
        vocab.nextRevisionTimestamp = now;

        return vocab;
    }

    /**
     * Phase 2: Review Step (Revision of an Existing Word) given a Vocabulary entity.
     * Updates word attempt counts, timestamps, new half-life, and next due timestamp based on current model weights.
     * Appends an entry to vocabulary_revision_logs table.
     * Note: Purely calculates half-life fields without saving to DB (insertion is handled by VocabularyRepository).
     */
    public Vocabulary processReview(Vocabulary vocab, int userRating, boolean hintUsed, long responseTimeMs) {
        if (vocab == null) return null;

        long now = System.currentTimeMillis();
        double actualY = HalfLifeUtils.scaleRatingToBinary(userRating);

        // 1. Update word entity state
        double halfLifeBefore = vocab.halfLife;
        boolean remembered = (actualY == 1.0);

        if (remembered) {
            vocab.correctAttempts++;
        } else {
            vocab.incorrectAttempts++;
        }

        if (hintUsed) {
            vocab.hintsUsedCount++;
        }

        vocab.lastReviewedTimestamp = now;

        // 2. Compute new half-life & due timestamp using current weights
        double[] weights = getWeights();
        vocab.halfLife = HalfLifeUtils.computeHalfLife(vocab.correctAttempts, vocab.incorrectAttempts, weights[0], weights[1], weights[2]);
        vocab.nextRevisionTimestamp = HalfLifeUtils.computeDueTimestamp(now, vocab.halfLife);

        // 3. Insert historical revision log
        VocabularyRevisionLog log = new VocabularyRevisionLog(
                vocab.id,
                now,
                remembered ? "REMEMBER" : "FORGET",
                hintUsed,
                userRating,
                halfLifeBefore,
                vocab.halfLife
        );
        db.vocabularyRevisionLogDao().insert(log);

        return vocab;
    }

    /**
     * Phase 2 Overload: Review Step given a vocabularyId.
     */
    public Vocabulary processReview(long vocabularyId, int userRating, boolean hintUsed, long responseTimeMs) {
        Vocabulary vocab = db.vocabularyDao().getVocabById(vocabularyId);
        if (vocab == null) return null;

        return processReview(vocab, userRating, hintUsed, responseTimeMs);
    }

    /**
     * Separate Weight Update Function (For Periodic Worker)
     * Performs a single Stochastic Gradient Descent (SGD) step given a predicted recall probability P(t),
     * actual outcome y (1.0 or 0.0), and attempt features.
     */
    public double[] updateWeightsWithSGD(double predictedP, double actualY, int correctAttempts, int incorrectAttempts) {
        double[] weights = getWeights();
        double w0 = weights[0];
        double w1 = weights[1];
        double w2 = weights[2];

        double error = predictedP - actualY;
        w0 -= LEARNING_RATE_ALPHA * error * 1.0;
        w1 -= LEARNING_RATE_ALPHA * error * correctAttempts;
        w2 -= LEARNING_RATE_ALPHA * error * incorrectAttempts;

        saveWeights(w0, w1, w2);
        return new double[]{w0, w1, w2};
    }

    /**
     * Recalculates the half-life and due date for all words in vocabulary.
     * In case data gets corrupted, this rebuilds the entire half-life and due date again.
     * Note: Returns the updated list of Vocabulary entities. Insertion into DB is handled by VocabularyRepository.
     */
    public List<Vocabulary> rebuildAllHalfLife() {
        List<Vocabulary> allVocab = db.vocabularyDao().getAllVocabList();
        if (allVocab == null || allVocab.isEmpty()) {
            return allVocab;
        }

        return rebuildHalfLifeForList(allVocab);
    }

    /**
     * Recalculates half-life and due dates for a specific list of Vocabulary entities.
     */
    public List<Vocabulary> rebuildHalfLifeForList(List<Vocabulary> vocabList) {
        if (vocabList == null || vocabList.isEmpty()) {
            return vocabList;
        }

        double[] weights = getWeights();

        for (Vocabulary vocab : vocabList) {
            recalculateWordHalfLife(vocab, weights);
        }

        return vocabList;
    }

    /**
     * Recalculates half-life and next revision timestamp for a single Vocabulary entity.
     */
    public void recalculateWordHalfLife(Vocabulary vocab, double[] weights) {
        if (vocab == null) return;

        if (weights == null || weights.length < 3) {
            weights = getWeights();
        }

        // Check revision logs to recover attempt counts if available
        List<VocabularyRevisionLog> logs = db.vocabularyRevisionLogDao().getLogsForVocabulary(vocab.id);
        if (logs != null && !logs.isEmpty()) {
            int correct = 0;
            int incorrect = 0;
            int hints = 0;
            long maxLogTimestamp = 0;

            for (VocabularyRevisionLog log : logs) {
                if ("REMEMBER".equalsIgnoreCase(log.action) || log.rating >= 3) {
                    correct++;
                } else {
                    incorrect++;
                }
                if (log.hintUsed) {
                    hints++;
                }
                if (log.timestamp > maxLogTimestamp) {
                    maxLogTimestamp = log.timestamp;
                }
            }

            vocab.correctAttempts = correct;
            vocab.incorrectAttempts = incorrect;
            vocab.hintsUsedCount = hints;
            if (maxLogTimestamp > 0) {
                vocab.lastReviewedTimestamp = maxLogTimestamp;
            }
        }

        vocab.halfLife = HalfLifeUtils.computeHalfLife(
                vocab.correctAttempts,
                vocab.incorrectAttempts,
                weights[0], weights[1], weights[2]
        );

        long baseTime = vocab.lastReviewedTimestamp > 0 ? vocab.lastReviewedTimestamp : vocab.createdTimestamp;
        if (baseTime <= 0) {
            baseTime = System.currentTimeMillis();
        }

        if (vocab.correctAttempts == 0 && vocab.incorrectAttempts == 0) {
            vocab.nextRevisionTimestamp = baseTime;
        } else {
            vocab.nextRevisionTimestamp = HalfLifeUtils.computeDueTimestamp(baseTime, vocab.halfLife);
        }
    }

    /**
     * Fetches overdue vocabulary items for notification scheduling using Room DB index.
     * Falls back to least recently reviewed items if no items are currently overdue.
     */
    public List<Vocabulary> getDueWordsForNotification(int limit) {
        long now = System.currentTimeMillis();
        List<Vocabulary> dueWords = db.vocabularyDao().getOverdueVocabulary(now, limit);

        if (dueWords == null || dueWords.isEmpty()) {
            dueWords = db.vocabularyDao().getOldestReviewedVocabulary(limit);
        }

        return dueWords;
    }
}

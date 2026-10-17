package com.example.pp.core.ml;

import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.local.entity.VocabularyRevisionLog;
import com.example.pp.domain.model.VocabularyReviewResult;
import com.example.pp.utils.HalfLifeUtils;

import java.util.List;

public class HalfLifeEngine {

    private static final double LEARNING_RATE_ALPHA = 0.01;
    private static final double[] DEFAULT_WEIGHTS = new double[]{2.0, 1.0, -0.5};

    private static volatile HalfLifeEngine instance;

    public HalfLifeEngine() {
    }

    public static HalfLifeEngine getInstance() {
        if (instance == null) {
            synchronized (HalfLifeEngine.class) {
                if (instance == null) {
                    instance = new HalfLifeEngine();
                }
            }
        }
        return instance;
    }


    /**
     * Phase 1: Ingestion Step (Birth of a Word)
     * Initializes baseline half-life and due timestamp for a vocabulary entity.
     * Sets nextRevisionTimestamp to 'now' so it immediately enters the top of the review queue.
     */
    public Vocabulary processIngestion(Vocabulary vocab, double[] weights) {
        if (vocab == null) return null;

        long now = System.currentTimeMillis();
        if (vocab.createdTimestamp <= 0) {
            vocab.createdTimestamp = now;
        }

        vocab.lastReviewedTimestamp = now;
        vocab.correctAttempts = 0.0;
        vocab.incorrectAttempts = 0.0;
        vocab.hintsUsedCount = 0;

        if (weights == null || weights.length < 3) {
            weights = DEFAULT_WEIGHTS;
        }

        vocab.halfLife = HalfLifeUtils.computeHalfLife(0, 0, weights[0], weights[1], weights[2]);
        // Set due timestamp to 'now' so brand-new words jump to the top of the review queue
        vocab.nextRevisionTimestamp = now;

        return vocab;
    }

    /**
     * Phase 2: Review Step (Revision of an Existing Word).
     * Updates word attempt counts, timestamps, new half-life, and next due timestamp based on model weights.
     * Returns a VocabularyReviewResult containing the updated Vocabulary entity and created VocabularyRevisionLog.
     */
    public VocabularyReviewResult processReview(Vocabulary vocab, int userRating, boolean hintUsed, double[] weights) {
        if (vocab == null) return null;

        long now = System.currentTimeMillis();
        HalfLifeUtils.RatingWeight ratingWeight = HalfLifeUtils.mapRatingToWeights(userRating);

        double halfLifeBefore = vocab.halfLife;
        boolean remembered = (userRating >= 3);

        vocab.correctAttempts += ratingWeight.correctDelta;
        vocab.incorrectAttempts += ratingWeight.incorrectDelta;

        if (hintUsed) {
            vocab.hintsUsedCount++;
        }

        vocab.lastReviewedTimestamp = now;

        if (weights == null || weights.length < 3) {
            weights = DEFAULT_WEIGHTS;
        }

        vocab.halfLife = HalfLifeUtils.computeHalfLife(vocab.correctAttempts, vocab.incorrectAttempts, weights[0], weights[1], weights[2]);
        vocab.nextRevisionTimestamp = HalfLifeUtils.computeDueTimestamp(now, vocab.halfLife);

        VocabularyRevisionLog log = new VocabularyRevisionLog(
                vocab.id,
                now,
                remembered ? "REMEMBER" : "FORGET",
                hintUsed,
                userRating,
                halfLifeBefore,
                vocab.halfLife
        );

        return new VocabularyReviewResult(vocab, log);
    }

    /**
     * Separate Weight Update Function (For Periodic Worker)
     * Performs a single Stochastic Gradient Descent (SGD) step given current weights,
     * predicted recall probability P(t), actual outcome y (1.0 or 0.0), and attempt features.
     */
    public double[] updateWeightsWithSGD(double[] currentWeights, double predictedP, double actualY, double correctAttempts, double incorrectAttempts) {
        if (currentWeights == null || currentWeights.length < 3) {
            currentWeights = DEFAULT_WEIGHTS;
        }

        double w0 = currentWeights[0];
        double w1 = currentWeights[1];
        double w2 = currentWeights[2];

        double error = predictedP - actualY;
        w0 -= LEARNING_RATE_ALPHA * error * 1.0;
        w1 -= LEARNING_RATE_ALPHA * error * correctAttempts;
        w2 -= LEARNING_RATE_ALPHA * error * incorrectAttempts;

        return new double[]{w0, w1, w2};
    }

    /**
     * Recalculates half-life and due dates for a list of Vocabulary entities using model weights.
     */
    public List<Vocabulary> rebuildHalfLifeForList(List<Vocabulary> vocabList, double[] weights) {
        if (vocabList == null || vocabList.isEmpty()) {
            return vocabList;
        }

        for (Vocabulary vocab : vocabList) {
            recalculateWordHalfLife(vocab, weights, null);
        }

        return vocabList;
    }

    /**
     * Recalculates half-life and next revision timestamp for a single Vocabulary entity,
     * optionally taking past revision logs to rebuild attempt counts.
     */
    public void recalculateWordHalfLife(Vocabulary vocab, double[] weights, List<VocabularyRevisionLog> logs) {
        if (vocab == null) return;

        if (weights == null || weights.length < 3) {
            weights = DEFAULT_WEIGHTS;
        }

        if (logs != null && !logs.isEmpty()) {
            double correct = 0.0;
            double incorrect = 0.0;
            int hints = 0;
            long maxLogTimestamp = 0;

            for (VocabularyRevisionLog log : logs) {
                if (log.rating >= 1 && log.rating <= 5) {
                    HalfLifeUtils.RatingWeight rw = HalfLifeUtils.mapRatingToWeights(log.rating);
                    correct += rw.correctDelta;
                    incorrect += rw.incorrectDelta;
                } else if ("REMEMBER".equalsIgnoreCase(log.action) || log.rating >= 3) {
                    correct += 1.0;
                } else {
                    incorrect += 1.0;
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
}

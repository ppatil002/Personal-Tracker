package com.example.pp.utils;

public class HalfLifeUtils {

    public static final double TARGET_RETENTION = 0.85; // 85% recall probability target
    public static final double MS_PER_DAY = 86400000.0;

    /**
     * Computes the half-life 'h' in days using current weights and attempts:
     * h = 2^(w0 + (w1 * correct) + (w2 * incorrect))
     */
    public static double computeHalfLife(int correctAttempts, int incorrectAttempts, double w0, double w1, double w2) {
        double exponent = w0 + (w1 * correctAttempts) + (w2 * incorrectAttempts);
        double halfLife = Math.pow(2.0, exponent);
        return halfLife <= 0 ? 0.01 : halfLife;
    }

    /**
     * Computes predicted recall probability P(t) based on elapsed time and half-life:
     * P(t) = 2^(-deltaDays / halfLife)
     */
    public static double computeRecallProbability(long lastReviewedMs, long currentMs, double halfLifeDays) {
        if (lastReviewedMs <= 0) return 0.0;
        double deltaDays = (currentMs - lastReviewedMs) / MS_PER_DAY;
        if (deltaDays < 0) deltaDays = 0;
        if (halfLifeDays <= 0) halfLifeDays = 0.01;
        double p = Math.pow(2.0, -deltaDays / halfLifeDays);
        if (p < 0.0) return 0.0;
        if (p > 1.0) return 1.0;
        return p;
    }

    /**
     * Computes the target due timestamp (Epoch ms) when recall probability drops to TARGET_RETENTION:
     * deltaDaysDue = -h * log2(TARGET_RETENTION)
     * T_due = lastReviewedMs + (deltaDaysDue * MS_PER_DAY)
     */
    public static long computeDueTimestamp(long lastReviewedMs, double halfLifeDays) {
        if (halfLifeDays <= 0) halfLifeDays = 0.01;
        double log2Target = Math.log(TARGET_RETENTION) / Math.log(2.0);
        double deltaDaysDue = -halfLifeDays * log2Target;
        long deltaMsDue = (long) (deltaDaysDue * MS_PER_DAY);
        return lastReviewedMs + deltaMsDue;
    }

    /**
     * Maps a 1-5 user recall rating scale to binary outcome y (0.0 or 1.0):
     * Ratings 1-2 (Forgot / Hard) -> 0.0
     * Ratings 3-5 (Good / Great / Confident) -> 1.0
     */
    public static double scaleRatingToBinary(int userRating) {
        return userRating >= 3 ? 1.0 : 0.0;
    }
}

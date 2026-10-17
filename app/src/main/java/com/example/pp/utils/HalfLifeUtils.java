package com.example.pp.utils;

public class HalfLifeUtils {

    public static final double TARGET_RETENTION = 0.85; // 85% recall probability target
    public static final double MS_PER_DAY = 86400000.0;

    public static class RatingWeight {
        public final double correctDelta;
        public final double incorrectDelta;

        public RatingWeight(double correctDelta, double incorrectDelta) {
            this.correctDelta = correctDelta;
            this.incorrectDelta = incorrectDelta;
        }
    }

    /**
     * Maps a 1-5 user recall rating to fractional performance weights (deltas):
     * 1 Star (Complete failure): correct = 0.0, incorrect = 1.0 (Full failure weight)
     * 2 Stars (Forgot/Hard): correct = 0.0, incorrect = 0.5 (Half failure weight)
     * 3 Stars (Pass/Good): correct = 0.3, incorrect = 0.0 (Small credit)
     * 4 Stars (Great): correct = 0.7, incorrect = 0.0 (Strong credit)
     * 5 Stars (Mastered): correct = 1.0, incorrect = 0.0 (Full standard credit)
     */
    public static RatingWeight mapRatingToWeights(int userRating) {
        switch (userRating) {
            case 1:
                return new RatingWeight(0.0, 1.0);
            case 2:
                return new RatingWeight(0.0, 0.5);
            case 3:
                return new RatingWeight(0.3, 0.0);
            case 4:
                return new RatingWeight(0.7, 0.0);
            case 5:
                return new RatingWeight(1.0, 0.0);
            default:
                if (userRating < 1) {
                    return new RatingWeight(0.0, 1.0);
                } else {
                    return new RatingWeight(1.0, 0.0);
                }
        }
    }

    /**
     * Computes the half-life 'h' in days using current weights and attempts:
     * h = 2^(w0 + (w1 * correct) + (w2 * incorrect))
     */
    public static double computeHalfLife(double correctAttempts, double incorrectAttempts, double w0, double w1, double w2) {
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
     * Maps a 1-5 user recall rating scale to a normalized [0.0 to 1.0] continuous value:
     * 1 Star -> 0.0
     * 2 Stars -> 0.25
     * 3 Stars -> 0.5
     * 4 Stars -> 0.75
     * 5 Stars -> 1.0
     */
    public static double scaleRatingToBinary(int userRating) {
        return (userRating - 1.0) / 4.0;
    }
}

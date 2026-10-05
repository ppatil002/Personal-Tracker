package com.example.pp.core.preferences;

import android.content.Context;

import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.RegistryDao;
import com.example.pp.data.local.entity.RegistryEntity;

public class HalfLifeSharedPreference extends BasePreferenceHelper {

    public static final String KEY_HLF_WEIGHTS = "hlf_weights";
    public static final String KEY_W0 = "hlf_w0";
    public static final String KEY_W1 = "hlf_w1";
    public static final String KEY_W2 = "hlf_w2";

    public static final double DEFAULT_W0 = 2.0;  // Base memory factor
    public static final double DEFAULT_W1 = 1.0;  // Correct attempts weight
    public static final double DEFAULT_W2 = -0.5; // Incorrect attempts weight
    public static final float DEFAULT_UNSET_VALUE = -1.0f;

    private final RegistryDao registryDao;

    public HalfLifeSharedPreference(Context context) {
        super(context);
        this.registryDao = AppDatabase.getInstance(context).registryDao();
    }

    public static class HalfLifeWeights {
        public double w0;
        public double w1;
        public double w2;

        public HalfLifeWeights() {
            this.w0 = DEFAULT_W0;
            this.w1 = DEFAULT_W1;
            this.w2 = DEFAULT_W2;
        }

        public HalfLifeWeights(double w0, double w1, double w2) {
            this.w0 = w0;
            this.w1 = w1;
            this.w2 = w2;
        }
    }

    /**
     * Retrieves current global weights [w0, w1, w2].
     * If SharedPreferences are cleared (value equals -1), it fetches the latest value from DB
     * and stores it in SharedPreferences. If key is absent in DB, it seeds default weights.
     */
    public double[] getWeights() {
        float w0 = getFloat(KEY_W0, DEFAULT_UNSET_VALUE);
        float w1 = getFloat(KEY_W1, DEFAULT_UNSET_VALUE);
        float w2 = getFloat(KEY_W2, DEFAULT_UNSET_VALUE);

        if (w0 == DEFAULT_UNSET_VALUE || w1 == DEFAULT_UNSET_VALUE || w2 == DEFAULT_UNSET_VALUE) {
            HalfLifeWeights dbWeights = loadWeightsFromDb();
            if (dbWeights == null) {
                dbWeights = seedWeights();
            } else {
                saveToSharedPreferences(dbWeights.w0, dbWeights.w1, dbWeights.w2);
            }
            return new double[]{dbWeights.w0, dbWeights.w1, dbWeights.w2};
        }

        return new double[]{w0, w1, w2};
    }

    /**
     * Retrieves current weights as a HalfLifeWeights object.
     */
    public HalfLifeWeights getWeightsObject() {
        double[] weights = getWeights();
        return new HalfLifeWeights(weights[0], weights[1], weights[2]);
    }

    /**
     * Saves updated weights to SharedPreferences and writes them back to the DB registry table in nested JSON.
     */
    public void saveWeights(double w0, double w1, double w2) {
        saveToSharedPreferences(w0, w1, w2);
        saveToDb(w0, w1, w2);
    }

    /**
     * Seed weight method: Writes default seed weights into the registry table under key "hlf_weights"
     * as nested JSON and updates SharedPreferences.
     */
    public HalfLifeWeights seedWeights() {
        HalfLifeWeights seed = new HalfLifeWeights(DEFAULT_W0, DEFAULT_W1, DEFAULT_W2);
        saveToDb(seed.w0, seed.w1, seed.w2);
        saveToSharedPreferences(seed.w0, seed.w1, seed.w2);
        return seed;
    }

    private HalfLifeWeights loadWeightsFromDb() {
        try {
            RegistryEntity entity = registryDao.getRegistryItem(KEY_HLF_WEIGHTS);
            if (entity != null && entity.value != null && !entity.value.trim().isEmpty()) {
                HalfLifeWeights weights = gson.fromJson(entity.value, HalfLifeWeights.class);
                if (weights != null) {
                    return weights;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void saveToSharedPreferences(double w0, double w1, double w2) {
        putFloat(KEY_W0, (float) w0);
        putFloat(KEY_W1, (float) w1);
        putFloat(KEY_W2, (float) w2);
    }

    private void saveToDb(double w0, double w1, double w2) {
        HalfLifeWeights weights = new HalfLifeWeights(w0, w1, w2);
        String json = gson.toJson(weights);
        RegistryEntity entity = new RegistryEntity(KEY_HLF_WEIGHTS, json, System.currentTimeMillis());
        registryDao.insertOrUpdate(entity);
    }
}
